/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import javax.annotation.Nullable;
import java.util.Objects;

public final class HardDriveWithExternalDataItem extends AbstractBlockDeviceItem implements ColoredItem, CreativeTabItemProvider {
    private final int defaultColor;
    @Nullable
    private String descriptionId;

    // ------------------------------------------------------------- //

    public HardDriveWithExternalDataItem(final ResourceLocation defaultData, final DyeColor defaultColor) {
        super(defaultData);
        this.defaultColor = defaultColor.getTextureDiffuseColor();
    }

    // ------------------------------------------------------------- //

    @Override
    public void addCreativeTabItems(final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output) {
        output.accept(new ItemStack(this));

        BlockDeviceDataRegistry.values().forEach(data -> {
            if (!Objects.equals(BlockDeviceDataRegistry.getKey(data), getDefaultData())) {
                final ItemStack stack = withData(data);
                if (!stack.isEmpty()) {
                    output.accept(stack);
                }
            }
        });
    }

    @Override
    public int getColor(final ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, defaultColor);
    }

    // ------------------------------------------------------------- //

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "hard_drive"));
        }
        return descriptionId;
    }
}
