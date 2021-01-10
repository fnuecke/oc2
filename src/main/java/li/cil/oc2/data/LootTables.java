package li.cil.oc2.data;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.LootTableProvider;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.loot.*;
import net.minecraft.loot.functions.CopyNbt;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static li.cil.oc2.common.Constants.BLOCK_ENTITY_INVENTORY_TAG_NAME;
import static li.cil.oc2.common.Constants.BLOCK_ENTITY_TAG_NAME_IN_ITEM;

public final class LootTables extends LootTableProvider {
    public LootTables(final DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void validate(final Map<ResourceLocation, LootTable> map, final ValidationTracker validationtracker) {
        map.forEach((location, table) -> LootTableManager.validateLootTable(validationtracker, location, table));
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables() {
        return Collections.singletonList(Pair.of(ModBlockLootTables::new, LootParameterSets.BLOCK));
    }

    public static final class ModBlockLootTables extends BlockLootTables {
        @Override
        protected void addTables() {
            registerDropSelfLootTable(Blocks.BUS_CABLE_BLOCK.get());
            registerDropSelfLootTable(Blocks.REDSTONE_INTERFACE_BLOCK.get());
            registerDropSelfLootTable(Blocks.NETWORK_CONNECTOR_BLOCK.get());
            registerDropSelfLootTable(Blocks.NETWORK_HUB_BLOCK.get());

            registerLootTable(Blocks.COMPUTER_BLOCK.get(), ModBlockLootTables::droppingWithInventory);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return StreamSupport.stream(super.getKnownBlocks().spliterator(), false)
                    .filter(block -> requireNonNull(block.getRegistryName()).getNamespace().equals(API.MOD_ID))
                    .collect(Collectors.toList());
        }

        private static LootTable.Builder droppingWithInventory(final Block block) {
            return LootTable.builder()
                    .addLootPool(withSurvivesExplosion(block, LootPool.builder()
                            .rolls(ConstantRange.of(1))
                            .addEntry(ItemLootEntry.builder(block)
                                    .acceptFunction(CopyNbt.builder(CopyNbt.Source.BLOCK_ENTITY)
                                            .addOperation(BLOCK_ENTITY_INVENTORY_TAG_NAME,
                                                    concat(BLOCK_ENTITY_TAG_NAME_IN_ITEM, BLOCK_ENTITY_INVENTORY_TAG_NAME),
                                                    CopyNbt.Action.REPLACE)
                                    )
                            )
                    ));
        }

        private static String concat(final String... paths) {
            return String.join(".", paths);
        }
    }
}
