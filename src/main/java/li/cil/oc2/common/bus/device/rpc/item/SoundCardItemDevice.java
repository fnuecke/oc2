/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class SoundCardItemDevice extends AbstractItemRPCDevice {
    private static final int COOLDOWN_IN_TICKS = TickUtils.toTicks(Duration.ofSeconds(2));
    private static final int MAX_FIND_RESULTS = 25;

    ///////////////////////////////////////////////////////////////////

    private final Supplier<Optional<BlockLocation>> location;
    private long gameTimeCooldownExpiresAt;

    ///////////////////////////////////////////////////////////////////

    public SoundCardItemDevice(final ItemStack identity, final Supplier<Optional<BlockLocation>> location) {
        super(identity, "sound");
        this.location = location;
    }

    ///////////////////////////////////////////////////////////////////

    @Callback
    public void playSound(@Nullable @Parameter("name") final String name) {
        playSound(name, 1, 1);
    }

    @Callback
    public void playSound(@Nullable @Parameter("name") final String name, @Parameter("volume") final float volume) {
        playSound(name, volume, 1);
    }
    
    @Callback
    public void playSound(@Nullable @Parameter("name") final String name, @Parameter("volume") final float volume, @Parameter("pitch") final float pitch) {
        if (name == null) throw new IllegalArgumentException();

        location.get().ifPresent(location -> location.tryGetLevel().ifPresent(level -> {
            if (!(level instanceof final ServerLevel serverLevel)) {
                return;
            }

            final long gameTime = serverLevel.getGameTime();
            if (gameTime < gameTimeCooldownExpiresAt) {
                return;
            }

            gameTimeCooldownExpiresAt = gameTime + COOLDOWN_IN_TICKS;

            final SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(name));
            if (soundEvent == null) throw new IllegalArgumentException("Sound not found.");
            level.playSound(null, location.blockPos(), soundEvent, SoundSource.BLOCKS, volume, pitch);
        }));
    }

    @Callback
    public List<String> findSound(@Nullable @Parameter("name") final String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException();

        final ArrayList<String> matches = new ArrayList<>();

        for (final ResourceLocation key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
            final String keyName = key.toString();
            if (keyName.contains(name)) {
                matches.add(keyName);
                if (matches.size() >= MAX_FIND_RESULTS) {
                    break;
                }
            }
        }

        return matches;
    }
}
