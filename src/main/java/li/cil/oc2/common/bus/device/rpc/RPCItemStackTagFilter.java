package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;

public final class RPCItemStackTagFilter {
    public ResourceLocation item;
    public String[] tags;

    private String[][] paths; // Cache of resolved paths specified in tags.

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public CompoundNBT apply(final ItemStack stack, final CompoundNBT tag) {
        if (stack.isEmpty() || tags == null) {
            return null;
        }

        if (item != null && !Objects.equals(stack.getItem().getRegistryName(), item)) {
            return null;
        }

        validatePaths();

        final CompoundNBT filtered = new CompoundNBT();
        for (final String[] path : paths) {
            final CompoundNBT filteredByPath = filterPath(path, tag);
            if (filteredByPath != null) {
                filtered.merge(filteredByPath);
            }
        }

        return filtered;
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    private CompoundNBT filterPath(final String[] path, final CompoundNBT source) {
        if (path.length == 0) {
            return null;
        }

        final CompoundNBT result = new CompoundNBT();

        CompoundNBT currentSource = source;
        CompoundNBT currentTarget = result;
        for (int j = 0; j < path.length - 1; j++) {
            final String segment = path[j];
            if (currentSource.contains(segment, NBTTagIds.TAG_COMPOUND)) {
                currentSource = currentSource.getCompound(segment);
                currentTarget.put(segment, new CompoundNBT());
                currentTarget = currentTarget.getCompound(segment);
            } else {
                return null; // Path mismatch, inner element is not a compound tag.
            }
        }

        if (!currentSource.contains(path[path.length - 1])) {
            return null; // Cannot find tag at path.
        }

        currentTarget.put(path[path.length - 1], currentSource.get(path[path.length - 1]));

        return result;
    }

    private void validatePaths() {
        paths = new String[tags.length][];
        for (int i = 0; i < tags.length; i++) {
            if (!StringUtils.isNullOrEmpty(tags[i])) {
                paths[i] = tags[i].split("\\.");
            }
        }
    }
}
