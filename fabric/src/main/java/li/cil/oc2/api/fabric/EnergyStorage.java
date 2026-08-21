/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.fabric;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The lookups we do energy interop through on Fabric.
 * <p>
 * {@link #SIDED} and {@link #ITEM} are Team Reborn's standard lookups, re-exported for clarity.
 * <p>
 * {@link #ENTITY} has no standard counterpart: Team Reborn's API does not define an entity lookup
 * for energy, so we have to roll our own.
 */
public abstract class EnergyStorage {
    /**
     * Entity access to energy storages, {@code oc2:entity_energy_storage}.
     */
    public static final EntityApiLookup<team.reborn.energy.api.EnergyStorage, Direction> ENTITY =
        EntityApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "entity_energy_storage"),
            team.reborn.energy.api.EnergyStorage.class, Direction.class);

    /**
     * Team Reborn's sided block lookup, {@code teamreborn:sided_energy}.
     */
    public static final BlockApiLookup<team.reborn.energy.api.EnergyStorage, Direction> SIDED =
        team.reborn.energy.api.EnergyStorage.SIDED;

    /**
     * Team Reborn's item lookup, {@code teamreborn:energy}.
     */
    public static final ItemApiLookup<team.reborn.energy.api.EnergyStorage, ContainerItemContext> ITEM =
        team.reborn.energy.api.EnergyStorage.ITEM;

    private EnergyStorage() {
    }
}
