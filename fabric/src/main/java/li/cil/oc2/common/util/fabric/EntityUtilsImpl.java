/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util.fabric;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Matches NeoForge's {@code Entity#getPersistentData}.
 */
public final class EntityUtilsImpl {
    private static final AttachmentType<CompoundTag> PERSISTENT_DATA = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "persistent_data"),
            builder -> builder.persistent(CompoundTag.CODEC).initializer(CompoundTag::new));

    public static CompoundTag getPersistentData(final Entity entity) {
        return entity.getAttachedOrCreate(PERSISTENT_DATA);
    }

    private EntityUtilsImpl() {
    }
}
