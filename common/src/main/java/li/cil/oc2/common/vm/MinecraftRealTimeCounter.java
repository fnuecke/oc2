/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.sedna.api.device.rtc.RealTimeCounter;

import java.util.function.LongSupplier;

public final class MinecraftRealTimeCounter implements RealTimeCounter {
    private static final int TICKS_PER_DAY = 24000;
    private static final int FREQUENCY = TICKS_PER_DAY;

    // --------------------------------------------------------------------- //

    private LongSupplier gameTime = () -> 0;

    // --------------------------------------------------------------------- //

    public void setGameTimeSource(final LongSupplier gameTime) {
        this.gameTime = gameTime;
    }

    @Override
    public long getTime() {
        final long ticks = gameTime.getAsLong();
        final long days = ticks; // / TICKS_PER_DAY
        final long hours = days * 24;
        final long minutes = hours * 60;
        final long seconds = minutes * 60;
        return seconds; // * FREQUENCY
    }

    @Override
    public int getFrequency() {
        return FREQUENCY;
    }
}
