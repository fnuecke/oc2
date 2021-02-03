package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import static li.cil.oc2.client.item.CustomItemColors.withColor;

public final class HardDriveItem extends AbstractBlockDeviceItem {
    private static final int DEFAULT_CAPACITY = 2 * Constants.MEGABYTE;

    ///////////////////////////////////////////////////////////////////

    public HardDriveItem() {
        super(DEFAULT_CAPACITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (isInGroup(group)) {
            items.add(withColor(withCapacity(2 * Constants.MEGABYTE), DyeColor.LIGHT_GRAY));
            items.add(withColor(withCapacity(4 * Constants.MEGABYTE), DyeColor.GREEN));
            items.add(withColor(withCapacity(8 * Constants.MEGABYTE), DyeColor.CYAN));
            items.add(withColor(withData(BlockDeviceDataRegistration.BUILDROOT.get()), DyeColor.BROWN));
        }
    }
}
