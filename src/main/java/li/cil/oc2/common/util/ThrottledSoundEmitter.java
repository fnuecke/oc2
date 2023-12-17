/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public final class ThrottledSoundEmitter {
    private final Supplier<Optional<BlockLocation>> location;
    private final SoundEvent sound;
    private long minInterval;
    private SoundSource category;
    private float volume = 0.95f;
    private float volumeVariance = 0.05f;
    private float pitch = 0.9f;
    private float pitchVariance = 0.1f;

    private long lastEmittedTime;

    ///////////////////////////////////////////////////////////////////

    public ThrottledSoundEmitter(final Supplier<Optional<BlockLocation>> location, final SoundEvent sound) {
        this.location = location;
        this.sound = sound;
        this.category = SoundSource.BLOCKS;
        this.minInterval = 500;
    }

    ///////////////////////////////////////////////////////////////////

    public void play() {
        final long now = System.currentTimeMillis();
        if (now - lastEmittedTime > minInterval) {
            lastEmittedTime = now;
            this.location.get().ifPresent(location -> location.tryGetLevel().ifPresent(level -> {
                final float volume = sampleVolume(level.getRandom());
                final float pitch = samplePitch(level.getRandom());
                LevelUtils.playSound(level, location.blockPos(), sound, category, volume, pitch);
            }));
        }
    }

    public ThrottledSoundEmitter withMinInterval(final Duration minInterval) {
        this.minInterval = minInterval.toMillis();
        return this;
    }

    public ThrottledSoundEmitter withCategory(final SoundSource category) {
        this.category = category;
        return this;
    }

    public ThrottledSoundEmitter withVolume(final float volume) {
        this.volume = volume;
        return this;
    }

    public ThrottledSoundEmitter withVolumeVariance(final float volumeVariance) {
        this.volumeVariance = volumeVariance;
        return this;
    }

    public ThrottledSoundEmitter withPitch(final float pitch) {
        this.pitch = pitch;
        return this;
    }

    public ThrottledSoundEmitter withPitchVariance(final float pitchVariance) {
        this.pitchVariance = pitchVariance;
        return this;
    }

    ///////////////////////////////////////////////////////////////////

    private float sampleVolume(final RandomSource random) {
        return Mth.clamp(volume + volumeVariance * (random.nextFloat() - 0.5f), 0, 1);
    }

    private float samplePitch(final RandomSource random) {
        return Mth.clamp(pitch + pitchVariance * (random.nextFloat() - 0.5f), 0, 1);
    }
}
