/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import li.cil.oc2.MinecraftBootstrap;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.serialization.gson.SideJsonDeserializer;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MinecraftBootstrap.class)
public class GuestInputBoundsTests {
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
    public void sideByIndexFollowsMinecraftNumbering() {
        for (int i = 0; i < 6; i++) {
            assertEquals(Direction.from3DDataValue(i), Side.byIndex(i).getDirection());
        }
        assertThrows(IllegalArgumentException.class, () -> Side.byIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> Side.byIndex(6));
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
}
