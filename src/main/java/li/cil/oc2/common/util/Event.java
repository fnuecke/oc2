package li.cil.oc2.common.util;

import java.util.HashSet;

public final class Event extends HashSet<Runnable> implements Runnable {
    @Override
    public void run() {
        for (final Runnable runnable : this) {
            runnable.run();
        }
    }
}
