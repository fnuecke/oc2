/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.MinecraftBootstrap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.oc2.common.bus.TestIOInvocation;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.sedna.api.Sizes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.EOFException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static li.cil.oc2.common.bus.TestIOInvocation.method;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftBootstrap.class)
public final class ItemHandlerDeviceIOTests {
    private static final int GET_SLOT_COUNT = 1;
    private static final int GET_SLOTS = 2;
    private static final int GET_SLOT_LIMIT = 3;

    private static final int REG_SELECT = 0;
    private static final int REG_FUNCTION = 1;
    private static final int REG_DATA = 2;
    private static final int REG_STATUS = 3;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int STATUS_ERROR = 0b1000_0000;
    private static final int ERROR_INVALID_ARGUMENTS = 0x05;
    private static final int CONTROL_EXECUTE = 0x01;

    private static final int SLOT_RECORD_SIZE = 4;
    private static final int RESULT_BUFFER_SIZE = IOCallback.MAX_DATA_SIZE;
    private static final int MAX_SLOT_RECORDS = RESULT_BUFFER_SIZE / SLOT_RECORD_SIZE;

    // --------------------------------------------------------------------- //

    @Test
    public void deviceIsNamedItems() {
        assertEquals("ITEMS", new ObjectDevice(new ItemHandlerDevice(new ArrayItemHandler(1))).getIOName());
    }

    @Test
    public void slotReadsRunOnTheServerThread() {
        assertTrue(isSynchronized(GET_SLOT_COUNT), "reading the slot count touches the inventory");
        assertTrue(isSynchronized(GET_SLOTS), "reading slots touches the inventory");
        assertTrue(isSynchronized(GET_SLOT_LIMIT), "reading a slot limit touches the inventory");
    }

    @Test
    public void slotCountIsReported() throws Throwable {
        assertArrayEquals(new byte[]{27}, invoke(new ArrayItemHandler(27), GET_SLOT_COUNT));
    }

    @Test
    public void slotCountClampsToByte() throws Throwable {
        assertArrayEquals(new byte[]{(byte) 255}, invoke(new ArrayItemHandler(300), GET_SLOT_COUNT));
    }

    @Test
    public void emptySlotReadsAsAir() throws Throwable {
        assertArrayEquals(new byte[]{0, 0, 0, 0}, invoke(new ArrayItemHandler(1), GET_SLOTS, 0, 1));
    }

    @Test
    public void slotRecordCarriesIdAndCount() throws Throwable {
        final ArrayItemHandler handler = new ArrayItemHandler(1);
        handler.stacks[0] = new ItemStack(Items.REDSTONE, 42);

        final int id = BuiltInRegistries.ITEM.getId(Items.REDSTONE);
        assertArrayEquals(new byte[]{(byte) (id & 0xFF), (byte) (id >>> 8), 42, 0},
            invoke(handler, GET_SLOTS, 0, 1));
    }

    @Test
    public void stackCountClampsToByte() throws Throwable {
        final ArrayItemHandler handler = new ArrayItemHandler(1);
        handler.stacks[0] = new ItemStack(Items.REDSTONE, 1000);

        assertEquals((byte) 255, invoke(handler, GET_SLOTS, 0, 1)[2]);
    }

    @Test
    public void undamagedToolReportsNoDamage() throws Throwable {
        final ArrayItemHandler handler = new ArrayItemHandler(1);
        handler.stacks[0] = new ItemStack(Items.IRON_PICKAXE);

        assertEquals(0, invoke(handler, GET_SLOTS, 0, 1)[3]);
    }

    @Test
    public void fullyDamagedToolReportsMaximumDamage() throws Throwable {
        final ArrayItemHandler handler = new ArrayItemHandler(1);
        final ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.setDamageValue(stack.getMaxDamage());
        handler.stacks[0] = stack;

        assertEquals((byte) 255, invoke(handler, GET_SLOTS, 0, 1)[3]);
    }

    @Test
    public void anyDamageIsDistinguishableFromNone() throws Throwable {
        final ArrayItemHandler handler = new ArrayItemHandler(1);
        final ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.setDamageValue(1);
        handler.stacks[0] = stack;

        assertNotEquals(0, invoke(handler, GET_SLOTS, 0, 1)[3]);
    }

    @Test
    public void bulkReadStartsAtTheRequestedOffset() throws Throwable {
        final ArrayItemHandler handler = new ArrayItemHandler(4);
        handler.stacks[2] = new ItemStack(Items.REDSTONE, 7);

        final byte[] results = invoke(handler, GET_SLOTS, 2, 2);
        assertEquals(2 * SLOT_RECORD_SIZE, results.length);
        assertEquals(7, results[2]);
    }

    @Test
    public void bulkReadClampsToRemainingSlots() throws Throwable {
        assertEquals(2 * SLOT_RECORD_SIZE, invoke(new ArrayItemHandler(4), GET_SLOTS, 2, 64).length);
    }

    @Test
    public void bulkReadRejectsCountPastTheResultBuffer() {
        assertThrows(IllegalArgumentException.class,
            () -> invoke(new ArrayItemHandler(200), GET_SLOTS, 0, MAX_SLOT_RECORDS + 1));
    }

    @Test
    public void bulkReadRejectsZeroCount() {
        assertThrows(IllegalArgumentException.class, () -> invoke(new ArrayItemHandler(4), GET_SLOTS, 0, 0));
    }

    @Test
    public void bulkReadRejectsOffsetPastTheEnd() {
        assertThrows(IllegalArgumentException.class, () -> invoke(new ArrayItemHandler(4), GET_SLOTS, 4, 1));
    }

    @Test
    public void bulkReadRejectsMissingArguments() {
        assertThrows(EOFException.class, () -> invoke(new ArrayItemHandler(4), GET_SLOTS, 0));
    }

    @Test
    public void slotLimitIsReported() throws Throwable {
        assertArrayEquals(new byte[]{64}, invoke(new ArrayItemHandler(2), GET_SLOT_LIMIT, 1));
    }

    @Test
    public void slotLimitClampsToByte() throws Throwable {
        assertArrayEquals(new byte[]{(byte) 255}, invoke(new ArrayItemHandler(1, 1000), GET_SLOT_LIMIT, 0));
    }

    @Test
    public void slotLimitRejectsSlotPastTheEnd() {
        assertThrows(IllegalArgumentException.class, () -> invoke(new ArrayItemHandler(2), GET_SLOT_LIMIT, 2));
    }

    @Test
    public void missingArgumentsReachTheGuestAsAnArgumentError() {
        final IODeviceBusAdapter adapter = adapterFor(new ArrayItemHandler(4));
        adapter.store(REG_SELECT, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_FUNCTION, GET_SLOTS, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_STATUS, CONTROL_EXECUTE, Sizes.SIZE_8_LOG2);
        adapter.tick();

        assertEquals(STATUS_ERROR, (int) adapter.load(REG_STATUS, Sizes.SIZE_8_LOG2));
        assertEquals(ERROR_INVALID_ARGUMENTS, (int) adapter.load(REG_DATA, Sizes.SIZE_8_LOG2),
            "too few arguments is the guest's mistake, not an internal error");
    }

    @Test
    public void bulkReadFillsAdapterResultBufferExactly() {
        final ArrayItemHandler handler = new ArrayItemHandler(MAX_SLOT_RECORDS);
        Arrays.setAll(handler.stacks, slot -> new ItemStack(Items.REDSTONE, slot + 1));

        final IODeviceBusAdapter adapter = adapterFor(handler);
        adapter.store(REG_SELECT, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_FUNCTION, GET_SLOTS, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, MAX_SLOT_RECORDS, Sizes.SIZE_8_LOG2);
        adapter.store(REG_STATUS, CONTROL_EXECUTE, Sizes.SIZE_8_LOG2);
        adapter.tick();

        assertEquals(STATUS_DATA_AVAILABLE, (int) adapter.load(REG_STATUS, Sizes.SIZE_8_LOG2),
            "a full bulk read must not overflow the result buffer");

        final int expectedId = BuiltInRegistries.ITEM.getId(Items.REDSTONE);
        int count = 0;
        while ((adapter.load(REG_STATUS, Sizes.SIZE_8_LOG2) & STATUS_DATA_AVAILABLE) != 0) {
            final int value = (int) adapter.load(REG_DATA, Sizes.SIZE_8_LOG2);
            switch (count % SLOT_RECORD_SIZE) {
                case 0 -> assertEquals(expectedId & 0xFF, value);
                case 1 -> assertEquals(expectedId >>> 8, value);
                case 2 -> assertEquals(count / SLOT_RECORD_SIZE + 1, value);
                default -> assertEquals(0, value);
            }
            count++;
        }

        assertEquals(RESULT_BUFFER_SIZE, count);
    }

    @Test
    public void badSlotReachesTheGuestAsAnArgumentError() {
        final IODeviceBusAdapter adapter = adapterFor(new ArrayItemHandler(4));
        adapter.store(REG_SELECT, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_FUNCTION, GET_SLOTS, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, 9, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, 1, Sizes.SIZE_8_LOG2);
        adapter.store(REG_STATUS, CONTROL_EXECUTE, Sizes.SIZE_8_LOG2);
        adapter.tick();

        assertEquals(STATUS_ERROR, (int) adapter.load(REG_STATUS, Sizes.SIZE_8_LOG2));
        assertEquals(ERROR_INVALID_ARGUMENTS, (int) adapter.load(REG_DATA, Sizes.SIZE_8_LOG2));
    }

    // --------------------------------------------------------------------- //

    private static boolean isSynchronized(final int code) {
        return method(new ItemHandlerDevice(new ArrayItemHandler(1)), code).isSynchronized();
    }

    private static IODeviceBusAdapter adapterFor(final ItemHandler handler) {
        final ObjectDevice device = new ObjectDevice(new ItemHandlerDevice(handler));
        final DeviceBusController controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(Set.of(device));
        when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(UUID.randomUUID()));

        final IODeviceBusAdapter adapter = new IODeviceBusAdapter(amount -> true);
        adapter.rebuild(controller);
        return adapter;
    }

    private static byte[] invoke(final ItemHandler handler, final int code, final int... arguments) throws Throwable {
        return TestIOInvocation.invoke(method(new ItemHandlerDevice(handler), code), TestIOInvocation.bytes(arguments));
    }

    // --------------------------------------------------------------------- //

    private static final class ArrayItemHandler implements ItemHandler {
        private final ItemStack[] stacks;
        private final int limit;

        ArrayItemHandler(final int slots) {
            this(slots, 64);
        }

        ArrayItemHandler(final int slots, final int limit) {
            this.stacks = new ItemStack[slots];
            this.limit = limit;
            Arrays.fill(stacks, ItemStack.EMPTY);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return limit;
        }

        @Override
        public int getSlots() {
            return stacks.length;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return stacks[slot];
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
