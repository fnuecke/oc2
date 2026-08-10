/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class Network {
    private static final Map<Class<?>, CustomPacketPayload.Type<? extends CustomPacketPayload>> MESSAGE_TYPES = new HashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        registerMessage(ComputerTerminalOutputMessage.class, ComputerTerminalOutputMessage::new, NetworkManager.serverToClient());
        registerMessage(ComputerTerminalInputMessage.class, ComputerTerminalInputMessage::new, NetworkManager.clientToServer());
        registerMessage(ComputerRunStateMessage.class, ComputerRunStateMessage::new, NetworkManager.serverToClient());
        registerMessage(ComputerBusStateMessage.class, ComputerBusStateMessage::new, NetworkManager.serverToClient());
        registerMessage(ComputerBootErrorMessage.class, ComputerBootErrorMessage::new, NetworkManager.serverToClient());
        registerMessage(ComputerPowerMessage.class, ComputerPowerMessage::new, NetworkManager.clientToServer());
        registerMessage(OpenComputerInventoryMessage.class, OpenComputerInventoryMessage::new, NetworkManager.clientToServer());
        registerMessage(OpenComputerTerminalMessage.class, OpenComputerTerminalMessage::new, NetworkManager.clientToServer());

        registerMessage(NetworkConnectorConnectionsMessage.class, NetworkConnectorConnectionsMessage::new, NetworkManager.serverToClient());

        registerMessage(RobotTerminalOutputMessage.class, RobotTerminalOutputMessage::new, NetworkManager.serverToClient());
        registerMessage(RobotTerminalInputMessage.class, RobotTerminalInputMessage::new, NetworkManager.clientToServer());
        registerMessage(RobotRunStateMessage.class, RobotRunStateMessage::new, NetworkManager.serverToClient());
        registerMessage(RobotBusStateMessage.class, RobotBusStateMessage::new, NetworkManager.serverToClient());
        registerMessage(RobotBootErrorMessage.class, RobotBootErrorMessage::new, NetworkManager.serverToClient());
        registerMessage(RobotPowerMessage.class, RobotPowerMessage::new, NetworkManager.clientToServer());
        registerMessage(RobotInitializationRequestMessage.class, RobotInitializationRequestMessage::new, NetworkManager.clientToServer());
        registerMessage(RobotInitializationMessage.class, RobotInitializationMessage::new, NetworkManager.serverToClient());
        registerMessage(OpenRobotInventoryMessage.class, OpenRobotInventoryMessage::new, NetworkManager.clientToServer());
        registerMessage(OpenRobotTerminalMessage.class, OpenRobotTerminalMessage::new, NetworkManager.clientToServer());

        registerMessage(DiskDriveFloppyMessage.class, DiskDriveFloppyMessage::new, NetworkManager.serverToClient());

        registerMessage(BusInterfaceNameMessage.ToClient.class, BusInterfaceNameMessage.ToClient::new, NetworkManager.serverToClient());
        registerMessage(BusInterfaceNameMessage.ToServer.class, BusInterfaceNameMessage.ToServer::new, NetworkManager.clientToServer());

        registerMessage(ExportedFileMessage.class, ExportedFileMessage::new, NetworkManager.serverToClient());
        registerMessage(RequestImportedFileMessage.class, RequestImportedFileMessage::new, NetworkManager.serverToClient());
        registerMessage(ImportedFileMessage.class, ImportedFileMessage::new, NetworkManager.clientToServer());
        registerMessage(ServerCanceledImportFileMessage.class, ServerCanceledImportFileMessage::new, NetworkManager.serverToClient());
        registerMessage(ClientCanceledImportFileMessage.class, ClientCanceledImportFileMessage::new, NetworkManager.clientToServer());

        registerMessage(BusCableFacadeMessage.class, BusCableFacadeMessage::new, NetworkManager.serverToClient());

        registerMessage(NetworkInterfaceCardConfigurationMessage.class, NetworkInterfaceCardConfigurationMessage::new, NetworkManager.clientToServer());
        registerMessage(NetworkTunnelLinkMessage.class, NetworkTunnelLinkMessage::new, NetworkManager.clientToServer());

        registerMessage(ProjectorRequestFramebufferMessage.class, ProjectorRequestFramebufferMessage::new, NetworkManager.clientToServer());
        registerMessage(ProjectorFramebufferMessage.class, ProjectorFramebufferMessage::new, NetworkManager.serverToClient());
        registerMessage(ProjectorStateMessage.class, ProjectorStateMessage::new, NetworkManager.serverToClient());

        registerMessage(KeyboardInputMessage.class, KeyboardInputMessage::new, NetworkManager.clientToServer());

        registerMessage(MultipartMessage.class, MultipartMessage::new, NetworkManager.clientToServer());

        MultipartMessage.registerMessage(ImportedFileMessage.class, ImportedFileMessage::new);
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getMessageType(final Class<?> type) {
        final CustomPacketPayload.Type<? extends CustomPacketPayload> messageType = MESSAGE_TYPES.get(type);
        if (messageType == null) {
            throw new IllegalStateException("Message type [" + type + "] was not registered in Network.initialize().");
        }
        return (CustomPacketPayload.Type<T>) messageType;
    }

    ///////////////////////////////////////////////////////////////////

    public static void sendToServer(final AbstractMessage message) {
        NetworkManager.sendToServer(message);
    }

    public static void sendToClient(final AbstractMessage message, final ServerPlayer player) {
        NetworkManager.sendToPlayer(player, message);
    }

    public static void sendToClientsTrackingChunk(final AbstractMessage message, final LevelChunk chunk) {
        if (chunk.getLevel().getChunkSource() instanceof final ServerChunkCache chunkCache) {
            NetworkManager.sendToPlayers(chunkCache.chunkMap.getPlayers(chunk.getPos(), false), message);
        }
    }

    public static void sendToClientsTrackingBlockEntity(final AbstractMessage message, final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        if (level.getServer() == null) {
            return;
        }

        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException(
                "Attempting to send network message to BlockEntity from non-server " +
                    "thread [" + Thread.currentThread() + "]. This is not supported, " +
                    "because looking up the chunk from the level is required. " +
                    "Consider caching the containing chunk and using " +
                    "sendToClientsTrackingChunk() directly, instead.");
        }

        final BlockPos blockPos = blockEntity.getBlockPos();
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            sendToClientsTrackingChunk(message, level.getChunk(chunkX, chunkZ));
        }
    }

    public static void sendToClientsTrackingEntity(final AbstractMessage message, final Entity entity) {
        if (entity.level().getChunkSource() instanceof final ServerChunkCache chunkCache) {
            NetworkManager.sendToPlayers(chunkCache.chunkMap.getPlayers(entity.chunkPosition(), false), message);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static String messageId(final Class<?> type) {
        final String name = type.getEnclosingClass() != null
            ? type.getEnclosingClass().getSimpleName().replaceAll("Message$", "") + "_" + type.getSimpleName()
            : type.getSimpleName().replaceAll("Message$", "");
        return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toLowerCase(Locale.ROOT);
    }

    private static <T extends AbstractMessage> void registerMessage(final Class<T> type,
                                                                    final Function<RegistryFriendlyByteBuf, T> decoder,
                                                                    final NetworkManager.Side side) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, messageId(type));
        final CustomPacketPayload.Type<T> payloadType = new CustomPacketPayload.Type<>(id);
        final StreamCodec<RegistryFriendlyByteBuf, T> codec =
            CustomPacketPayload.codec(AbstractMessage::toBytes, decoder::apply);

        MESSAGE_TYPES.put(type, payloadType);

        if (side == NetworkManager.serverToClient() && Platform.getEnvironment() != Env.CLIENT) {
            NetworkManager.registerS2CPayloadType(payloadType, codec);
        } else {
            NetworkManager.registerReceiver(side, payloadType, codec,
                (message, context) -> context.queue(() -> message.handle(context)));
        }
    }
}
