/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc2.instrumentation.ClientCommandResult;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.Objects;

public final class ClientCommandRunnerImpl {
    public static String run(final String command) {
        final String normalized = command.startsWith("/") ? command.substring(1) : command;

        final CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (dispatcher == null || connection == null) {
            return "no such client command";
        }

        final FabricClientCommandSource source = (FabricClientCommandSource) connection.getSuggestionsProvider();

        ClientCommandResult.clear();
        try {
            dispatcher.execute(normalized, source);
            return ClientCommandResult.take("ok");
        } catch (final CommandSyntaxException e) {
            return isUnknownCommand(e.getType()) ? "no such client command" : describe(e);
        } catch (final Throwable e) {
            return describe(e);
        }
    }

    // --------------------------------------------------------------------- //

    private static boolean isUnknownCommand(final CommandExceptionType type) {
        return Objects.equals(type, CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand())
            || Objects.equals(type, CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException());
    }

    private static String describe(final Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private ClientCommandRunnerImpl() {
    }
}
