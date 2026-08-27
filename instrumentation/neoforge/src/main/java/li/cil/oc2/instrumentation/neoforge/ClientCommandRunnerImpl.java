/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.neoforge;

import li.cil.oc2.instrumentation.ClientCommandResult;
import net.neoforged.neoforge.client.ClientCommandHandler;

public final class ClientCommandRunnerImpl {
    public static String run(final String command) {
        final String normalized = command.startsWith("/") ? command.substring(1) : command;
        ClientCommandResult.clear();
        try {
            return ClientCommandHandler.runCommand(normalized)
                ? ClientCommandResult.take("ok")
                : "no such client command";
        } catch (final Throwable e) {
            return describe(e);
        }
    }

    private static String describe(final Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private ClientCommandRunnerImpl() {
    }
}
