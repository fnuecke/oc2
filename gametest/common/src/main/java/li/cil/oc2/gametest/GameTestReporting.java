/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import net.minecraft.gametest.framework.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class GameTestReporting {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final long SHUTDOWN_GRACE_MILLIS = 60_000L;

    // --------------------------------------------------------------------- //

    public static void install(@Nullable final File report) {
        LOGGER.info("Game test reporting installed, report: {}", report);
        GlobalTestReporter.replaceWith(new WatchdogTestReporter(createReporters(report)));
    }

    // --------------------------------------------------------------------- //

    private static List<TestReporter> createReporters(@Nullable final File report) {
        final List<TestReporter> reporters = new ArrayList<>();
        reporters.add(new LogTestReporter());

        if (report == null) {
            return reporters;
        }

        final File parent = report.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("could not create game test report directory " + parent);
        }

        try {
            reporters.add(new JUnitLikeTestReporter(report));
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException("could not create the JUnit game test reporter", e);
        }

        return reporters;
    }

    private static void startShutdownWatchdog(final int exitCode) {
        final Thread watchdog = new Thread(() -> {
            if (!sleep(SHUTDOWN_GRACE_MILLIS)) {
                return;
            }

            LOGGER.error("The game test server did not shut down within {}s. This is the vanilla chunk "
                    + "unload livelock, not a test failure -- the results reported above stand. Halting.",
                SHUTDOWN_GRACE_MILLIS / 1000L);

            sleep(200);

            Runtime.getRuntime().halt(exitCode);
        }, "gametest-shutdown-watchdog");

        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static boolean sleep(final long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // --------------------------------------------------------------------- //

    private static final class WatchdogTestReporter implements TestReporter {
        private final List<TestReporter> reporters;
        private boolean anyFailed;

        WatchdogTestReporter(final List<TestReporter> reporters) {
            this.reporters = List.copyOf(reporters);
        }

        @Override
        public void onTestFailed(final GameTestInfo info) {
            anyFailed = true;
            reporters.forEach(reporter -> reporter.onTestFailed(info));
        }

        @Override
        public void onTestSuccess(final GameTestInfo info) {
            reporters.forEach(reporter -> reporter.onTestSuccess(info));
        }

        @Override
        public void finish() {
            reporters.forEach(TestReporter::finish);
            startShutdownWatchdog(anyFailed ? 1 : 0);
        }
    }

    // --------------------------------------------------------------------- //

    private GameTestReporting() {
    }
}
