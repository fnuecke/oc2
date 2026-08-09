/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.jei;

import com.google.common.base.Strings;
import li.cil.oc2.api.API;
import li.cil.oc2.common.item.AbstractBlockDeviceItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import static li.cil.oc2.common.Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM;
import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

@JeiPlugin
public class ExtraItemsJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(API.MOD_ID, "extra_items");
    }

    @Override
    public void registerItemSubtypes(final ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.COMPUTER.get(), new ComputerSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.ROBOT.get(), new RobotSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.HARD_DRIVE_CUSTOM.get(), new BlockDeviceSubtypeInterpreter());
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Items.FLASH_MEMORY_CUSTOM.get(), new BlockDeviceSubtypeInterpreter());
    }

    private static final class ComputerSubtypeInterpreter implements IIngredientSubtypeInterpreter<ItemStack> {
        @Override
        public String apply(final ItemStack ingredient, final UidContext context) {
            final CompoundTag itemsTag = NBTUtils.getChildTag(ingredient.getTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME);
            return itemsTag.isEmpty() ? NONE : stableTagToString(itemsTag);
        }
    }

    private static final class RobotSubtypeInterpreter implements IIngredientSubtypeInterpreter<ItemStack> {
        @Override
        public String apply(final ItemStack ingredient, final UidContext context) {
            final CompoundTag itemsTag = NBTUtils.getChildTag(ingredient.getTag(), API.MOD_ID, ITEMS_TAG_NAME);
            return itemsTag.isEmpty() ? NONE : stableTagToString(itemsTag);
        }
    }

    private static final class BlockDeviceSubtypeInterpreter implements IIngredientSubtypeInterpreter<ItemStack> {
        @Override
        public String apply(final ItemStack ingredient, final UidContext context) {
            final String registryName = ItemStackUtils.getModDataTag(ingredient).getString(AbstractBlockDeviceItem.DATA_TAG_NAME);
            return Strings.isNullOrEmpty(registryName) ? NONE : registryName;
        }
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
}
