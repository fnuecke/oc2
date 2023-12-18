/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public final class Network {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(API.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    ///////////////////////////////////////////////////////////////////

    private static int nextPacketId = 1;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        registerMessage(ComputerTerminalOutputMessage.class, ComputerTerminalOutputMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ComputerTerminalInputMessage.class, ComputerTerminalInputMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(ComputerRunStateMessage.class, ComputerRunStateMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ComputerBusStateMessage.class, ComputerBusStateMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ComputerBootErrorMessage.class, ComputerBootErrorMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ComputerPowerMessage.class, ComputerPowerMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(OpenComputerInventoryMessage.class, OpenComputerInventoryMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(OpenComputerTerminalMessage.class, OpenComputerTerminalMessage::new, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(NetworkConnectorConnectionsMessage.class, NetworkConnectorConnectionsMessage::new, NetworkDirection.PLAY_TO_CLIENT);

        registerMessage(RobotTerminalOutputMessage.class, RobotTerminalOutputMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(RobotTerminalInputMessage.class, RobotTerminalInputMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(RobotRunStateMessage.class, RobotRunStateMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(RobotBusStateMessage.class, RobotBusStateMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(RobotBootErrorMessage.class, RobotBootErrorMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(RobotPowerMessage.class, RobotPowerMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(RobotInitializationRequestMessage.class, RobotInitializationRequestMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(RobotInitializationMessage.class, RobotInitializationMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(OpenRobotInventoryMessage.class, OpenRobotInventoryMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(OpenRobotTerminalMessage.class, OpenRobotTerminalMessage::new, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(DiskDriveFloppyMessage.class, DiskDriveFloppyMessage::new, NetworkDirection.PLAY_TO_CLIENT);

        registerMessage(BusInterfaceNameMessage.ToClient.class, BusInterfaceNameMessage.ToClient::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(BusInterfaceNameMessage.ToServer.class, BusInterfaceNameMessage.ToServer::new, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(ExportedFileMessage.class, ExportedFileMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(RequestImportedFileMessage.class, RequestImportedFileMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ImportedFileMessage.class, ImportedFileMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(ServerCanceledImportFileMessage.class, ServerCanceledImportFileMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ClientCanceledImportFileMessage.class, ClientCanceledImportFileMessage::new, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(BusCableFacadeMessage.class, BusCableFacadeMessage::new, NetworkDirection.PLAY_TO_CLIENT);

        registerMessage(NetworkInterfaceCardConfigurationMessage.class, NetworkInterfaceCardConfigurationMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(NetworkTunnelLinkMessage.class, NetworkTunnelLinkMessage::new, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(ProjectorRequestFramebufferMessage.class, ProjectorRequestFramebufferMessage::new, NetworkDirection.PLAY_TO_SERVER);
        registerMessage(ProjectorFramebufferMessage.class, ProjectorFramebufferMessage::new, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(ProjectorStateMessage.class, ProjectorStateMessage::new, NetworkDirection.PLAY_TO_CLIENT);

        registerMessage(KeyboardInputMessage.class, KeyboardInputMessage::new, NetworkDirection.PLAY_TO_SERVER);

        registerMessage(MultipartMessage.class, MultipartMessage::new, NetworkDirection.PLAY_TO_SERVER);

        MultipartMessage.registerMessage(ImportedFileMessage.class, ImportedFileMessage::new);
    }

    public static <T> void sendToServer(final T message) {
        Network.INSTANCE.sendToServer(message);
    }

    public static <T> void sendToClient(final T message, final ServerPlayer player) {
        Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <T> void sendToClientsTrackingChunk(final T message, final LevelChunk chunk) {
        Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
    }

    public static <T> void sendToClientsTrackingBlockEntity(final T message, final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        final MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        if (!server.isSameThread()) {
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
            final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            sendToClientsTrackingChunk(message, chunk);
        }
    }

    public static <T> void sendToClientsTrackingEntity(final T message, final Entity entity) {
        Network.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    ///////////////////////////////////////////////////////////////////

    private static <T extends AbstractMessage> void registerMessage(final Class<T> type, final Function<FriendlyByteBuf, T> decoder, final NetworkDirection direction) {
        INSTANCE.messageBuilder(type, getNextPacketId(), direction)
            .encoder(AbstractMessage::toBytes)
            .decoder(decoder)
            .consumerMainThread(AbstractMessage::handleMessage)
            .add();
    }

    private static int getNextPacketId() {
        return nextPacketId++;
    }
}
