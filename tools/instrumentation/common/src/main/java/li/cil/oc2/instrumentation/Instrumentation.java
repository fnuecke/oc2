/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class Instrumentation {
    public static final String MOD_ID = "oc2instrumentation";

    // --------------------------------------------------------------------- //

    public static void initialize() {
        if (Platform.getEnvironment() != Env.CLIENT) {
            NetworkManager.registerS2CPayloadType(RunClientCommandPayload.TYPE, RunClientCommandPayload.STREAM_CODEC);
        } else {
            NetworkManager.registerReceiver(NetworkManager.serverToClient(),
                RunClientCommandPayload.TYPE, RunClientCommandPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    final String result = ClientCommandRunner.run(payload.command());
                    NetworkManager.sendToServer(new ClientCommandResultPayload(payload.command(), result));
                }));
        }

        NetworkManager.registerReceiver(NetworkManager.clientToServer(),
            ClientCommandResultPayload.TYPE, ClientCommandResultPayload.STREAM_CODEC,
            (payload, context) -> context.queue(() -> {
                final ServerPlayer player = (ServerPlayer) context.getPlayer();
                player.level().getServer().sendSystemMessage(Component.literal(
                    "[client:" + player.getGameProfile().name() + "] "
                        + payload.command() + " -> " + payload.result()));
            }));

        ReflectCommand.register();

        LifecycleEvent.SERVER_STOPPED.register(Instrumentation::exitAfterServerThread);

        EnvExecutor.runInEnv(EnvType.CLIENT, () -> InstrumentationClient::initialize);
    }

    // --------------------------------------------------------------------- //

    private static void exitAfterServerThread(final MinecraftServer server) {
        if (!server.isDedicatedServer()) {
            return;
        }

        final Thread serverThread = server.getRunningThread();
        final Thread thread = new Thread(() -> {
            try {
                serverThread.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.exit(0);
        }, "Dev Server Exit");
        thread.setDaemon(true);
        thread.start();
    }

    // --------------------------------------------------------------------- //

    public record RunClientCommandPayload(String command) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RunClientCommandPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "run_client_command"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RunClientCommandPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RunClientCommandPayload::command, RunClientCommandPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientCommandResultPayload(String command, String result) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientCommandResultPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "client_command_result"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientCommandResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ClientCommandResultPayload::command,
                ByteBufCodecs.STRING_UTF8, ClientCommandResultPayload::result,
                ClientCommandResultPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Instrumentation() {
    }
}
