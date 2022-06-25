package li.cil.oc2.api.inet.layer;

import li.cil.oc2.api.inet.InternetDeviceLifecycle;
import li.cil.oc2.api.inet.provider.NetworkLayerInternetProvider;

import java.nio.ByteBuffer;

/**
 * Link local or channel TCP/IP layer interface.
 * <p>
 * There is a default link local layer implementation that uses provided {@link NetworkLayer} implementation
 * (see {@link NetworkLayerInternetProvider} for more information).
 */
public interface LinkLocalLayer extends InternetDeviceLifecycle {
    /**
     * Ethernet frame header size. Consists of two ethernet (MAC) addresses and protocol number.
     */
    int FRAME_HEADER_SIZE = 14;

    /**
     * Default ethernet frame body size that is used in Linux.
     */
    int DEFAULT_MTU = 1500;

    /**
     * Default ethernet frame size.
     */
    int FRAME_SIZE = FRAME_HEADER_SIZE + DEFAULT_MTU;

    String LAYER_NAME = "LinkLocal";

    ////////////////////////////////////////////////////////////////////

    /**
     * Tries to get the next ethernet frame to send it to virtual computer later.
     * <p>
     * Normally, this method is invoked every game tick in the Internet thread.
     *
     * @param frame byte buffer where frame body should be put
     * @return should return false, if no ethernet frame were gathered, and true otherwise
     */
    default boolean receiveEthernetFrame(final ByteBuffer frame) {
        return false;
    }

    /**
     * Sends an ethernet frame from virtual computer.
     *
     * @param frame byte buffer filled with ethernet frame data
     */
    default void sendEthernetFrame(final ByteBuffer frame) {

    }
}
