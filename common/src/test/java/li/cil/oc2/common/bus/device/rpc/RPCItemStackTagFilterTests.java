/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MinecraftBootstrap.class)
public class RPCItemStackTagFilterTests {
    @Test
    public void pathIsCopiedFromTheSource() {
        final RPCItemStackTagFilter filter = filterFor("kept");

        final CompoundTag source = new CompoundTag();
        source.putInt("kept", 42);
        source.putInt("dropped", 23);

        final CompoundTag result = filter.apply(new ItemStack(Items.STONE), source);

        assertNotNull(result);
        assertEquals(42, result.getInt("kept"));
        assertFalse(result.contains("dropped"));
    }

    @Test
    public void nestedPathIsCopiedFromTheSource() {
        final RPCItemStackTagFilter filter = filterFor("outer.inner");

        final CompoundTag inner = new CompoundTag();
        inner.putInt("inner", 42);
        final CompoundTag source = new CompoundTag();
        source.put("outer", inner);

        final CompoundTag result = filter.apply(new ItemStack(Items.STONE), source);

        assertNotNull(result);
        assertEquals(42, result.getCompound("outer").getInt("inner"));
    }

    @Test
    public void blankTagEntriesDoNotBreakFiltering() {
        final RPCItemStackTagFilter filter = filterFor("kept", null, "", "  ");

        final CompoundTag source = new CompoundTag();
        source.putInt("kept", 42);

        final CompoundTag result = assertDoesNotThrow(() -> filter.apply(new ItemStack(Items.STONE), source));

        assertNotNull(result);
        assertEquals(42, result.getInt("kept"));
    }

    @Test
    public void filterOfNothingButBlanksYieldsNothing() {
        final RPCItemStackTagFilter filter = filterFor(null, "");

        final CompoundTag source = new CompoundTag();
        source.putInt("kept", 42);

        final CompoundTag result = assertDoesNotThrow(() -> filter.apply(new ItemStack(Items.STONE), source));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --------------------------------------------------------------------- //

    private static RPCItemStackTagFilter filterFor(final String... tags) {
        final RPCItemStackTagFilter filter = new RPCItemStackTagFilter();
        filter.tags = tags;
        return filter;
    }
}
