/* SPDX-License-Identifier: MIT */

package li.cil.oc2;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class MinecraftBootstrap implements BeforeAllCallback {
    @Override
    public void beforeAll(final ExtensionContext context) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
}
