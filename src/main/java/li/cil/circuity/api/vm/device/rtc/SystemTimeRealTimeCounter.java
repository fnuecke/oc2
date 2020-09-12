package li.cil.circuity.api.vm.device.rtc;

public final class SystemTimeRealTimeCounter implements RealTimeCounter {
    private static final SystemTimeRealTimeCounter INSTANCE = new SystemTimeRealTimeCounter();

    private static final int NANOSECONDS_PER_SECOND = 1_000_000_000;
    private static final int FREQUENCY = 1000; // 10_000_000

    public static RealTimeCounter create() {
        return INSTANCE;
    }

    @Override
    public long getTime() {
        return System.currentTimeMillis();
//        final long milliseconds = System.currentTimeMillis();
//        final long seconds = milliseconds / 1000;
//        final long nanoseconds = System.nanoTime() % NANOSECONDS_PER_SECOND;
//
//        return seconds * FREQUENCY + (nanoseconds / (NANOSECONDS_PER_SECOND / FREQUENCY));
    }

    @Override
    public int getFrequency() {
        return FREQUENCY;
    }
}
