package com.example.blockentity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "oc2_example_block_entity";

    public static final CounterBlock COUNTER = new CounterBlock(BlockBehaviour.Properties.of());

    public static final BlockEntityType<CounterBlockEntity> COUNTER_BLOCK_ENTITY =
        BlockEntityType.Builder.of(CounterBlockEntity::new, COUNTER).build(null);

    @Override
    public void onInitialize() {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, "counter");
        Registry.register(BuiltInRegistries.BLOCK, id, COUNTER);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(COUNTER, new Item.Properties()));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, COUNTER_BLOCK_ENTITY);

        if (FabricLoader.getInstance().isModLoaded("oc2")) {
            Integration.registerLookups();
        }
    }
}
