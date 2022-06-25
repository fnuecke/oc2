package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public final class SocketManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static int socketManagerUsesCount = 0;
    private static SocketManager socketManager = null;

    public static SocketManager attach(final InternetManager internetManager) {
        if (socketManagerUsesCount++ == 0) {
            assert socketManager == null;
            socketManager = new SocketManager(internetManager);
        }
        assert socketManager != null;
        return socketManager;
    }

    private final Selector selector;
    private final InternetManager.Task selectionTask;

    private void selectionTaskFunction() {
        try {
            selector.selectNow(selectionKey -> {
                final ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
                final Session session = attachment.session;
                final ReadySessions readySessions = attachment.readySessions;
                if (selectionKey.isReadable()) {
                    readySessions.getToRead().add(session);
                }
                if (selectionKey.isWritable()) {
                    readySessions.getToWrite().add(session);
                }
                if (selectionKey.isConnectable()) {
                    readySessions.getToConnect().add(session);
                }
            });
        } catch (final IOException exception) {
            LOGGER.error("Exception while selecting", exception);
        }
    }

    private SocketManager(final InternetManager internetManager) {
        try {
            selector = Selector.open();
        } catch (final IOException exception) {
            throw new Error("Failed to open selector", exception);
        }
        selectionTask = internetManager.runOnInternetThreadTick(this::selectionTaskFunction);
        LOGGER.info("Started socket manager");
    }

    private record ChannelAttachment(Session session, ReadySessions readySessions) {
    }

    public DatagramChannel createDatagramChannel(final DatagramSession session,
                                              final ReadySessions readySessions) throws IOException {
        final DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
        final ChannelAttachment attachment = new ChannelAttachment(session, readySessions);
        final int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        datagramChannel.register(selector, ops, attachment);
        return datagramChannel;
    }

    public SocketChannel createStreamChannel(final StreamSession session,
                                            final ReadySessions readySessions) throws IOException {
        final SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        final ChannelAttachment attachment = new ChannelAttachment(session, readySessions);
        final int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
        socketChannel.register(selector, ops, attachment);
        return socketChannel;
    }

    private void shutdown() {
        selectionTask.close();
        try {
            selector.close();
        } catch (final IOException exception) {
            LOGGER.error("Exception during socket manager shutdown", exception);
        }
        LOGGER.info("Stopped socket manager");
    }

    public void detach() {
        if (--socketManagerUsesCount == 0) {
            shutdown();
            socketManager = null;
        }
    }
}
