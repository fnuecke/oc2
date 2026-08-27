/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import li.cil.oc2.instrumentation.reflect.ReflectContext;
import li.cil.oc2.instrumentation.reflect.ReflectOps;
import li.cil.oc2.instrumentation.reflect.Results;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class InstrumentationClient {
    private static final int MAX_DEPTH = 4;
    private static final int SCREENSHOT_TIMEOUT_SECONDS = 10;

    public static void initialize() {
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> {
            var reflect = ClientCommandRegistrationEvent.literal("oc2reflect");
            for (final String op : ReflectCommand.OPS) {
                reflect = reflect.then(ClientCommandRegistrationEvent.literal(op)
                    .executes(ctx -> run(op, ""))
                    .then(ClientCommandRegistrationEvent.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> run(op, StringArgumentType.getString(ctx, "args")))));
            }
            dispatcher.register(reflect);

            dispatcher.register(ClientCommandRegistrationEvent.literal("screenshot")
                .executes(ctx -> screenshot(null))
                .then(ClientCommandRegistrationEvent.argument("name", StringArgumentType.word())
                    .executes(ctx -> screenshot(StringArgumentType.getString(ctx, "name")))));
        });
    }

    // --------------------------------------------------------------------- //

    private static int run(final String op, final String args) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final ReflectContext ctx = new ReflectContext(
            level == null ? null : level.registryAccess(), null, MAX_DEPTH);

        final Results.Outcome outcome;
        try {
            outcome = ReflectOps.run(op, args, minecraft, ctx);
        } catch (final Throwable e) {
            ClientCommandResult.set(Results.render(Results.Outcome.error(e)));
            return 0;
        }
        ClientCommandResult.set(Results.render(outcome));
        return "success".equals(outcome.result()) ? 1 : 0;
    }

    private static int screenshot(@Nullable final String name) {
        final Minecraft minecraft = Minecraft.getInstance();
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<String> outcome = new AtomicReference<>();

        Screenshot.grab(minecraft.gameDirectory, name != null ? name + ".png" : null,
            minecraft.getMainRenderTarget(), message -> {
                outcome.set(message.getString());
                done.countDown();
            });

        try {
            if (!done.await(SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                ClientCommandResult.set(Results.render(Results.Outcome.error(new Results.Failure(
                    "no answer within " + SCREENSHOT_TIMEOUT_SECONDS + "s", "Timeout"))));
                return 0;
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            ClientCommandResult.set(Results.render(Results.Outcome.error(new Results.Failure(
                "interrupted while waiting", "InterruptedException"))));
            return 0;
        }

        ClientCommandResult.set(Results.render(Results.Outcome.success(
            new Results.Screenshot(name, outcome.get()))));
        return 1;
    }

    private InstrumentationClient() {
    }
}
