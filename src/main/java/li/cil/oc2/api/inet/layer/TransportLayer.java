package li.cil.oc2.api.inet.layer;

import li.cil.oc2.api.inet.InternetDeviceLifecycle;
import li.cil.oc2.api.inet.TransportMessage;
import li.cil.oc2.api.inet.provider.SessionLayerInternetProvider;

/**
 * Transport TCP/IP layer interface.
 * <p>
 * There is a default transport layer implementation that uses provided {@link SessionLayer} implementation
 * (see {@link SessionLayerInternetProvider} for more information).
 */
public interface TransportLayer extends InternetDeviceLifecycle {

    /**
     * The value of this constant should be returned by {@link TransportLayer#receiveTransportMessage(TransportMessage)}
     * method if no data is arrived.
     */
    byte PROTOCOL_NONE = 0;

    /**
     * The value of this constant should be returned by {@link TransportLayer#receiveTransportMessage(TransportMessage)}
     * method if an ICMP message is arrived.
     */
    byte PROTOCOL_ICMP = 1;

    /**
     * The value of this constant should be returned by {@link TransportLayer#receiveTransportMessage(TransportMessage)}
     * method if a TCP packet is arrived.
     */
    byte PROTOCOL_TCP = 6;

    /**
     * The value of this constant should be returned by {@link TransportLayer#receiveTransportMessage(TransportMessage)}
     * method if a UDP message is arrived.
     */
    byte PROTOCOL_UDP = 17;

    String LAYER_NAME = "Transport";

    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * Tries to get the next transport message to wrap it into an IP packet and send it to virtual computer later.
     * <p>
     * Normally, this method is invoked every game tick in the Internet thread.
     *
     * @param message transport message object that should be filled with arrived data
     * @return protocol number of received data (can be, for example, {@link TransportLayer#PROTOCOL_ICMP},
     * {@link TransportLayer#PROTOCOL_TCP} or {@link TransportLayer#PROTOCOL_UDP}) or
     * {@link TransportLayer#PROTOCOL_NONE}, if no new data has arrived
     */
    default byte receiveTransportMessage(final TransportMessage message) {
        return PROTOCOL_NONE;
    }

    /**
     * Sends a transport message extracted from an IP packet that sent virtual computer.
     *
     * @param protocol protocol number of arrived message
     * @param message  arrived transport message
     */
    default void sendTransportMessage(byte protocol, final TransportMessage message) {

    }
}
