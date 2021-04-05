package li.cil.oc2.common.item;

import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.common.Constants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public final class FloppyItem extends AbstractStorageItem implements IDyeableArmorItem {
    public FloppyItem(final int capacity) {
        super(capacity);
    }
}
