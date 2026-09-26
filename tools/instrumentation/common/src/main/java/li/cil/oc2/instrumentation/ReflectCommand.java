/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import li.cil.oc2.instrumentation.reflect.ReflectContext;
import li.cil.oc2.instrumentation.reflect.ReflectOps;
import li.cil.oc2.instrumentation.reflect.Results;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ReflectCommand {
    public static final String[] OPS = {"list", "get", "set", "invoke", "items"};

    private static final int MAX_DEPTH = 4;

    private interface RootResolver {
        List<Labeled> resolve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private record Labeled(@Nullable String label, Object value) {
    }

    // --------------------------------------------------------------------- //

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, context, selection) ->
            dispatcher.register(Commands.literal("oc2reflect")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("block").then(
                    ops(Commands.argument("pos", BlockPosArgument.blockPos()), ReflectCommand::blocks)))
                .then(Commands.literal("entity").then(
                    ops(Commands.argument("target", EntityArgument.entities()), ReflectCommand::entities)))
                .then(clientOps())));
    }

    // --------------------------------------------------------------------- //

    private static LiteralArgumentBuilder<CommandSourceStack> clientOps() {
        LiteralArgumentBuilder<CommandSourceStack> client = Commands.literal("client");
        for (final String op : OPS) {
            client = client.then(Commands.literal(op)
                .executes(ctx -> forward(ctx, op, ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .executes(ctx -> forward(ctx, op, StringArgumentType.getString(ctx, "args")))));
        }
        return client.then(Commands.literal("raw")
            .then(Commands.argument("command", StringArgumentType.greedyString())
                .executes(ctx -> forward(ctx, null, StringArgumentType.getString(ctx, "command")))));
    }

    private static int forward(final CommandContext<CommandSourceStack> ctx, @Nullable final String op, final String args) {
        final Collection<ServerPlayer> players = ctx.getSource().getServer().getPlayerList().getPlayers();
        final String command = (op == null ? args : "oc2reflect " + op + " " + args).trim();
        for (final ServerPlayer player : players) {
            NetworkManager.sendToPlayer(player, new Instrumentation.RunClientCommandPayload(command));
        }
        send(ctx, Results.Outcome.success(new Results.Dispatched(players.size(),
            "answers arrive in the server log")));
        return players.size();
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T ops(T target, final RootResolver resolver) {
        for (final String op : OPS) {
            target = target.then(Commands.literal(op)
                .executes(ctx -> run(ctx, resolver, op, ""))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .executes(ctx -> run(ctx, resolver, op, StringArgumentType.getString(ctx, "args")))));
        }
        return target;
    }

    private static int run(final CommandContext<CommandSourceStack> ctx, final RootResolver resolver,
                           final String op, final String args) {
        final List<Labeled> roots;
        try {
            roots = resolver.resolve(ctx);
        } catch (final Throwable e) {
            send(ctx, Results.Outcome.error(e));
            return 0;
        }
        if (roots.isEmpty()) {
            send(ctx, Results.Outcome.error(new Results.Failure("nothing to reflect on", "NoTarget")));
            return 0;
        }

        final ReflectContext reflectContext = new ReflectContext(
            ctx.getSource().registryAccess(), selector -> entity(ctx, selector), MAX_DEPTH);

        if (roots.size() == 1 && roots.getFirst().label() == null) {
            final Results.Outcome outcome = tryRun(op, args, roots.getFirst(), reflectContext);
            send(ctx, outcome);
            return "success".equals(outcome.result()) ? 1 : 0;
        }

        int handled = 0;
        final List<Results.Target> targets = new ArrayList<>();
        for (final Labeled root : roots) {
            final Results.Outcome outcome = tryRun(op, args, root, reflectContext);
            if ("success".equals(outcome.result())) {
                handled++;
            }
            targets.add(new Results.Target(root.label(), Results.tree(outcome)));
        }
        send(ctx, Results.Outcome.success(new Results.Targets(targets)));
        return handled;
    }

    private static Results.Outcome tryRun(final String op, final String args,
                                          final Labeled root, final ReflectContext reflectContext) {
        try {
            return ReflectOps.run(op, args, root.value(), reflectContext);
        } catch (final Throwable e) {
            return Results.Outcome.error(e);
        }
    }

    private static void send(final CommandContext<CommandSourceStack> ctx, final Results.Outcome outcome) {
        final String body = Results.render(outcome);
        if ("success".equals(outcome.result())) {
            ctx.getSource().sendSuccess(() -> Component.literal(body), false);
        } else {
            ctx.getSource().sendFailure(Component.literal(body));
        }
    }

    private static List<Labeled> blocks(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        final BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);
        if (blockEntity == null) {
            throw new IllegalArgumentException("no block entity at " + pos.toShortString()
                + "; only blocks with one can be reflected on");
        }
        return List.of(new Labeled(null, blockEntity));
    }

    private static List<Labeled> entities(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final List<Labeled> result = new ArrayList<>();
        for (final Entity entity : EntityArgument.getEntities(ctx, "target")) {
            result.add(new Labeled(entity.getName().getString() + "#" + entity.getId(), entity));
        }
        return result;
    }

    @Nullable
    private static Entity entity(final CommandContext<CommandSourceStack> ctx, final String selector) {
        try {
            return new EntitySelectorParser(new StringReader(selector), true)
                .parse().findSingleEntity(ctx.getSource());
        } catch (final CommandSyntaxException e) {
            return null;
        }
    }

    private ReflectCommand() {
    }
}
