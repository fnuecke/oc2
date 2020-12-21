package li.cil.oc2.common.item;

import li.cil.oc2.Constants;
import li.cil.oc2.common.init.Items;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemStack;

public final class ItemGroup {
    public static final net.minecraft.item.ItemGroup COMMON = FabricItemGroupBuilder
            .create(Constants.COMMON_ITEM_GROUP_NAME)
            .icon(() -> new ItemStack(Items.COMPUTER_ITEM))
            .build();
}
