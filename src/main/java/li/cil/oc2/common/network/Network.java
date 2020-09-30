package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
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
        INSTANCE.messageBuilder(ComputerTerminalOutputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ComputerTerminalOutputMessage::toBytes)
                .decoder(ComputerTerminalOutputMessage::new)
                .consumer(ComputerTerminalOutputMessage::handleOutput)
                .add();

        INSTANCE.messageBuilder(ComputerTerminalInputMessage.class, getNextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ComputerTerminalInputMessage::toBytes)
                .decoder(ComputerTerminalInputMessage::new)
                .consumer(ComputerTerminalInputMessage::handleInput)
                .add();
    }

    private static int getNextPacketId() {
        return nextPacketId++;
    }
}
