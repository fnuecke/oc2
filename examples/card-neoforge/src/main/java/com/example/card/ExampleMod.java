package com.example.card;

import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.util.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "oc2_example_card";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<ItemDeviceProvider> ITEM_DEVICE_PROVIDERS =
        DeferredRegister.create(Registries.ITEM_DEVICE_PROVIDER, MOD_ID);

    public static final DeferredItem<Item> DICE_CARD = ITEMS.registerSimpleItem("dice_card");

    public ExampleMod(final IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ITEM_DEVICE_PROVIDERS.register(modEventBus);

        ITEM_DEVICE_PROVIDERS.register("dice_card", DiceCardDeviceProvider::new);
    }
}
