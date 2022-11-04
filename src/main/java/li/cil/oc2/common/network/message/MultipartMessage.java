/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility wrapper message for client to server messages exceeding the regular custom payload size.
 */
public final class MultipartMessage extends AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_MULTIPART_MESSAGE_SIZE = 1024 * Constants.KILOBYTE;
    private static final int MAX_PAYLOAD_SIZE = 8 * Constants.KILOBYTE;
    private static final int HEADER_SIZE =
        1 /* forge message index */ +
            4 /* message id */ +
            4 /* multipart message id */ +
            2 /* length */;

    ///////////////////////////////////////////////////////////////////

    /**
     * Cache for collecting multipart messages on the server into one big buffer again. Discard them after some
     * time to avoid malicious clients being able to grow the memory used by this cache to grow infinitely.
     */
    private static final Cache<Integer, ByteBuf> MULTIPART_MESSAGE_BUFFER_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .build();
    private static int lastAssignedMultipartMessageId;

    ///////////////////////////////////////////////////////////////////

    private static final Map<Class<? extends AbstractMessage>, Entry> ENTRY_BY_TYPE = new HashMap<>();
    private static final Int2ObjectMap<Entry> ENTRY_BY_ID = new Int2ObjectArrayMap<>();
    private static int lastAssignedId;

    public static <T extends AbstractMessage> void registerMessage(final Class<T> type, final Function<FriendlyByteBuf, T> factory) {
        if (ENTRY_BY_TYPE.containsKey(type)) {
            throw new IllegalArgumentException("Message of this type has already been registered.");
        }
        final int id = ++lastAssignedId;
        final Entry entry = new Entry(id, factory);
        ENTRY_BY_TYPE.put(type, entry);
        ENTRY_BY_ID.put(id, entry);
    }

    ///////////////////////////////////////////////////////////////////

    public static void sendToServer(final AbstractMessage message) {
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        message.toBytes(buffer);
        if (buffer.readableBytes() <= MAX_PAYLOAD_SIZE) {
            // Message fits into one custom payload packet, send it as is.
            Network.sendToServer(message);
            return;
        }
        if (buffer.readableBytes() > MAX_MULTIPART_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message too large.");
        }

        final Entry entry = ENTRY_BY_TYPE.get(message.getClass());
        if (entry == null) {
            throw new IllegalArgumentException("Trying to send multipart message of unregistered message (" + message.getClass().getName() + ").");
        }

        final int messageId = entry.id();
        final int multipartMessageId = ++lastAssignedMultipartMessageId;

        while (buffer.readableBytes() > 0) {
            final int dataLength = Math.min(buffer.readableBytes(), MAX_PAYLOAD_SIZE - HEADER_SIZE);
            final byte[] data = new byte[dataLength];
            buffer.readBytes(data);
            Network.sendToServer(new MultipartMessage(messageId, multipartMessageId, data));
        }
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Automatically computed on client. Implicit because all but last packets are max size.
     */
    private boolean isFinalPart;

    private int messageId;
    private int multipartMessageId;
    private byte[] data;

    ///////////////////////////////////////////////////////////////////

    public MultipartMessage(final int messageId, final int multipartMessageId, final byte[] data) {
        this.messageId = messageId;
        this.multipartMessageId = multipartMessageId;
        this.data = data;
    }

    public MultipartMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        isFinalPart = buffer.readableBytes() < MAX_PAYLOAD_SIZE - 1 /* forge message index */;

        messageId = buffer.readInt();
        multipartMessageId = buffer.readInt();
        final int length = buffer.readUnsignedShort();
        data = new byte[length];
        buffer.readBytes(data);
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeInt(messageId);
        buffer.writeInt(multipartMessageId);
        buffer.writeShort(data.length);
        buffer.writeBytes(data);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final Supplier<NetworkEvent.Context> contextSupplier) {
        try {
            final ByteBuf buffer = MULTIPART_MESSAGE_BUFFER_CACHE.get(lastAssignedMultipartMessageId, Unpooled::buffer);
            if (buffer.capacity() == 0) {
                return; // Invalidated entry due to being over-sized.
            }

            buffer.writeBytes(data);
            if (buffer.readableBytes() > MAX_MULTIPART_MESSAGE_SIZE) {
                LOGGER.error("Received over-sized multipart message from client [{}], ignoring.", contextSupplier.get().getSender());
                MULTIPART_MESSAGE_BUFFER_CACHE.put(lastAssignedMultipartMessageId, Unpooled.buffer(0));
                return;
            }

            if (isFinalPart) {
                MULTIPART_MESSAGE_BUFFER_CACHE.invalidate(lastAssignedMultipartMessageId);

                final Entry entry = ENTRY_BY_ID.get(messageId);
                if (entry == null) {
                    LOGGER.error("Received multipart message for unregistered message from client [{}]. Are the mod version on the server and client the same?", contextSupplier.get().getSender());
                    return;
                }

                entry.factory.apply(new FriendlyByteBuf(buffer)).handleMessage(contextSupplier);
            }
        } catch (final ExecutionException e) {
            LOGGER.error("Error when handling multipart message received from client [{}]: {}", contextSupplier.get().getSender(), e);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private record Entry(int id, Function<FriendlyByteBuf, ? extends AbstractMessage> factory) { }
}
