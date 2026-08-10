/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.annotation.Nullable;

public final class InstrumentationClient {
    public static void initialize() {
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> {
            dispatcher.register(ClientCommandRegistrationEvent.literal("screenshot")
                .executes(ctx -> screenshot(null))
                .then(ClientCommandRegistrationEvent.argument("name", StringArgumentType.word())
                    .executes(ctx -> screenshot(StringArgumentType.getString(ctx, "name")))));

            dispatcher.register(ClientCommandRegistrationEvent.literal("hidegui")
                .then(ClientCommandRegistrationEvent.argument("value", BoolArgumentType.bool())
                    .executes(ctx -> {
                        Minecraft.getInstance().options.hideGui = BoolArgumentType.getBool(ctx, "value");
                        return 1;
                    })));
        });
    }

    ///////////////////////////////////////////////////////////////////

    private static int screenshot(@Nullable final String name) {
        final Minecraft minecraft = Minecraft.getInstance();
        // Grabs the last rendered frame, so it has to run where that frame is still current.
        minecraft.execute(() -> Screenshot.grab(minecraft.gameDirectory, name != null ? name + ".png" : null,
            minecraft.getMainRenderTarget(), message -> {
            }));
        return 1;
    }

    private InstrumentationClient() {
    }
}
