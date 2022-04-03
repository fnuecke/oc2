package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LinkLocalLayer;
import li.cil.oc2.api.inet.NetworkLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Random;

public final class DefaultLinkLocalLayer implements LinkLocalLayer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random random = new Random();

    ///////////////////////////////////////////////////

    private static final short MAC_PREFIX = 0x5ed1;

    private static final short PROTOCOL_ARP = 0x0806;

    private static final short HW_TYPE_ETHERNET = 0x0001;

    private static final int ARP_MESSAGE_SIZE = 28;
    private static final int ARP_ADDRESS_TYPE = (HW_TYPE_ETHERNET << 16) | NetworkLayer.PROTOCOL_IPv4;
    private static final short ARP_ADDRESSES_SIZES = (6 << 8) | 4;

    private static final short ARP_REQUEST = 0x0001;
    private static final short ARP_RESPONSE = 0x0002;

    private static final int IP_VER4 = 4; // obviously
    private static final int IP_VER6 = 6; // obviously

    ///////////////////////////////////////////////////

    private final NetworkLayer networkLayer;

    private final int myMacAddress;
    private int myIpAddress = -1;

    private short cardMacPrefix = -1;
    private int cardMacAddress = -1;
    private int cardIpAddress = -1;

    private boolean needArpResponse = false;

    ///////////////////////////////////////////////////

    public DefaultLinkLocalLayer(final NetworkLayer networkLayer) {
        this.networkLayer = networkLayer;
        myMacAddress = random.nextInt();
    }

    private void prepareEthernetHeader(final ByteBuffer frame, final short protocol) {
        // Prepare ethernet header
        frame.putShort(cardMacPrefix);
        frame.putInt(cardMacAddress);
        frame.putShort(MAC_PREFIX);
        frame.putInt(myMacAddress);
        frame.putShort(protocol);
    }

    @Override
    public void onStop() {
        networkLayer.onStop();
    }

    @Override
    public boolean receiveEthernetFrame(final ByteBuffer frame) {
        if (needArpResponse) {
            // Make ARP response before anything else
            needArpResponse = false;

            prepareEthernetHeader(frame, PROTOCOL_ARP);

            // Prepare ARP response
            frame.putInt(ARP_ADDRESS_TYPE);
            frame.putShort(ARP_ADDRESSES_SIZES);
            frame.putShort(ARP_RESPONSE);
            frame.putShort(MAC_PREFIX);
            frame.putInt(myMacAddress);
            frame.putInt(myIpAddress);
            frame.putShort(cardMacPrefix);
            frame.putInt(cardMacAddress);
            frame.putInt(cardIpAddress);
            frame.position(frame.position() - FRAME_HEADER_SIZE - ARP_MESSAGE_SIZE);
            LOGGER.trace("ARP message sent");
        } else {
            // Wrap IP message
            frame.position(frame.position() + FRAME_HEADER_SIZE);
            short protocol = networkLayer.receivePacket(frame);
            if (protocol == NetworkLayer.PROTOCOL_NONE) {
                return false;
            }
            if (protocol == NetworkLayer.PROTOCOL_IP) {
                // This code block exists to make Network layer implementation a bit easier
                final int version = Byte.toUnsignedInt(frame.get(frame.position())) >>> 4;
                if (version == IP_VER6) {
                    protocol = NetworkLayer.PROTOCOL_IPv6;
                }
            }
            frame.position(frame.position() - FRAME_HEADER_SIZE);
            prepareEthernetHeader(frame, protocol);
            frame.position(frame.position() - FRAME_HEADER_SIZE);
            LOGGER.trace("IP message sent");
        }
        return true;
    }

    @Override
    public void sendEthernetFrame(final ByteBuffer frame) {
        /// Read ethernet header
        if (frame.remaining() < FRAME_HEADER_SIZE) {
            LOGGER.trace("Ethernet header too low");
            return;
        }
        // Get destination
        final short dstMacPrefix = frame.getShort();
        final int dstMacAddress = frame.getInt();
        // Get source
        final short srcMacPrefix = frame.getShort();
        final int srcMacAddress = frame.getInt();
        // Get protocol type
        final short protocol = frame.getShort();

        /// Protocol action
        if (protocol == PROTOCOL_ARP) {
            LOGGER.trace("ARP message received");
            /// ARP message verification
            if (frame.remaining() < ARP_MESSAGE_SIZE) {
                return;
            }
            final int hwAndProtocolAddressesTypes = frame.getInt();
            if (hwAndProtocolAddressesTypes != ARP_ADDRESS_TYPE) {
                LOGGER.trace("Wrong ARP address type, drop");
                return;
            }
            final short addressesSizes = frame.getShort();
            if (addressesSizes != ARP_ADDRESSES_SIZES) {
                LOGGER.trace("Wrong ARP address size, drop");
                return;
            }
            final short messageType = frame.getShort();
            if (messageType != ARP_REQUEST) {
                LOGGER.trace("Not an ARP request, drop");
                return;
            }
            final short senderMacPrefix = frame.getShort();
            final int senderMacAddress = frame.getInt();
            if (senderMacPrefix != srcMacPrefix || senderMacAddress != srcMacAddress) {
                LOGGER.trace("Wrong sender, drop");
                return;
            }

            /// Valid message, extracting useful data
            cardIpAddress = frame.getInt();
            // Do not care in target MAC address
            frame.getShort();
            frame.getInt();
            myIpAddress = frame.getInt();
            cardMacPrefix = senderMacPrefix;
            cardMacAddress = senderMacAddress;
            needArpResponse = true;
        } else {
            LOGGER.trace("Network message received");
            /// Network message forwarding
            networkLayer.sendPacket(protocol, frame);
        }
    }
}
