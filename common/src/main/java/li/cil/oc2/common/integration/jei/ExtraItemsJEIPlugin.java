/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.jei;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

@JeiPlugin
public class ExtraItemsJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "extra_items");
    }

    @Override
    public void registerItemSubtypes(final ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.COMPUTER.get(), new ComputerSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.ROBOT.get(), new RobotSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.HARD_DRIVE_LARGE.get(), new ImageSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.FLASH_MEMORY.get(), new ImageSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.FLOPPY.get(), new ImageSubtypeInterpreter());
    }

    @Nullable
    private static String itemsInfo(final CompoundTag rootTag) {
        final CompoundTag itemsTag = NBTUtils.getChildTag(rootTag, ITEMS_TAG_NAME);
        return itemsTag.isEmpty() ? null : stableTagToString(itemsTag);
    }

    private static String stableTagToString(@Nullable final Tag tag) {
        final StringBuilder stringBuilder = new StringBuilder();
        stableTagToString(tag, stringBuilder);
        return stringBuilder.toString();
    }

    private static void stableTagToString(@Nullable final Tag tag, final StringBuilder stringBuilder) {
        if (tag == null) {
            stringBuilder.append("null");
        }
        if (tag instanceof CompoundTag compoundTag) {
            stringBuilder.append("{");
            compoundTag.getAllKeys().stream().sorted().forEach(key -> {
                stringBuilder.append(key).append(":");
                stableTagToString(compoundTag.get(key), stringBuilder);
                stringBuilder.append(",");
            });
            stringBuilder.setLength(stringBuilder.length() - 1); // remove last comma
            stringBuilder.append("}");
        } else if (tag instanceof ListTag listTag) {
            stringBuilder.append("[");
            for (final Tag childTag : listTag) {
                stableTagToString(childTag, stringBuilder);
                stringBuilder.append(",");
            }
            stringBuilder.setLength(stringBuilder.length() - 1); // remove last comma
            stringBuilder.append("]");
        } else if (tag instanceof NumericTag numericTag) {
            stringBuilder.append(numericTag.getAsNumber());
        } else {
            stringBuilder.append(tag);
        }
    }

    private abstract static class SubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
        @Nullable
        @Override
        public final Object getSubtypeData(final ItemStack ingredient, final UidContext context) {
            return getInfo(ingredient);
        }

        @SuppressWarnings("deprecation")
        @Override
        public final String getLegacyStringSubtypeInfo(final ItemStack ingredient, final UidContext context) {
            final String info = getInfo(ingredient);
            return info == null ? "" : info;
        }

        @Nullable
        protected abstract String getInfo(final ItemStack stack);
    }

    private static final class ComputerSubtypeInterpreter extends SubtypeInterpreter {
        @Nullable
        @Override
        protected String getInfo(final ItemStack stack) {
            return itemsInfo(ItemStackUtils.getBlockEntityDataTag(stack));
        }
    }

    private static final class RobotSubtypeInterpreter extends SubtypeInterpreter {
        @Nullable
        @Override
        protected String getInfo(final ItemStack stack) {
            return itemsInfo(ItemStackUtils.getModDataTag(stack));
        }
    }

    private static final class ImageSubtypeInterpreter extends SubtypeInterpreter {
        @Nullable
        @Override
        protected String getInfo(final ItemStack stack) {
            final String registryName = ItemStackUtils.getModDataTag(stack).getString(StorageItemUtils.IMAGE_TAG_NAME);
            return registryName.isEmpty() ? null : registryName;
        }
    }
}
