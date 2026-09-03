/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.serialization.gson.SideJsonDeserializer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GuestInputBoundsTests {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void sideIndicesInRangeAreAccepted() {
        for (int index = 0; index < 6; index++) {
            assertEquals(Side.byIndex(index), deserializeSide(index));
        }
    }

    @Test
    public void sideIndexPastTheEndIsRejected() {
        assertThrows(JsonParseException.class, () -> deserializeSide(6));
    }

    @Test
    public void negativeSideIndexIsRejected() {
        assertThrows(JsonParseException.class, () -> deserializeSide(-1));
    }

    @Test
    public void slotPastTheEndIsRejected() {
        final ItemHandlerDevice device = new ItemHandlerDevice(new EmptyItemHandler(2));

        assertThrows(IllegalArgumentException.class, () -> device.getItemStackInSlot(2));
        assertThrows(IllegalArgumentException.class, () -> device.getItemSlotLimit(2));
    }

    @Test
    public void negativeSlotIsRejected() {
        final ItemHandlerDevice device = new ItemHandlerDevice(new EmptyItemHandler(2));

        assertThrows(IllegalArgumentException.class, () -> device.getItemStackInSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> device.getItemSlotLimit(-1));
    }

    @Test
    public void slotsInRangeAreAccepted() {
        final ItemHandlerDevice device = new ItemHandlerDevice(new EmptyItemHandler(2));

        assertTrue(device.getItemStackInSlot(0).isEmpty());
        assertTrue(device.getItemStackInSlot(1).isEmpty());
    }

    @Test
    public void handlerWithNoSlotsRejectsEverything() {
        final ItemHandlerDevice device = new ItemHandlerDevice(new EmptyItemHandler(0));

        assertThrows(IllegalArgumentException.class, () -> device.getItemStackInSlot(0));
    }

    // --------------------------------------------------------------------- //

    private static Side deserializeSide(final int ordinal) {
        return new SideJsonDeserializer().deserialize(new JsonPrimitive(ordinal), Side.class, null);
    }

    private record EmptyItemHandler(int slots) implements ItemHandler {
        @Override
        public int getSlots() {
            return slots;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return ItemStack.EMPTY;
        }
    }
}
