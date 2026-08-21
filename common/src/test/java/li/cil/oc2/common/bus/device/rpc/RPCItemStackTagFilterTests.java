/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RPCItemStackTagFilterTests {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

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

    @Test
    public void resolvedPathsAreCachedRatherThanRebuiltPerCall() {
        final RPCItemStackTagFilter filter = filterFor("kept");
        final ItemStack stack = new ItemStack(Items.STONE);
        final CompoundTag source = new CompoundTag();
        source.putInt("kept", 42);

        filter.apply(stack, source);
        final Object first = paths(filter);
        filter.apply(stack, source);

        assertSame(first, paths(filter), "paths are documented as a cache, so they should survive a second call");
    }

    // --------------------------------------------------------------------- //

    private static RPCItemStackTagFilter filterFor(final String... tags) {
        final RPCItemStackTagFilter filter = new RPCItemStackTagFilter();
        filter.tags = tags;
        return filter;
    }

    private static Object paths(final RPCItemStackTagFilter filter) {
        // Only used here, so let's just grab it with reflection...
        try {
            final Field field = RPCItemStackTagFilter.class.getDeclaredField("paths");
            field.setAccessible(true);
            return field.get(filter);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("could not read the filter's resolved paths", e);
        }
    }
}
