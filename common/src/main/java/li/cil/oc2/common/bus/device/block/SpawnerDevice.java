/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

@RPCDeviceDescription(typeName = "spawner", description = """
    Provided by monster spawners connected to a [bus interface](../block/bus_interface.md).""")
@IODeviceDescription(name = "SPAWNR")
public final class SpawnerDevice extends AbstractBlockDevice {
    private static final int GET_ENTITY_TYPE_CODE = 1;

    // --------------------------------------------------------------------- //

    public SpawnerDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets what the spawner spawns next.",
        returnValueDescription = "the name of the creature, such as `minecraft:zombie`, if any.")
    @Nullable
    public String getEntityType() {
        final SpawnData spawnData = getBlockEntity(SpawnerBlockEntity.class).getSpawner().nextSpawnData;
        if (spawnData == null) {
            return null;
        }

        return EntityType.by(spawnData.entityToSpawn())
            .map(type -> EntityType.getKey(type).toString())
            .orElse(null);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ENTITY_TYPE_CODE,
        description = "Gets what the spawner spawns next.",
        resultsDescription = "the name of the creature, such as `minecraft:zombie`, if any. Read while `OCDAV` is set to read fully.")
    public void getEntityType(final IOOutputStream results) throws IOException {
        results.writeString(Objects.requireNonNullElse(getEntityType(), ""));
    }
}
