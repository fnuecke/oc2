/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l2;

import li.cil.oc2.common.inet.l3.NetworkLayer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class LinkLocalLayer {
    public static final int FRAME_HEADER_SIZE = 14;
    public static final int DEFAULT_MTU = 1500;
    public static final int FRAME_SIZE = FRAME_HEADER_SIZE + DEFAULT_MTU;

    private static final short MAC_PREFIX = 0x0242;
    private static final short BROADCAST_MAC_PREFIX = (short) 0xFFFF;
    private static final int BROADCAST_MAC_ADDRESS = 0xFFFFFFFF;
    private static final short PROTOCOL_ARP = 0x0806;
    private static final short HW_TYPE_ETHERNET = 0x0001;
    private static final int ARP_MESSAGE_SIZE = 28;
    private static final int ARP_ADDRESS_TYPE = (HW_TYPE_ETHERNET << 16) | (NetworkLayer.PROTOCOL_IPv4 & 0xFFFF);
    private static final short ARP_ADDRESS_SIZES = (6 << 8) | 4;
    private static final short ARP_REQUEST = 0x0001;
    private static final short ARP_REPLY = 0x0002;
    private static final int MAX_GUESTS = 32;
    private static final int MAX_PENDING_ARP_REPLIES = 8;

    // --------------------------------------------------------------------- //

    private final NetworkLayer networkLayer;
    private final Map<Integer, MacAddress> guestMacAddresses = new LinkedHashMap<>();
    private final Deque<PendingArpReply> pendingArpReplies = new ArrayDeque<>();
    private final MacAddress gatewayMacAddress = randomMacAddress();
    private int gatewayIpAddress;
    private boolean hasGatewayIpAddress;

    // --------------------------------------------------------------------- //

    public LinkLocalLayer(final NetworkLayer networkLayer) {
        this.networkLayer = networkLayer;
    }

    // --------------------------------------------------------------------- //

    public void onTick() {
        networkLayer.onTick();
    }

    public void onStop() {
        networkLayer.onStop();
    }

    public boolean receiveEthernetFrame(final ByteBuffer frame) {
        final int start = frame.position();

        final PendingArpReply reply = pendingArpReplies.pollFirst();
        if (reply != null) {
            writeArpReply(frame, start, reply);
            return true;
        }

        if (guestMacAddresses.isEmpty()) {
            return false;
        }

        frame.position(start + FRAME_HEADER_SIZE);
        final short protocol = networkLayer.receivePacket(frame);
        if (protocol == NetworkLayer.PROTOCOL_NONE) {
            frame.position(start);
            return false;
        }

        final int destination = frame.getInt(start + FRAME_HEADER_SIZE + NetworkLayer.IPv4_DESTINATION_OFFSET);
        final MacAddress destinationMacAddress = guestMacAddresses.get(destination);
        if (destinationMacAddress == null) {
            // We never learned who holds this address, and guessing would mean flooding the segment
            // with someone else's traffic. Dropping costs one retransmission.
            frame.position(start);
            return false;
        }

        writeEthernetHeader(frame, start, destinationMacAddress, protocol);
        frame.position(start);
        return true;
    }

    public void sendEthernetFrame(final ByteBuffer frame) {
        if (frame.remaining() < FRAME_HEADER_SIZE) {
            return;
        }
        if (frame.remaining() > FRAME_SIZE) {
            return;
        }

        final short destinationMacPrefix = frame.getShort();
        final int destinationMacAddress = frame.getInt();
        final short sourceMacPrefix = frame.getShort();
        final int sourceMacAddress = frame.getInt();
        final short protocol = frame.getShort();

        if (!isAddressedToUs(destinationMacPrefix, destinationMacAddress)) {
            // The hub floods every frame on the segment to us, including what guests say to each
            // other. Proxying that would leak their conversation to the outside world.
            return;
        }

        if (protocol == PROTOCOL_ARP) {
            handleArpRequest(frame, sourceMacPrefix, sourceMacAddress);
        } else {
            if (protocol == NetworkLayer.PROTOCOL_IPv4
                && frame.remaining() >= NetworkLayer.IPv4_SOURCE_OFFSET + 4) {
                // An IP packet without a preceding ARP still says who holds the source address.
                learnGuest(frame.getInt(frame.position() + NetworkLayer.IPv4_SOURCE_OFFSET),
                    new MacAddress(sourceMacPrefix, sourceMacAddress));
            }
            networkLayer.sendPacket(protocol, frame);
        }
    }

    // --------------------------------------------------------------------- //

    private static MacAddress randomMacAddress() {
        return new MacAddress(MAC_PREFIX, ThreadLocalRandom.current().nextInt());
    }

    private boolean isAddressedToUs(final short macPrefix, final int macAddress) {
        final boolean broadcast = macPrefix == BROADCAST_MAC_PREFIX && macAddress == BROADCAST_MAC_ADDRESS;
        return broadcast
            || (macPrefix == gatewayMacAddress.prefix() && macAddress == gatewayMacAddress.address());
    }

    private void handleArpRequest(final ByteBuffer frame, final short sourceMacPrefix, final int sourceMacAddress) {
        if (frame.remaining() < ARP_MESSAGE_SIZE) {
            return;
        }

        if (frame.getInt() != ARP_ADDRESS_TYPE) {
            return;
        }
        if (frame.getShort() != ARP_ADDRESS_SIZES) {
            return;
        }
        if (frame.getShort() != ARP_REQUEST) {
            return;
        }

        final short senderMacPrefix = frame.getShort();
        final int senderMacAddress = frame.getInt();
        if (senderMacPrefix != sourceMacPrefix || senderMacAddress != sourceMacAddress) {
            return;
        }

        final int senderIpAddress = frame.getInt();
        frame.getShort(); // Target MAC, unset in a request.
        frame.getInt();
        final int targetIpAddress = frame.getInt();

        final MacAddress senderMac = new MacAddress(senderMacPrefix, senderMacAddress);

        if (senderIpAddress == 0) {
            // An ARP probe: the sender holds no address yet and is testing whether one is free.
            // Answering would tell a DHCP client the address is taken and make it decline its lease.
            return;
        }

        if (targetIpAddress == senderIpAddress) {
            // A gratuitous ARP, announcing the guest's own address rather than asking for ours.
            learnGuest(senderIpAddress, senderMac);
            return;
        }

        if (guestMacAddresses.containsKey(targetIpAddress)) {
            // Another machine on the segment holds this address. Answering would take its identity
            // over, and would latch us onto an address that is not ours.
            return;
        }

        if (hasGatewayIpAddress && targetIpAddress != gatewayIpAddress) {
            // The guest is looking for some other host on its subnet, which is not us.
            return;
        }

        learnGuest(senderIpAddress, senderMac);
        gatewayIpAddress = targetIpAddress;
        hasGatewayIpAddress = true;

        if (pendingArpReplies.size() >= MAX_PENDING_ARP_REPLIES) {
            return;
        }
        pendingArpReplies.addLast(new PendingArpReply(senderMac, senderIpAddress, targetIpAddress));
    }

    private void learnGuest(final int ipAddress, final MacAddress macAddress) {
        if (ipAddress == 0) {
            // The unspecified address, as a DHCP client uses until it has a lease. It identifies
            // nobody, so remembering it would only cost a real entry its place.
            return;
        }

        if (hasGatewayIpAddress && ipAddress == gatewayIpAddress) {
            // We answered for this address before its owner spoke up. It is theirs, so let go of it
            // and take the next address a guest asks us for instead.
            hasGatewayIpAddress = false;
        }

        guestMacAddresses.remove(ipAddress);

        if (guestMacAddresses.size() >= MAX_GUESTS) {
            final Iterator<Map.Entry<Integer, MacAddress>> iterator = guestMacAddresses.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }

        guestMacAddresses.put(ipAddress, macAddress);
    }

    private void writeArpReply(final ByteBuffer frame, final int start, final PendingArpReply reply) {
        writeEthernetHeader(frame, start, reply.requesterMacAddress(), PROTOCOL_ARP);
        frame.position(start + FRAME_HEADER_SIZE);
        frame.putInt(ARP_ADDRESS_TYPE);
        frame.putShort(ARP_ADDRESS_SIZES);
        frame.putShort(ARP_REPLY);
        frame.putShort(gatewayMacAddress.prefix());
        frame.putInt(gatewayMacAddress.address());
        frame.putInt(reply.targetIpAddress());
        frame.putShort(reply.requesterMacAddress().prefix());
        frame.putInt(reply.requesterMacAddress().address());
        frame.putInt(reply.requesterIpAddress());
        frame.limit(frame.position());
        frame.position(start);
    }

    private void writeEthernetHeader(final ByteBuffer frame, final int start,
                                     final MacAddress destinationMacAddress, final short protocol) {
        frame.putShort(start, destinationMacAddress.prefix());
        frame.putInt(start + 2, destinationMacAddress.address());
        frame.putShort(start + 6, gatewayMacAddress.prefix());
        frame.putInt(start + 8, gatewayMacAddress.address());
        frame.putShort(start + 12, protocol);
    }

    // --------------------------------------------------------------------- //

    private record PendingArpReply(MacAddress requesterMacAddress, int requesterIpAddress, int targetIpAddress) {
    }
}
