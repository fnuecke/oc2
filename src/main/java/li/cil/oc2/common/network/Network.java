package li.cil.oc2.common.network;

import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.TerminalBlockInputMessage;
import li.cil.oc2.common.network.message.TerminalBlockOutputMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Network {
    private static final Identifier CHANNEL = new Identifier(API.MOD_ID, "main");

    private static final Map<NetworkDirection, Int2ObjectArrayMap<MessageDefinition<?>>> MESSAGES_BY_ID = Maps.newEnumMap(NetworkDirection.class);
    private static final Map<NetworkDirection, Map<Class<?>, MessageDefinition<?>>> MESSAGES_BY_TYPE = Maps.newEnumMap(NetworkDirection.class);

    ///////////////////////////////////////////////////////////////////

    private static int nextPacketId = 1;

    ///////////////////////////////////////////////////////////////////

    public static void setup() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, Network::handleMessageOnServer);
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, Network::handleMessageOnClient);

        messageBuilder(TerminalBlockOutputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TerminalBlockOutputMessage::toBytes)
                .decoder(TerminalBlockOutputMessage::new)
                .consumer(TerminalBlockOutputMessage::handleMessage)
                .add();

        messageBuilder(TerminalBlockInputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(TerminalBlockInputMessage::toBytes)
                .decoder(TerminalBlockInputMessage::new)
                .consumer(TerminalBlockInputMessage::handleMessage)
                .add();

        messageBuilder(ComputerRunStateMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerRunStateMessage::toBytes)
                .decoder(ComputerRunStateMessage::new)
                .consumer(ComputerRunStateMessage::handleMessage)
                .add();

        messageBuilder(ComputerBusStateMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerBusStateMessage::toBytes)
                .decoder(ComputerBusStateMessage::new)
                .consumer(ComputerBusStateMessage::handleMessage)
                .add();
    }

    public static <T> void sendToClientsTrackingChunk(final T message, final ServerWorld world, final ChunkPos chunkPos) {
        final Collection<ServerPlayerEntity> players = PlayerLookup.tracking(world, chunkPos);
        if (players.isEmpty()) {
            return;
        }

        final PacketByteBuf buffer = encodeMessage(message, NetworkDirection.PLAY_TO_CLIENT);
        for (final ServerPlayerEntity player : players) {
            ServerPlayNetworking.send(player, CHANNEL, buffer);
        }
    }

    public static <T> void sendToServer(final T message) {
        final PacketByteBuf buffer = encodeMessage(message, NetworkDirection.PLAY_TO_SERVER);
        ClientPlayNetworking.send(CHANNEL, buffer);
    }

    ///////////////////////////////////////////////////////////////////

    private static int getNextPacketId() {
        return nextPacketId++;
    }

    private static <T> MessageBuilder<T> messageBuilder(final Class<T> type, final int id, final NetworkDirection direction) {
        return new MessageBuilder<>(type, id, direction);
    }

    private static void handleMessageOnServer(final MinecraftServer server,
                                              final ServerPlayerEntity playerEntity,
                                              final ServerPlayNetworkHandler networkHandler,
                                              final PacketByteBuf buffer,
                                              final PacketSender sender) {
        handleMessage(buffer, NetworkDirection.PLAY_TO_SERVER, new MessageContext(server, playerEntity, sender, server));

    }

    private static void handleMessageOnClient(final MinecraftClient client,
                                              final ClientPlayNetworkHandler networkHandler,
                                              final PacketByteBuf buffer,
                                              final PacketSender sender) {
        handleMessage(buffer, NetworkDirection.PLAY_TO_CLIENT, new MessageContext(null, null, sender, client));
    }

    @SuppressWarnings("unchecked")
    private static <T> PacketByteBuf encodeMessage(final T message, final NetworkDirection direction) {
        final MessageDefinition<T> definition = (MessageDefinition<T>) MESSAGES_BY_TYPE.get(direction).get(message.getClass());
        final PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());

        buffer.writeVarInt(definition.getId());
        definition.encode(message, buffer);

        return buffer;
    }

    private static <T> void handleMessage(final PacketByteBuf buffer, final NetworkDirection direction, final MessageContext context) {
        final int id = buffer.readVarInt();
        final MessageDefinition<?> definition = MESSAGES_BY_ID.get(direction).get(id);

        definition.handleMessage(buffer, context);
    }

    ///////////////////////////////////////////////////////////////////

    public static final class MessageContext {
        private final MinecraftServer server;
        private final ServerPlayerEntity playerEntity;
        private final PacketSender sender;
        private final ThreadExecutor<?> executor;

        public MessageContext(@Nullable final MinecraftServer server, @Nullable final ServerPlayerEntity playerEntity, final PacketSender sender, final ThreadExecutor<?> executor) {
            this.server = server;
            this.playerEntity = playerEntity;
            this.sender = sender;
            this.executor = executor;
        }

        @Nullable
        public MinecraftServer getServer() {
            return server;
        }

        @Nullable
        public ServerPlayerEntity getServerPlayer() {
            return playerEntity;
        }

        public PacketSender getSender() {
            return sender;
        }

        public void enqueueWork(final Runnable work) {
            executor.execute(work);
        }
    }

    private enum NetworkDirection {
        PLAY_TO_CLIENT,
        PLAY_TO_SERVER;
    }

    private static final class MessageDefinition<T> {
        private final int id;
        private final BiConsumer<T, PacketByteBuf> encoder;
        private final Function<PacketByteBuf, T> decoder;
        private final BiConsumer<T, MessageContext> consumer;

        public MessageDefinition(final int id, final BiConsumer<T, PacketByteBuf> encoder, final Function<PacketByteBuf, T> decoder, final BiConsumer<T, MessageContext> consumer) {
            this.id = id;
            this.encoder = encoder;
            this.decoder = decoder;
            this.consumer = consumer;
        }

        public int getId() {
            return id;
        }

        public void encode(final T message, final PacketByteBuf buffer) {
            encoder.accept(message, buffer);
        }

        public void handleMessage(final PacketByteBuf buffer, final MessageContext context) {
            consumer.accept(decoder.apply(buffer), context);
        }
    }

    private static final class MessageBuilder<T> {
        private final Class<T> type;
        private final int id;
        private final NetworkDirection direction;
        private BiConsumer<T, PacketByteBuf> encoder;
        private Function<PacketByteBuf, T> decoder;
        private BiConsumer<T, MessageContext> consumer;

        public MessageBuilder(final Class<T> type, final int id, final NetworkDirection direction) {
            this.type = type;
            this.id = id;
            this.direction = direction;
        }

        public MessageBuilder<T> encoder(final BiConsumer<T, PacketByteBuf> value) {
            this.encoder = value;
            return this;
        }

        public MessageBuilder<T> decoder(final Function<PacketByteBuf, T> value) {
            this.decoder = value;
            return this;
        }

        public MessageBuilder<T> consumer(final BiConsumer<T, MessageContext> value) {
            this.consumer = value;
            return this;
        }

        public void add() {
            final MessageDefinition<T> definition = new MessageDefinition<>(id, encoder, decoder, consumer);
            MESSAGES_BY_ID.computeIfAbsent(direction, unused -> new Int2ObjectArrayMap<>()).put(id, definition);
            MESSAGES_BY_TYPE.computeIfAbsent(direction, unused -> new HashMap<>()).put(type, definition);
        }
    }
}
