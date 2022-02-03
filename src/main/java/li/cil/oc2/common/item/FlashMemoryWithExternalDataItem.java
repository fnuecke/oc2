/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class FlashMemoryWithExternalDataItem extends ModItem {
    public static final String FIRMWARE_TAG_NAME = "firmware";

    ///////////////////////////////////////////////////////////////////

    private final ResourceLocation defaultData;
    @Nullable private String descriptionId;

    ///////////////////////////////////////////////////////////////////

    public FlashMemoryWithExternalDataItem(final ResourceLocation defaultData) {
        super(createProperties().stacksTo(1));
        this.defaultData = defaultData;
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public Firmware getFirmware(final ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(FIRMWARE_TAG_NAME);

        ResourceLocation location = defaultData;
        if (!StringUtil.isNullOrEmpty(registryName)) {
            try {
                location = new ResourceLocation(registryName);
            } catch (final ResourceLocationException ignored) {
            }
        }

        return Firmwares.REGISTRY.get().getValue(location);
    }

    public ItemStack withFirmware(final ItemStack stack, final Firmware firmware) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return stack;
        }

        final ResourceLocation key = Firmwares.REGISTRY.get().getKey(firmware);
        if (key == null) {
            return stack;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).putString(FIRMWARE_TAG_NAME, key.toString());

        return stack;
    }

    public ItemStack withFirmware(final Firmware firmware) {
        return withFirmware(new ItemStack(this), firmware);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final Firmware firmware = getFirmware(stack);
        if (firmware != null) {
            return new TextComponent("")
                .append(super.getName(stack))
                .append(" (")
                .append(firmware.getDisplayName())
                .append(")");
        } else {
            return super.getName(stack);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", Items.FLASH_MEMORY.getId());
        }
        return descriptionId;
    }
}
