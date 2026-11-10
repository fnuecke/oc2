package com.example.blockentity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "oc2_example_block_entity";

    private static final Identifier COUNTER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "counter");

    public static final CounterBlock COUNTER =
        new CounterBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, COUNTER_ID)));

    public static final BlockEntityType<CounterBlockEntity> COUNTER_BLOCK_ENTITY =
        FabricBlockEntityTypeBuilder.create(CounterBlockEntity::new, COUNTER).build();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.BLOCK, COUNTER_ID, COUNTER);
        Registry.register(BuiltInRegistries.ITEM, COUNTER_ID, new BlockItem(COUNTER,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, COUNTER_ID)).useBlockDescriptionPrefix()));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, COUNTER_ID, COUNTER_BLOCK_ENTITY);

        if (FabricLoader.getInstance().isModLoaded("oc2")) {
            Integration.registerLookups();
        }
    }
}
