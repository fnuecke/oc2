package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.common.item.ItemGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

public final class Items {
    public static final Item COMPUTER_ITEM = new BlockItem(Blocks.COMPUTER_BLOCK, commonProperties());
    public static final Item BUS_CABLE_ITEM = new BlockItem(Blocks.BUS_CABLE_BLOCK, commonProperties());
    public static final Item REDSTONE_INTERFACE_ITEM = new BlockItem(Blocks.REDSTONE_INTERFACE_BLOCK, commonProperties());
    public static final Item SCREEN_ITEM = new BlockItem(Blocks.SCREEN_BLOCK, commonProperties());

    ///////////////////////////////////////////////////////////////////

    public static final Item HDD_ITEM = new Item(commonProperties());
    public static final Item RAM_8M_ITEM = new Item(commonProperties());

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        Registry.register(Registry.ITEM, Constants.COMPUTER_BLOCK_NAME, COMPUTER_ITEM);
        Registry.register(Registry.ITEM, Constants.BUS_CABLE_BLOCK_NAME, BUS_CABLE_ITEM);
        Registry.register(Registry.ITEM, Constants.REDSTONE_INTERFACE_BLOCK_NAME, REDSTONE_INTERFACE_ITEM);
        Registry.register(Registry.ITEM, Constants.SCREEN_BLOCK_NAME, SCREEN_ITEM);
        Registry.register(Registry.ITEM, Constants.HDD_ITEM_NAME, HDD_ITEM);
        Registry.register(Registry.ITEM, Constants.RAM_ITEM_NAME, RAM_8M_ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    private static Item.Settings commonProperties() {
        return new Item.Settings().group(ItemGroup.COMMON);
    }
}
