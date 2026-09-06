/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l3;

import li.cil.oc2.common.inet.InetUtils;
import li.cil.oc2.common.inet.l4.TransportLayer;
import li.cil.oc2.common.inet.l4.TransportMessage;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

public final class NetworkLayer {
    public static final short PROTOCOL_NONE = 0;
    public static final short PROTOCOL_IPv4 = 0x0800;

    public static final int IPv4_HEADER_SIZE = 20;
    public static final int IPv4_SOURCE_OFFSET = 12;
    public static final int IPv4_DESTINATION_OFFSET = 16;
    private static final int IPv4_VERSION = 4;
    private static final byte PROTOCOL_ICMP = 1;
    private static final byte ICMP_TYPE_DESTINATION_UNREACHABLE = 3;
    private static final byte ICMP_CODE_ADMINISTRATIVELY_PROHIBITED = 13;
    private static final byte ICMP_TYPE_TIME_EXCEEDED = 11;
    private static final byte ICMP_CODE_TTL_EXCEEDED = 0;
    private static final int ICMP_QUOTED_PAYLOAD_SIZE = 8;
    private static final int ICMP_ERROR_HEADER_SIZE = 8;
    private static final int MAX_PENDING_ERRORS = 8;

    // --------------------------------------------------------------------- //

    private final TransportLayer transportLayer;
    private final AddressFilter addressFilter;
    private final TransportMessage inMessage = new TransportMessage();
    private final TransportMessage outMessage = new TransportMessage();
    private final Deque<IcmpError> pendingErrors = new ArrayDeque<>();

    // --------------------------------------------------------------------- //

    public NetworkLayer(final TransportLayer transportLayer, final AddressFilter addressFilter) {
        this.transportLayer = transportLayer;
        this.addressFilter = addressFilter;
    }

    // --------------------------------------------------------------------- //

    public void onTick() {
        transportLayer.onTick();
    }

    public void onStop() {
        pendingErrors.clear();
        transportLayer.onStop();
    }

    public short receivePacket(final ByteBuffer packet) {
        final int start = packet.position();

        if (!pendingErrors.isEmpty()) {
            return writeIcmpError(packet, pendingErrors.poll());
        }

        packet.position(start + IPv4_HEADER_SIZE);
        inMessage.initializeBuffer(packet);
        final byte protocol = transportLayer.receiveTransportMessage(inMessage);
        if (protocol == TransportLayer.PROTOCOL_NONE) {
            packet.position(start);
            return PROTOCOL_NONE;
        }

        writeIpv4Header(packet, start, protocol,
            inMessage.getSourceIpv4Address(), inMessage.getDestinationIpv4Address(), inMessage.getTimeToLive());
        return PROTOCOL_IPv4;
    }

    public void sendPacket(final short protocol, final ByteBuffer packet) {
        if (protocol != PROTOCOL_IPv4) {
            return;
        }
        if (packet.remaining() < IPv4_HEADER_SIZE) {
            return;
        }

        final int start = packet.position();
        final int available = packet.remaining();

        final int versionAndHeaderLength = Byte.toUnsignedInt(packet.get());
        if ((versionAndHeaderLength >>> 4) != IPv4_VERSION) {
            return;
        }
        final int headerSize = (versionAndHeaderLength & 0xF) * 4;
        if (headerSize < IPv4_HEADER_SIZE || headerSize > available) {
            return;
        }

        packet.get(); // Differentiated services.

        final int totalLength = Short.toUnsignedInt(packet.getShort());
        // The length field covers the header, so anything shorter than the header is malformed, and
        // anything longer than what arrived would let a crafted header read past the packet.
        if (totalLength < headerSize || totalLength > available) {
            return;
        }

        packet.getShort(); // Identification.

        final int flagsAndFragmentOffset = Short.toUnsignedInt(packet.getShort());
        final boolean moreFragments = (flagsAndFragmentOffset & 0x2000) != 0;
        final int fragmentOffset = flagsAndFragmentOffset & 0x1FFF;
        if (moreFragments || fragmentOffset != 0) {
            // Reassembly is a well-known source of parser bugs and buys nothing here, since we
            // advertise an MTU the guest can always fit a datagram into.
            return;
        }

        final int timeToLive = Byte.toUnsignedInt(packet.get());
        final byte transportProtocol = packet.get();
        packet.getShort(); // Header checksum; the link to the guest cannot corrupt anything.
        final int sourceIpAddress = packet.getInt();
        final int destinationIpAddress = packet.getInt();

        if (timeToLive <= 1) {
            queueIcmpError(packet, start, headerSize, totalLength,
                ICMP_TYPE_TIME_EXCEEDED, ICMP_CODE_TTL_EXCEEDED, sourceIpAddress, destinationIpAddress);
            return;
        }

        if (!addressFilter.isAllowed(destinationIpAddress)) {
            queueIcmpError(packet, start, headerSize, totalLength,
                ICMP_TYPE_DESTINATION_UNREACHABLE, ICMP_CODE_ADMINISTRATIVELY_PROHIBITED,
                sourceIpAddress, destinationIpAddress);
            return;
        }

        packet.position(start + headerSize);
        packet.limit(start + totalLength);

        outMessage.initializeBuffer(packet);
        outMessage.updateIpv4(sourceIpAddress, destinationIpAddress, (byte) (timeToLive - 1));
        transportLayer.sendTransportMessage(transportProtocol, outMessage);
    }

    // --------------------------------------------------------------------- //

    private static void writeIpv4Header(
        final ByteBuffer packet,
        final int start,
        final byte protocol,
        final int sourceIpAddress,
        final int destinationIpAddress,
        final byte timeToLive
    ) {
        final int totalLength = packet.limit() - start;

        packet.position(start);
        packet.put((byte) ((IPv4_VERSION << 4) | (IPv4_HEADER_SIZE / 4)));
        packet.put((byte) 0); // Differentiated services.
        packet.putShort((short) totalLength);
        packet.putShort((short) ThreadLocalRandom.current().nextInt());
        packet.putShort((short) 0x4000); // Don't fragment; we never emit fragments.
        packet.put(timeToLive);
        packet.put(protocol);
        packet.putShort((short) 0); // Checksum, computed below.
        packet.putInt(sourceIpAddress);
        packet.putInt(destinationIpAddress);

        packet.position(start);
        final short checksum = InetUtils.rfc1071Checksum(packet, IPv4_HEADER_SIZE);
        packet.putShort(start + 10, checksum);
        packet.position(start);
    }

    private void queueIcmpError(
        final ByteBuffer packet,
        final int start,
        final int headerSize,
        final int totalLength,
        final byte type,
        final byte code,
        final int sourceIpAddress,
        final int destinationIpAddress
    ) {
        if (pendingErrors.size() >= MAX_PENDING_ERRORS) {
            return;
        }

        final int quoted = Math.min(headerSize + ICMP_QUOTED_PAYLOAD_SIZE, totalLength);
        final byte[] payload = new byte[quoted];
        final int oldPosition = packet.position();
        final int oldLimit = packet.limit();
        packet.limit(start + quoted);
        packet.position(start);
        packet.get(payload);
        packet.limit(oldLimit);
        packet.position(oldPosition);

        // Report the error as coming from the address the guest was trying to reach; we have no
        // address of our own at this layer, and this is what the guest's tooling expects to see.
        pendingErrors.add(new IcmpError(type, code, destinationIpAddress, sourceIpAddress, payload));
    }

    private short writeIcmpError(final ByteBuffer packet, final IcmpError error) {
        final int start = packet.position();
        final int bodyStart = start + IPv4_HEADER_SIZE;
        final int bodyLength = ICMP_ERROR_HEADER_SIZE + error.payload().length;

        if (packet.capacity() - bodyStart < bodyLength) {
            return PROTOCOL_NONE;
        }

        packet.position(bodyStart);
        packet.put(error.type());
        packet.put(error.code());
        packet.putShort((short) 0); // Checksum, computed below.
        packet.putInt(0); // Unused for both errors we emit.
        packet.put(error.payload());
        packet.limit(packet.position());

        packet.position(bodyStart);
        final short checksum = InetUtils.rfc1071Checksum(packet);
        packet.putShort(bodyStart + 2, checksum);

        writeIpv4Header(packet, start, PROTOCOL_ICMP,
            error.sourceIpAddress(), error.destinationIpAddress(), (byte) 64);
        return PROTOCOL_IPv4;
    }

    // --------------------------------------------------------------------- //

    private record IcmpError(byte type, byte code, int sourceIpAddress, int destinationIpAddress, byte[] payload) {
    }
}
