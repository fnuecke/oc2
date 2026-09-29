/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

@RPCDeviceDescription(typeNames = {"beacon"}, description = """
    Provided by beacons connected to a [bus interface](../block/bus_interface.md).""")
@IODeviceDescription(name = "BEACON")
public final class BeaconDevice extends AbstractBlockDevice {
    private static final int GET_LEVELS_CODE = 1;
    private static final int GET_PRIMARY_EFFECT_CODE = 2;
    private static final int GET_SECONDARY_EFFECT_CODE = 3;

    // --------------------------------------------------------------------- //

    public BeaconDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many levels the pyramid below the beacon has.",
        returnValueDescription = "the levels, from `0` to `4`.")
    public int getLevels() {
        return getBeacon().levels;
    }

    @Callback(description = "Gets the primary effect the beacon grants.",
        returnValueDescription = "the name of the effect, such as `minecraft:speed`, if set.")
    @Nullable
    public String getPrimaryEffect() {
        return toName(getBeacon().primaryPower);
    }

    @Callback(description = "Gets the secondary effect the beacon grants.",
        returnValueDescription = "the name of the effect, such as `minecraft:regeneration`, if set.")
    @Nullable
    public String getSecondaryEffect() {
        return toName(getBeacon().secondaryPower);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_LEVELS_CODE,
        description = "Reads how many levels the pyramid below the beacon has.",
        resultsDescription = "one byte, the levels.")
    public void getLevels(final IOOutputStream results) throws IOException {
        results.writeU8(getLevels());
    }

    @IOCallback(value = GET_PRIMARY_EFFECT_CODE,
        description = "Reads the primary effect the beacon grants.",
        resultsDescription = "the name, such as `minecraft:speed`, if set. Read while `OCDAV` is set to read fully.")
    public void getPrimaryEffect(final IOOutputStream results) throws IOException {
        results.writeString(Objects.requireNonNullElse(getPrimaryEffect(), ""));
    }

    @IOCallback(value = GET_SECONDARY_EFFECT_CODE,
        description = "Reads the secondary effect the beacon grants.",
        resultsDescription = "the name, such as `minecraft:speed`, if set. Read while `OCDAV` is set to read fully.")
    public void getSecondaryEffect(final IOOutputStream results) throws IOException {
        results.writeString(Objects.requireNonNullElse(getSecondaryEffect(), ""));
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private static String toName(@Nullable final Holder<MobEffect> effect) {
        return effect == null ? null : effect.unwrapKey().map(key -> key.location().toString()).orElse(null);
    }

    private BeaconBlockEntity getBeacon() {
        return getBlockEntity(BeaconBlockEntity.class);
    }
}
