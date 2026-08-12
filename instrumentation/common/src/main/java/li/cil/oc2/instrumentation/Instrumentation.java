/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.Terminal;
import net.fabricmc.api.EnvType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public final class Instrumentation {
    public static final String MOD_ID = "oc2instrumentation";

    // ------------------------------------------------------------- //

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
                    player.getServer().sendSystemMessage(Component.literal(
                            "[client:" + player.getGameProfile().getName() + "] "
                                    + payload.command() + " -> " + payload.result()));
                }));

        CommandRegistrationEvent.EVENT.register((dispatcher, context, selection) ->
                dispatcher.register(Commands.literal("oc2debug")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("facade")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("block", BlockStateArgument.block(context))
                                                .executes(ctx -> facade(ctx, BlockStateArgument.getBlock(ctx, "block")
                                                        .getState().getBlock().asItem())))
                                        .executes(ctx -> facade(ctx, null))))
                        .then(Commands.literal("client")
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            final String command = StringArgumentType.getString(ctx, "command");
                                            final Collection<ServerPlayer> players =
                                                    ctx.getSource().getServer().getPlayerList().getPlayers();
                                            for (final ServerPlayer player : players) {
                                                NetworkManager.sendToPlayer(player, new RunClientCommandPayload(command));
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Dispatched to " + players.size() + " client(s): " + command), false);
                                            return players.size();
                                        })))));

        CommandRegistrationEvent.EVENT.register((dispatcher, context, selection) ->
                dispatcher.register(Commands.literal("oc2computer")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.literal("start").executes(ctx -> computer(ctx, true)))
                                .then(Commands.literal("stop").executes(ctx -> computer(ctx, false)))
                                .then(Commands.literal("inventory")
                                        .executes(ctx -> openScreen(ctx, true)))
                                .then(Commands.literal("terminal")
                                        .executes(ctx -> openScreen(ctx, false)))
                                .then(Commands.literal("status").executes(Instrumentation::status))
                                .then(Commands.literal("screen").executes(Instrumentation::screen))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> type(ctx, StringArgumentType.getString(ctx, "text")))))
                                .then(Commands.literal("enter").executes(ctx -> type(ctx, "")))
                                .then(Commands.literal("gdb")
                                        .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                                                .executes(ctx -> gdb(ctx, IntegerArgumentType.getInteger(ctx, "port"))))))));

        EnvExecutor.runInEnv(EnvType.CLIENT, () -> InstrumentationClient::initialize);
    }

    // ------------------------------------------------------------- //

    private static int facade(final CommandContext<CommandSourceStack> ctx, @Nullable final Item item) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        final BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCable)) {
            ctx.getSource().sendFailure(Component.literal("No bus cable at " + pos.toShortString()));
            return 0;
        }

        busCable.setFacade(item == null ? ItemStack.EMPTY : new ItemStack(item));
        ctx.getSource().sendSuccess(() -> Component.literal(
                (item == null ? "Cleared" : "Set") + " facade at " + pos.toShortString()), false);
        return 1;
    }

    private static int status(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ComputerBlockEntity computer = requireComputer(ctx);
        if (computer == null) {
            return 0;
        }

        final var vm = computer.getVirtualMachine();
        final StringBuilder report = new StringBuilder("computer ")
                .append(BlockPosArgument.getLoadedBlockPos(ctx, "pos").toShortString())
                .append("\n  runState=").append(vm.getRunState())
                .append("\n  busState=").append(vm.getBusState())
                .append("\n  bootError=").append(vm.getBootError() == null ? "none" : vm.getBootError().getString())
                .append("\n  bus devices:");
        for (final var device : ((AbstractVirtualMachine) vm).busController.getDevices()) {
            report.append("\n    ").append(device.getClass().getSimpleName());
        }

        ctx.getSource().sendSuccess(() -> Component.literal(report.toString()), false);
        return 1;
    }

    private static int screen(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final ComputerBlockEntity computer = requireComputer(ctx);
        if (computer == null) {
            return 0;
        }

        final CompoundTag tag = NBTSerialization.serialize(computer.getTerminal());
        final byte[] buffer = tag.getByteArray("buffer");
        final StringBuilder text = new StringBuilder();
        for (int row = 0; row < Terminal.HEIGHT; row++) {
            for (int column = 0; column < Terminal.WIDTH; column++) {
                final int index = row * Terminal.WIDTH + column;
                final byte value = index < buffer.length ? buffer[index] : 0;
                text.append(value == 0 ? ' ' : (char) (value & 0xFF));
            }
            text.append('\n');
        }

        ctx.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int type(final CommandContext<CommandSourceStack> ctx, final String text) throws CommandSyntaxException {
        final ComputerBlockEntity computer = requireComputer(ctx);
        if (computer == null) {
            return 0;
        }

        final Terminal terminal = computer.getTerminal();
        for (final byte value : text.getBytes(StandardCharsets.UTF_8)) {
            terminal.putInput(value);
        }
        terminal.putInput((byte) '\r');

        ctx.getSource().sendSuccess(() -> Component.literal("typed: " + text), false);
        return 1;
    }

    private static int computer(final CommandContext<CommandSourceStack> ctx, final boolean start) throws CommandSyntaxException {
        final ComputerBlockEntity computer = requireComputer(ctx);
        if (computer == null) {
            return 0;
        }

        if (start) {
            computer.start();
        } else {
            computer.stop();
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                (start ? "Started" : "Stopped") + " computer at "
                        + computer.getBlockPos().toShortString()), false);
        return 1;
    }

    @Nullable
    private static ComputerBlockEntity requireComputer(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        final BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof final ComputerBlockEntity computer)) {
            ctx.getSource().sendFailure(Component.literal("No computer at " + pos.toShortString()));
            return null;
        }
        return computer;
    }

    private static int gdb(final CommandContext<CommandSourceStack> ctx, final int port) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        final BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof final ComputerBlockEntity computer)) {
            ctx.getSource().sendFailure(Component.literal("No computer at " + pos.toShortString()));
            return 0;
        }

        ((AbstractVirtualMachine) computer.getVirtualMachine()).state.board.enableGDB(port, false);
        ctx.getSource().sendSuccess(() -> Component.literal("GDB stub listening on port " + port), false);
        return 1;
    }

    private static int openScreen(final CommandContext<CommandSourceStack> ctx, final boolean inventory) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        final BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof final ComputerBlockEntity computer)) {
            ctx.getSource().sendFailure(Component.literal("No computer at " + pos.toShortString()));
            return 0;
        }

        int n = 0;
        for (final ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            if (inventory) {
                computer.openInventoryScreen(player);
            } else {
                computer.openTerminalScreen(player);
            }
            n++;
        }
        return n;
    }

    public record RunClientCommandPayload(String command) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RunClientCommandPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "run_client_command"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RunClientCommandPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RunClientCommandPayload::command, RunClientCommandPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientCommandResultPayload(String command, String result) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClientCommandResultPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "client_command_result"));

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
