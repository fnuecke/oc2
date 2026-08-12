/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public final class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)), registries);
    }

    // ------------------------------------------------------------- //

    public static final class ModBlockLootTables extends BlockLootSubProvider {
        public ModBlockLootTables(final HolderLookup.Provider registries) {
            super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            dropSelf(Blocks.CHARGER.get());
            dropSelf(Blocks.COMPUTER.get());
            dropSelf(Blocks.DISK_DRIVE.get());
            dropSelf(Blocks.KEYBOARD.get());
            dropSelf(Blocks.NETWORK_CONNECTOR.get());
            dropSelf(Blocks.NETWORK_HUB.get());
            dropSelf(Blocks.PROJECTOR.get());
            dropSelf(Blocks.REDSTONE_INTERFACE.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return StreamSupport.stream(super.getKnownBlocks().spliterator(), false)
                    .filter(block -> Objects.equals(requireNonNull(BuiltInRegistries.BLOCK.getKey(block)).getNamespace(), API.MOD_ID))
                    .filter(block -> block != Blocks.BUS_CABLE.get()) // All bus drops depend on block state.
                    .collect(Collectors.toList());
        }
    }
}
