package li.cil.oc2.common.util;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ResourceUtils {
    @Nullable
    public static <T> T getMetadata(final ResourceManager manager, final ResourceLocation resourceLocation, final MetadataSectionSerializer<T> serializer) throws IOException {
        final ResourceLocation metadataLocation = new ResourceLocation(
                resourceLocation.getNamespace(), resourceLocation.getPath() + ".mcmeta");
        if (!manager.hasResource(metadataLocation)) {
            return null;
        }

        try (final Resource metadataResource = manager.getResource(metadataLocation)) {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(metadataResource.getInputStream(), StandardCharsets.UTF_8));
            final JsonObject metadataJson = GsonHelper.parse(bufferedReader);
            if (metadataJson == null) {
                return null;
            }

            final String sectionName = serializer.getMetadataSectionName();
            if (!metadataJson.has(sectionName)) {
                return null;
            }

            final JsonObject section = GsonHelper.convertToJsonObject(metadataJson, sectionName);
            return serializer.fromJson(section);
        }
    }
}
