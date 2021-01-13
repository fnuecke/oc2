package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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

    public static void setup() {
        INSTANCE.messageBuilder(ComputerTerminalOutputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerTerminalOutputMessage::toBytes)
                .decoder(ComputerTerminalOutputMessage::new)
                .consumer(ComputerTerminalOutputMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(ComputerTerminalInputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ComputerTerminalInputMessage::toBytes)
                .decoder(ComputerTerminalInputMessage::new)
                .consumer(ComputerTerminalInputMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(ComputerRunStateMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerRunStateMessage::toBytes)
                .decoder(ComputerRunStateMessage::new)
                .consumer(ComputerRunStateMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(ComputerBusStateMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerBusStateMessage::toBytes)
                .decoder(ComputerBusStateMessage::new)
                .consumer(ComputerBusStateMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(ComputerBootErrorMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerBootErrorMessage::toBytes)
                .decoder(ComputerBootErrorMessage::new)
                .consumer(ComputerBootErrorMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(ComputerPowerMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ComputerPowerMessage::toBytes)
                .decoder(ComputerPowerMessage::new)
                .consumer(ComputerPowerMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(NetworkConnectorConnectionsMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NetworkConnectorConnectionsMessage::toBytes)
                .decoder(NetworkConnectorConnectionsMessage::new)
                .consumer(NetworkConnectorConnectionsMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotTerminalOutputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RobotTerminalOutputMessage::toBytes)
                .decoder(RobotTerminalOutputMessage::new)
                .consumer(RobotTerminalOutputMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotTerminalInputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RobotTerminalInputMessage::toBytes)
                .decoder(RobotTerminalInputMessage::new)
                .consumer(RobotTerminalInputMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotRunStateMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RobotRunStateMessage::toBytes)
                .decoder(RobotRunStateMessage::new)
                .consumer(RobotRunStateMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotBusStateMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RobotBusStateMessage::toBytes)
                .decoder(RobotBusStateMessage::new)
                .consumer(RobotBusStateMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotBootErrorMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RobotBootErrorMessage::toBytes)
                .decoder(RobotBootErrorMessage::new)
                .consumer(RobotBootErrorMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotPowerMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RobotPowerMessage::toBytes)
                .decoder(RobotPowerMessage::new)
                .consumer(RobotPowerMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotInitializationRequestMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RobotInitializationRequestMessage::toBytes)
                .decoder(RobotInitializationRequestMessage::new)
                .consumer(RobotInitializationRequestMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(RobotInitializationMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RobotInitializationMessage::toBytes)
                .decoder(RobotInitializationMessage::new)
                .consumer(RobotInitializationMessage::handleMessage)
                .add();
    }

    public static <T> void sendToClientsTrackingChunk(final T message, final Chunk chunk) {
        Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
    }

    public static <T> void sendToClientsTrackingEntity(final T message, final Entity entity) {
        Network.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    ///////////////////////////////////////////////////////////////////

    private static int getNextPacketId() {
        return nextPacketId++;
    }
}
