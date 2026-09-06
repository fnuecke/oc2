/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.socket;

import li.cil.oc2.common.inet.l4.AbstractSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SocketManager implements AutoCloseable {
    public static final class ReadySessions {
        private final Set<AbstractSession> toRead = new LinkedHashSet<>();
        private final Set<AbstractSession> toConnect = new LinkedHashSet<>();

        public Set<AbstractSession> toRead() {
            return toRead;
        }

        public Set<AbstractSession> toConnect() {
            return toConnect;
        }

        public void clear() {
            toRead.clear();
            toConnect.clear();
        }
    }

    // --------------------------------------------------------------------- //

    private static final Logger LOGGER = LogManager.getLogger();

    // --------------------------------------------------------------------- //

    private final Selector selector;

    // --------------------------------------------------------------------- //

    public SocketManager() throws IOException {
        selector = Selector.open();
    }

    // --------------------------------------------------------------------- //

    /**
     * Moves every ready channel into its owner's queues. Called once per tick.
     */
    public void poll() {
        try {
            selector.selectNow(key -> {
                final Registration registration = (Registration) key.attachment();
                if (key.isValid() && key.isConnectable()) {
                    registration.ready().toConnect().add(registration.session());
                }
                if (key.isValid() && key.isReadable()) {
                    registration.ready().toRead().add(registration.session());
                }
            });
        } catch (final IOException e) {
            LOGGER.error("Failed to poll internet sockets.", e);
        }
    }

    public void stopReading(final SelectableChannel channel) {
        final SelectionKey key = channel.keyFor(selector);
        if (key != null && key.isValid()) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
        }
    }

    public DatagramChannel openDatagramChannel(final AbstractSession session, final ReadySessions ready) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ, new Registration(session, ready));
        } catch (final IOException | RuntimeException e) {
            closeQuietly(channel);
            throw e;
        }
        return channel;
    }

    public SocketChannel openSocketChannel(final AbstractSession session, final ReadySessions ready) throws IOException {
        final SocketChannel channel = SocketChannel.open();
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT,
                new Registration(session, ready));
        } catch (final IOException | RuntimeException e) {
            closeQuietly(channel);
            throw e;
        }
        return channel;
    }

    @Override
    public void close() {
        // Closing the selector alone would leak every socket registered with it.
        for (final SelectionKey key : selector.keys()) {
            closeQuietly(key.channel());
        }
        try {
            selector.close();
        } catch (final IOException e) {
            LOGGER.error("Failed to close internet socket selector.", e);
        }
    }

    // --------------------------------------------------------------------- //

    private static void closeQuietly(final java.nio.channels.Channel channel) {
        try {
            channel.close();
        } catch (final IOException e) {
            // Nothing useful to do about a channel that will not close.
        }
    }

    // --------------------------------------------------------------------- //

    private record Registration(AbstractSession session, ReadySessions ready) {
    }
}
