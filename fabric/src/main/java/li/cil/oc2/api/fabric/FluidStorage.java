/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.fabric;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The lookups we do fluid storage interop through on Fabric.
 * <p>
 * {@link #SIDED} and {@link #ITEM} are Fabric's standard lookups, re-exported for clarity.
 * <p>
 * {@link #ENTITY} has no standard counterpart: Fabric's API does not define an entity lookup for
 * fluid storage, so we roll our own.
 */
public abstract class FluidStorage {
    /**
     * Entity access to fluid storages, {@code oc2:entity_fluid_storage}.
     */
    public static final EntityApiLookup<Storage<FluidVariant>, Direction> ENTITY =
        EntityApiLookup.get(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "entity_fluid_storage"),
            Storage.asClass(), Direction.class);

    /**
     * The transfer API's sided block lookup, {@code fabric:sided_fluid_storage}.
     */
    public static final BlockApiLookup<Storage<FluidVariant>, Direction> SIDED =
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED;

    /**
     * The transfer API's item lookup, {@code fabric:fluid_storage}.
     */
    public static final ItemApiLookup<Storage<FluidVariant>, ContainerItemContext> ITEM =
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM;

    private FluidStorage() {
    }
}
