package li.cil.oc2.api.inet.layer;

import li.cil.oc2.api.inet.InternetDeviceLifecycle;
import li.cil.oc2.api.inet.provider.TransportLayerInternetProvider;

import java.nio.ByteBuffer;

/**
 * Network TCP/IP layer interface.
 * <p>
 * There is a default network layer implementation that uses provided {@link TransportLayer} implementation
 * (see {@link TransportLayerInternetProvider} for more information).
 */
public interface NetworkLayer extends InternetDeviceLifecycle {

    /**
     * The value of this constant should be returned by {@link NetworkLayer#receivePacket(ByteBuffer)} method if no
     * data is arrived.
     */
    short PROTOCOL_NONE = 0;

    /**
     * The value of this constant should be returned by {@link NetworkLayer#receivePacket(ByteBuffer)} method if IPv4
     * packet is arrived.
     */
    short PROTOCOL_IPv4 = 0x0800;

    /**
     * The value of this constant should be returned by {@link NetworkLayer#receivePacket(ByteBuffer)} method if any IP
     * packet is arrived (can be either IPv4 packet or IPv6 packet).
     * <p>
     * Normally, this value should be returned if any data is arrived.
     */
    short PROTOCOL_IP = PROTOCOL_IPv4;

    /**
     * The value of this constant should be returned by {@link NetworkLayer#receivePacket(ByteBuffer)} method if IPv6
     * packet is arrived.
     */
    short PROTOCOL_IPv6 = (short) 0x86dd;

    String LAYER_NAME = "Network";

    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * Tries to get the next IP paket to wrap it into an ethernet frame and send it to virtual computer later.
     * <p>
     * Normally, this method is invoked every game tick in the Internet thread.
     *
     * @param packet byte buffer where IP packet body should be put
     * @return protocol number of received data (either {@link NetworkLayer#PROTOCOL_IP},
     * {@link NetworkLayer#PROTOCOL_IPv4} or {@link NetworkLayer#PROTOCOL_IPv6}) or
     * {@link NetworkLayer#PROTOCOL_NONE}, if no new data has arrived
     */
    default short receivePacket(final ByteBuffer packet) {
        return PROTOCOL_NONE;
    }

    /**
     * Sends an IP packet extracted from an ethernet frame that sent virtual computer.
     *
     * @param protocol protocol number of arrived message; normally, should be either
     *                 {@link NetworkLayer::PROTOCOL_IPv4} or {@link NetworkLayer::PROTOCOL_IPv6}
     * @param packet   arrived data
     */
    default void sendPacket(final short protocol, final ByteBuffer packet) {

    }
}
