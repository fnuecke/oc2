/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.audio;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

@Environment(EnvType.CLIENT)
public final class TerminalBell {
    public static void play() {
        final Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.getSoundManager().play(
            SimpleSoundInstance.forUI(NoteBlockInstrument.PLING.getSoundEvent(), 1)));
    }

    private TerminalBell() {
    }
}
