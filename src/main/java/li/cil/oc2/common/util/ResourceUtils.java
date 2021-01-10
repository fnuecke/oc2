package li.cil.oc2.common.util;

import com.google.gson.JsonObject;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ResourceUtils {
    @Nullable
    public static <T> T getMetadata(final IResourceManager manager, final ResourceLocation resourceLocation, final IMetadataSectionSerializer<T> serializer) throws IOException {
        final ResourceLocation metadataLocation = new ResourceLocation(
                resourceLocation.getNamespace(), resourceLocation.getNamespace() + ".mcmeta");
        if (!manager.hasResource(metadataLocation)) {
            return null;
        }

        try (final IResource metadataResource = manager.getResource(metadataLocation)) {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(metadataResource.getInputStream(), StandardCharsets.UTF_8));
            final JsonObject metadataJson = JSONUtils.fromJson(bufferedReader);
            if (metadataJson == null) {
                return null;
            }

            final String sectionName = serializer.getSectionName();
            if (!metadataJson.has(sectionName)) {
                return null;
            }

            final JsonObject section = JSONUtils.getJsonObject(metadataJson, sectionName);
            return serializer.deserialize(section);
        }
    }
}
