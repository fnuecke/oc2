/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration;

import dev.architectury.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record ModIntegration(String modId) {
    private static final Logger LOGGER = LogManager.getLogger(ModIntegration.class);

    public static final ModIntegration COMPUTERCRAFT = new ModIntegration("computercraft");
    public static final ModIntegration TIS3D = new ModIntegration("tis3d");

    // --------------------------------------------------------------------- //

    public void run(final Runnable action) {
        if (!Platform.isModLoaded(modId)) {
            return;
        }

        try {
            action.run();
        } catch (final Throwable e) {
            LOGGER.error("Integration with [{}] failed to initialize, the installed version [{}] may not be compatible.",
                modId, Platform.getMod(modId).getVersion(), e);
        }
    }
}
