package li.cil.oc2.common.vm;

import li.cil.sedna.api.device.rtc.RealTimeCounter;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class MinecraftRealTimeCounter implements RealTimeCounter {
    private static final int TICKS_PER_DAY = 24000;
    private static final int FREQUENCY = TICKS_PER_DAY;

    ///////////////////////////////////////////////////////////////////

    private World world;

    ///////////////////////////////////////////////////////////////////

    public void setWorld(@Nullable final World world) {
        this.world = world;
    }

    @Override
    public long getTime() {
        final long ticks = world != null ? world.getGameTime() : 0;
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
