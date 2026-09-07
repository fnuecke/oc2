/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Utility wrapper message for client to server messages exceeding the regular custom payload size.
 */
public final class MultipartMessage extends AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger(MultipartMessage.class);

    private static final int MAX_MULTIPART_MESSAGE_SIZE = 1024 * Constants.KILOBYTE;
    private static final int MAX_PAYLOAD_SIZE = ServerboundCustomPayloadPacket.MAX_PAYLOAD_SIZE;
    private static final int MAX_IN_FLIGHT_PER_CLIENT = 4;
    private static final int HEADER_SIZE =
        4 /* message id */ +
            4 /* multipart message id */ +
            1 /* is final part */ +
            2 /* length */;

    // --------------------------------------------------------------------- //

    /**
     * Cache for collecting multipart messages on the server into one big buffer again. Discard them after some
     * time to avoid malicious clients being able to grow the memory used by this cache to grow infinitely.
     * <p>
     * Keyed by sender <em>and</em> packet id: multiple clients can be uploading at the same time, and a single
     * client may interleave uploads.
     */
    private static final Cache<BufferKey, ByteBuf> MULTIPART_MESSAGE_BUFFER_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .maximumSize(256) // max across all clients
        .build();
    private static int lastAssignedMultipartMessageId;

    // --------------------------------------------------------------------- //

    private static final Map<Class<? extends AbstractMessage>, Entry> ENTRY_BY_TYPE = new HashMap<>();
    private static final Int2ObjectMap<Entry> ENTRY_BY_ID = new Int2ObjectArrayMap<>();
    private static int lastAssignedId;

    public static <T extends AbstractMessage> void registerMessage(final Class<T> type, final Function<RegistryFriendlyByteBuf, T> factory) {
        if (ENTRY_BY_TYPE.containsKey(type)) {
            throw new IllegalArgumentException("Message of this type has already been registered.");
        }
        final int id = ++lastAssignedId;
        final Entry entry = new Entry(id, factory);
        ENTRY_BY_TYPE.put(type, entry);
        ENTRY_BY_ID.put(id, entry);
    }

    static int messageIdOf(final Class<? extends AbstractMessage> type) {
        final Entry entry = ENTRY_BY_TYPE.get(type);
        if (entry == null) {
            throw new IllegalArgumentException("Trying to send multipart message of unregistered message (" + type.getName() + ").");
        }

        return entry.id();
    }

    // --------------------------------------------------------------------- //

    public static void sendToServer(final AbstractMessage message) {
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
            requireNonNull(Minecraft.getInstance().level).registryAccess());
        message.toBytes(buffer);
        if (buffer.readableBytes() <= MAX_PAYLOAD_SIZE) {
            // Message fits into one custom payload packet, send it as is.
            Network.sendToServer(message);
            return;
        }
        if (buffer.readableBytes() > MAX_MULTIPART_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message too large.");
        }

        final int messageId = messageIdOf(message.getClass());
        final int multipartMessageId = ++lastAssignedMultipartMessageId;

        while (buffer.readableBytes() > 0) {
            final int dataLength = Math.min(buffer.readableBytes(), MAX_PAYLOAD_SIZE - HEADER_SIZE);
            final byte[] data = new byte[dataLength];
            buffer.readBytes(data);
            Network.sendToServer(new MultipartMessage(messageId, multipartMessageId, buffer.readableBytes() == 0, data));
        }
    }

    // --------------------------------------------------------------------- //

    private boolean isFinalPart;

    private int messageId;
    private int multipartMessageId;
    private byte[] data;

    // --------------------------------------------------------------------- //

    public MultipartMessage(final int messageId, final int multipartMessageId, final boolean isFinalPart, final byte[] data) {
        this.messageId = messageId;
        this.multipartMessageId = multipartMessageId;
        this.isFinalPart = isFinalPart;
        this.data = data;
    }

    public MultipartMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        messageId = buffer.readInt();
        multipartMessageId = buffer.readInt();
        isFinalPart = buffer.readBoolean();
        final int length = buffer.readUnsignedShort();
        data = new byte[length];
        buffer.readBytes(data);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(messageId);
        buffer.writeInt(multipartMessageId);
        buffer.writeBoolean(isFinalPart);
        buffer.writeShort(data.length);
        buffer.writeBytes(data);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        final Player player = context.getPlayer();
        if (player == null) {
            return;
        }

        final Entry entry = ENTRY_BY_ID.get(messageId);
        if (entry == null) {
            LOGGER.error("Received multipart message for unregistered message from client [{}]. Are the mod version on the server and client the same?", player);
            return;
        }

        final BufferKey key = new BufferKey(player.getUUID(), multipartMessageId);

        try {
            final ByteBuf buffer = MULTIPART_MESSAGE_BUFFER_CACHE.get(key, Unpooled::buffer);
            if (buffer.capacity() == 0) {
                return; // Invalidated entry due to being over-sized.
            }

            if (buffer.readableBytes() == 0 && countInFlight(player.getUUID()) > MAX_IN_FLIGHT_PER_CLIENT) {
                LOGGER.error("Client [{}] has too many multipart messages in flight, ignoring.", player);
                MULTIPART_MESSAGE_BUFFER_CACHE.put(key, Unpooled.buffer(0));
                return;
            }

            buffer.writeBytes(data);
            if (buffer.readableBytes() > MAX_MULTIPART_MESSAGE_SIZE) {
                LOGGER.error("Received over-sized multipart message from client [{}], ignoring.", player);
                MULTIPART_MESSAGE_BUFFER_CACHE.put(key, Unpooled.buffer(0));
                return;
            }

            if (isFinalPart) {
                MULTIPART_MESSAGE_BUFFER_CACHE.invalidate(key);

                entry.factory.apply(new RegistryFriendlyByteBuf(buffer, context.registryAccess())).handleMessage(context);
            }
        } catch (final ExecutionException e) {
            LOGGER.error("Error when handling multipart message received from client [{}]: {}", player, e);
        }
    }

    private static long countInFlight(final UUID player) {
        return MULTIPART_MESSAGE_BUFFER_CACHE.asMap().keySet().stream()
            .filter(key -> key.player().equals(player))
            .count();
    }

    // --------------------------------------------------------------------- //

    private record BufferKey(UUID player, int multipartMessageId) {
    }

    private record Entry(int id, Function<RegistryFriendlyByteBuf, ? extends AbstractMessage> factory) {
    }
}
