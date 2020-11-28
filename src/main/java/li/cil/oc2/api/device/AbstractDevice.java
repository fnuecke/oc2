package li.cil.oc2.api.device;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

/**
 * Convenience base class for {@link Device} implementations.
 * <p>
 * In particular, this implements the logic needed for generating and
 * storing the unique ID for this device.
 */
public abstract class AbstractDevice implements Device, INBTSerializable<CompoundNBT> {
    protected static final String UUID_NBT_TAG_NAME = "uuid";

    protected final List<String> typeNames;
    protected UUID uuid;

    protected AbstractDevice() {
        this(Collections.emptyList());
    }

    protected AbstractDevice(final Collection<String> typeNames) {
        this.typeNames = new ArrayList<>(typeNames);
        this.uuid = java.util.UUID.randomUUID();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    public void setUniqueId(final UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public List<String> getTypeNames() {
        return typeNames;
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();
        nbt.putUniqueId(UUID_NBT_TAG_NAME, uuid);
        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT nbt) {
        if (nbt.hasUniqueId(UUID_NBT_TAG_NAME)) {
            uuid = nbt.getUniqueId(UUID_NBT_TAG_NAME);
        }
    }
}
