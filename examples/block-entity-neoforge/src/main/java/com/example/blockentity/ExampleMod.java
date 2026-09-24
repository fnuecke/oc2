package com.example.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = "oc2_example_block_entity";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<CounterBlock> COUNTER =
        BLOCKS.register("counter", () -> new CounterBlock(BlockBehaviour.Properties.of()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CounterBlockEntity>> COUNTER_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("counter", () ->
            BlockEntityType.Builder.of(CounterBlockEntity::new, COUNTER.get()).build(null));

    public ExampleMod(final IEventBus modEventBus) {
        ITEMS.registerSimpleBlockItem(COUNTER);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        if (ModList.get().isLoaded("oc2")) {
            modEventBus.addListener(Integration::registerCapabilities);
        }
    }
}
