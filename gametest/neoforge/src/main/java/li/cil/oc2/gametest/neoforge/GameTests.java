/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import net.minecraft.gametest.framework.*;
import net.neoforged.fml.common.Mod;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;

@Mod(MOD_ID)
public final class GameTests {
    private static final String JUNIT_OUTPUT_DIR_PROPERTY = "oc2.gameTest.junitDir";
    private static final String REPORT_FILE_NAME = "neoforge-game-tests.xml";

    public GameTests() {
        final String directory = System.getProperty(JUNIT_OUTPUT_DIR_PROPERTY);
        if (directory == null || directory.isEmpty()) {
            return;
        }

        final File report = new File(directory, REPORT_FILE_NAME);
        final File parent = report.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("could not create game test report directory " + parent);
        }

        try {
            GlobalTestReporter.replaceWith(new TeeTestReporter(
                    new LogTestReporter(), new JUnitLikeTestReporter(report)));
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException("could not create the JUnit game test reporter", e);
        }
    }

    // ------------------------------------------------------------- //

    private record TeeTestReporter(TestReporter first, TestReporter second) implements TestReporter {
        @Override
        public void onTestFailed(final GameTestInfo info) {
            first.onTestFailed(info);
            second.onTestFailed(info);
        }

        @Override
        public void onTestSuccess(final GameTestInfo info) {
            first.onTestSuccess(info);
            second.onTestSuccess(info);
        }

        @Override
        public void finish() {
            first.finish();
            second.finish();
        }
    }
}
