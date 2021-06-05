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
import static li.cil.oc2.common.Constants.*;

public final class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(final DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void validate(final Map<ResourceLocation, LootTable> map, final ValidationTracker validationtracker) {
        map.forEach((location, table) -> LootTableManager.validate(validationtracker, location, table));
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables() {
        return Collections.singletonList(Pair.of(ModBlockLootTables::new, LootParameterSets.BLOCK));
    }

    public static final class ModBlockLootTables extends BlockLootTables {
        @Override
        protected void addTables() {
            dropSelf(Blocks.REDSTONE_INTERFACE.get());
            dropSelf(Blocks.NETWORK_CONNECTOR.get());
            dropSelf(Blocks.NETWORK_HUB.get());
            dropSelf(Blocks.DISK_DRIVE.get());
            dropSelf(Blocks.CHARGER.get());

            add(Blocks.COMPUTER.get(), ModBlockLootTables::droppingWithInventory);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return StreamSupport.stream(super.getKnownBlocks().spliterator(), false)
                    .filter(block -> requireNonNull(block.getRegistryName()).getNamespace().equals(API.MOD_ID))
                    .filter(block -> block != Blocks.BUS_CABLE.get()) // All bus drops depend on block state.
                    .collect(Collectors.toList());
        }

        private static LootTable.Builder droppingWithInventory(final Block block) {
            return LootTable.lootTable()
                    .withPool(applyExplosionCondition(block, LootPool.lootPool()
                            .setRolls(ConstantRange.exactly(1))
                            .add(ItemLootEntry.lootTableItem(block)
                                    .apply(CopyNbt.copyData(CopyNbt.Source.BLOCK_ENTITY)
                                            .copy(ITEMS_TAG_NAME,
                                                    concat(BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME),
                                                    CopyNbt.Action.REPLACE)
                                            .copy(ENERGY_TAG_NAME,
                                                    concat(BLOCK_ENTITY_TAG_NAME_IN_ITEM, ENERGY_TAG_NAME),
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
