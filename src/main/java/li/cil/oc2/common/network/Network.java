package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
    }

    public static <T> void sendToClientsTrackingChunk(final T message, final LevelChunk chunk) {
        Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
    }

    public static <T> void sendToClientsTrackingBlockEntity(final T message, final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level != null) {
            final LevelChunk chunk = level.getChunkAt(blockEntity.getBlockPos());
            Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
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
            .consumer(AbstractMessage::handleMessage)
            .add();
    }

    private static int getNextPacketId() {
        return nextPacketId++;
    }
}
