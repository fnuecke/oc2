/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;

@Environment(EnvType.CLIENT)
public final class TerminalTextures {
    private static final Cache<Terminal, TerminalTexture> TEXTURES = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(TerminalTextures::handleNoLongerRendering)
        .build();
    private static final ArrayDeque<Identifier> FREE_LOCATIONS = new ArrayDeque<>();
    private static int locationCount;

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            TEXTURES.cleanUp();
            TEXTURES.asMap().values().forEach(TerminalTexture::refresh);
        });
    }

    public static TerminalTexture get(final Terminal terminal) {
        try {
            return TEXTURES.get(terminal, () -> new TerminalTexture(terminal, allocateLocation()));
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------- //

    private static Identifier allocateLocation() {
        final Identifier location = FREE_LOCATIONS.poll();
        if (location != null) {
            return location;
        }
        return Identifier.fromNamespaceAndPath(API.MOD_ID, "terminal/" + locationCount++);
    }

    private static void handleNoLongerRendering(final RemovalNotification<Terminal, TerminalTexture> notification) {
        final TerminalTexture value = notification.getValue();
        if (value != null) {
            value.close();
            FREE_LOCATIONS.add(value.getLocation());
        }
    }

    // --------------------------------------------------------------------- //

    private TerminalTextures() {
    }
}
