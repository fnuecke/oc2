/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc2.common.vm.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

@Environment(EnvType.CLIENT)
public final class TerminalTextures {
    private static final Cache<Terminal, TerminalTexture> TEXTURES = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(TerminalTextures::handleNoLongerRendering)
        .build();

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            TEXTURES.cleanUp();
            TEXTURES.asMap().values().forEach(TerminalTexture::refresh);
        });
    }

    public static TerminalTexture get(final Terminal terminal) {
        try {
            return TEXTURES.get(terminal, () -> new TerminalTexture(terminal));
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------- //

    private static void handleNoLongerRendering(final RemovalNotification<Terminal, TerminalTexture> notification) {
        final TerminalTexture value = notification.getValue();
        if (value != null) {
            value.close();
        }
    }

    // --------------------------------------------------------------------- //

    private TerminalTextures() {
    }
}
