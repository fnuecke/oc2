package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.TerminalBlockInputMessage;
import li.cil.oc2.common.network.message.TerminalBlockOutputMessage;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class Network {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextPacketId = 1;

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(API.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void setup() {
        INSTANCE.messageBuilder(TerminalBlockOutputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TerminalBlockOutputMessage::toBytes)
                .decoder(TerminalBlockOutputMessage::new)
                .consumer(TerminalBlockOutputMessage::handleMessage)
                .add();

        INSTANCE.messageBuilder(TerminalBlockInputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(TerminalBlockInputMessage::toBytes)
                .decoder(TerminalBlockInputMessage::new)
                .consumer(TerminalBlockInputMessage::handleMessage)
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
    }

    public static <T> void sendToClientsTrackingChunk(final T message, final Chunk chunk) {
        Network.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
    }

    private static int getNextPacketId() {
        return nextPacketId++;
    }
}
