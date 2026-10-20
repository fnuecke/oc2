/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import li.cil.oc2.api.API;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

import static li.cil.oc2.common.block.Blocks.*;
import static li.cil.oc2.common.tags.BlockTags.*;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    private static final TagKey<Block> SABLE_SILENT_ASSEMBLY_REMOVAL = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("sable", "silent_assembly_removal"));

    public ModBlockTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider, final ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        tag(DEVICES).add(
            COMPUTER.get(),
            REDSTONE_INTERFACE.get(),
            DISK_DRIVE.get(),
            FLASH_DRIVE.get(),
            PROJECTOR.get(),
            TRANSPOSER.get()
        );
        tag(CABLES).add(
            BUS_CABLE.get()
        );
        tag(WRENCH_BREAKABLE).add(
            COMPUTER.get(),
            BUS_CABLE.get(),
            NETWORK_CONNECTOR.get(),
            NETWORK_HUB.get(),
            INTERNET_GATEWAY.get(),
            REDSTONE_INTERFACE.get(),
            DISK_DRIVE.get(),
            FLASH_DRIVE.get(),
            CHARGER.get(),
            PROJECTOR.get(),
            TERMINAL.get(),
            TRANSPOSER.get()
        );

        // Makes Sable remove the block entity before loading its copy, as Create does.
        tag(SABLE_SILENT_ASSEMBLY_REMOVAL).add(
            BUS_CABLE.get(),
            CHARGER.get(),
            COMPUTER.get(),
            CREATIVE_ENERGY.get(),
            DISK_DRIVE.get(),
            FLASH_DRIVE.get(),
            KEYBOARD.get(),
            NETWORK_CONNECTOR.get(),
            NETWORK_HUB.get(),
            INTERNET_GATEWAY.get(),
            PROJECTOR.get(),
            REDSTONE_INTERFACE.get(),
            TERMINAL.get(),
            TRANSPOSER.get()
        );
    }
}
