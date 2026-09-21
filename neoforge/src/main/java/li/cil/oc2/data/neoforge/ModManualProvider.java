/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import com.google.common.hash.Hashing;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.common.doc.DeviceDocumentation;
import li.cil.oc2.common.doc.DevicePage;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.fml.ModList;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public final class ModManualProvider implements DataProvider {
    private static final Set<Type> CALLBACK_ANNOTATIONS = Set.of(Type.getType(Callback.class), Type.getType(IOCallback.class));
    private static final String DEVICE_PACKAGE = "li.cil.oc2.common.";

    private final Path outputFolder;

    public ModManualProvider(final PackOutput output) {
        outputFolder = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
            .resolve(API.MOD_ID).resolve("doc").resolve("en_us").resolve("device");
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final TreeMap<String, List<String>> pages = new TreeMap<>();
        for (final Class<?> type : findDevices()) {
            final DeviceDocumentation device = DeviceDocumentation.of(type);
            final String pageName = DevicePage.pageName(type);
            final List<String> page = DevicePage.page(type, device);
            final List<String> existing = pages.putIfAbsent(pageName, page);
            if (existing != null && !existing.equals(page)) {
                throw new IllegalStateException("Devices sharing the type name [" + pageName + "] document different APIs: " +
                    firstDifference(existing, page));
            }
        }
        pages.put("index", DevicePage.index(pages.keySet()));

        return CompletableFuture.allOf(pages.entrySet().stream()
            .map(page -> save(cache, outputFolder.resolve(page.getKey() + ".md"), page.getValue()))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Manual";
    }

    // --------------------------------------------------------------------- //

    private static List<Class<?>> findDevices() {
        return ModList.get().getAllScanData().stream()
            .flatMap(scanData -> scanData.getAnnotations().stream())
            .filter(annotation -> CALLBACK_ANNOTATIONS.contains(annotation.annotationType()))
            .map(annotation -> annotation.clazz().getClassName())
            .filter(className -> className.startsWith(DEVICE_PACKAGE))
            .distinct()
            .sorted()
            .<Class<?>>map(ModManualProvider::load)
            .toList();
    }

    private static String firstDifference(final List<String> a, final List<String> b) {
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
            final String lineA = i < a.size() ? a.get(i) : "<end>";
            final String lineB = i < b.size() ? b.get(i) : "<end>";
            if (!lineA.equals(lineB)) {
                return "line " + (i + 1) + ": [" + lineA + "] vs [" + lineB + "]";
            }
        }
        return "";
    }

    private static Class<?> load(final String className) {
        try {
            return Class.forName(className, false, ModManualProvider.class.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CompletableFuture<?> save(final CachedOutput cache, final Path path, final List<String> lines) {
        return CompletableFuture.runAsync(() -> {
            final byte[] bytes = (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8);
            try {
                cache.writeIfNeeded(path, bytes, Hashing.sha256().hashBytes(bytes));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Util.backgroundExecutor());
    }
}
