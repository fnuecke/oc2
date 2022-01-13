package li.cil.oc2.data;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static li.cil.oc2.common.Constants.*;

public final class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(final DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void validate(final Map<ResourceLocation, LootTable> map, final ValidationContext context) {
        map.forEach((location, table) -> LootTables.validate(context, location, table));
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
        return singletonList(Pair.of(ModBlockLootTables::new, LootContextParamSets.BLOCK));
    }

    public static final class ModBlockLootTables extends BlockLoot {
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
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(block)
                        .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                            .copy(ITEMS_TAG_NAME,
                                concat(BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME),
                                CopyNbtFunction.MergeStrategy.REPLACE)
                            .copy(ENERGY_TAG_NAME,
                                concat(BLOCK_ENTITY_TAG_NAME_IN_ITEM, ENERGY_TAG_NAME),
                                CopyNbtFunction.MergeStrategy.REPLACE)
                        )
                    )
                ));
        }

        private static String concat(final String... paths) {
            return String.join(".", paths);
        }
    }
}
