package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.*;
import li.cil.oc2.api.inet.layer.NetworkLayer;
import li.cil.oc2.api.inet.layer.TransportLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;

public final class DefaultNetworkLayer implements NetworkLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////

    private static final int IPv4_HEADER_SIZE = 20;
    private static final int IPv4_VERSION = 4; // obviously...

    ////////////////////////////////////////////////////////////////////////

    private final TransportLayer transportLayer;

    private final TransportMessage inMessage = new TransportMessage();
    private final TransportMessage outMessage = new TransportMessage();

    private final InternetManagerImpl internetManager;

    public DefaultNetworkLayer(final LayerParameters layerParameters, final TransportLayer transportLayer) {
        this.internetManager = (InternetManagerImpl) layerParameters.getInternetManager();
        this.transportLayer = transportLayer;
    }

    @Override
    public Optional<Tag> onSave() {
        return transportLayer.onSave().map(transportLayerState -> {
            final CompoundTag networkLayerState = new CompoundTag();
            networkLayerState.put(TransportLayer.LAYER_NAME, transportLayerState);
            return networkLayerState;
        });
    }

    @Override
    public void onStop() {
        transportLayer.onStop();
    }

    @Override
    public short receivePacket(final ByteBuffer packet) {
        // Try to receive something
        packet.position(packet.position() + IPv4_HEADER_SIZE);
        inMessage.initializeBuffer(packet);
        final byte protocol = transportLayer.receiveTransportMessage(inMessage);
        if (protocol == TransportLayer.PROTOCOL_NONE || !inMessage.isIpv4()) {
            return PROTOCOL_NONE;
        }

        // Prepare IP packet
        final int srcIpAddress = inMessage.getSrcIpv4Address();
        final int dstIpAddress = inMessage.getDstIpv4Address();
        final int bodySize = packet.remaining();

        packet.position(packet.position() - IPv4_HEADER_SIZE);
        packet.put((byte) ((IPv4_VERSION << 4) | 5));
        packet.put((byte) 0);
        packet.putShort((short) (IPv4_HEADER_SIZE + bodySize));
        packet.putShort((short) random.nextInt());
        packet.putShort((short) 0);
        packet.put(inMessage.getTtl());
        packet.put(protocol);
        packet.putShort((short) 0);
        packet.putInt(srcIpAddress);
        packet.putInt(dstIpAddress);

        // Calculate header checksum
        packet.position(packet.position() - IPv4_HEADER_SIZE);
        short checksum = InetUtils.rfc1071Checksum(packet, IPv4_HEADER_SIZE);
        packet.position(packet.position() - 10);
        packet.putShort(checksum);
        packet.position(packet.position() + 8 - IPv4_HEADER_SIZE);

        return PROTOCOL_IPv4;
    }

    @Override
    public void sendPacket(final short protocol, final ByteBuffer packet) {
        if (protocol != PROTOCOL_IPv4) {
            LOGGER.info("Unsupported network protocol");
            return;
        }
        if (packet.remaining() < IPv4_HEADER_SIZE) {
            LOGGER.info("IP header is too small");
            return;
        }
        final byte versionAndIhl = packet.get();
        if ((versionAndIhl >>> 4) != IPv4_VERSION) {
            LOGGER.info("Invalid protocol version");
            return;
        }
        final int headerSize = (versionAndIhl & 0xF) * 4;
        if (headerSize < IPv4_HEADER_SIZE || packet.remaining() < headerSize) {
            LOGGER.info("Invalid header size");
            return;
        }
        packet.get(); // too hard, ignore
        int messageLength = Short.toUnsignedInt(packet.getShort());
        if (packet.remaining() + 4 < messageLength) {
            LOGGER.info("Packet size is lower than IP message size");
            return;
        }
        packet.getShort(); // normally, we don't expect message to be fragmented
        short flagsAndFragmentOffset = packet.getShort();
        if (((flagsAndFragmentOffset >>> 13) & 0b101) != 0) {
            LOGGER.info("Fragmented packet prohibited (1)");
            return; // no fragments!
        }
        if ((flagsAndFragmentOffset & 0x1FFF) != 0) {
            LOGGER.info("Fragmented packet prohibited (2)");
            return; // no fragments!
        }
        byte ttl = (byte) (packet.get() - 1);
        if (ttl == 0) {
            LOGGER.info("Small TTL value");
            return;
        }
        byte transportProtocol = packet.get();
        packet.getShort(); // I don't think, that we should expect packet corruption in Minecraft
        int srcIpAddress = packet.getInt();
        int dstIpAddress = packet.getInt();
        if (!internetManager.isAllowedToConnect(dstIpAddress)) {
            LOGGER.info("Forbidden IP address");
            return;
        }
        packet.position(packet.position() + headerSize - IPv4_HEADER_SIZE); // skip options
        packet.limit(packet.position() + messageLength - headerSize); // set correct limit

        /// Next layer
        LOGGER.info("Transport message received");
        outMessage.initializeBuffer(packet);
        outMessage.updateIpv4(srcIpAddress, dstIpAddress, ttl);
        transportLayer.sendTransportMessage(transportProtocol, outMessage);
    }
}
