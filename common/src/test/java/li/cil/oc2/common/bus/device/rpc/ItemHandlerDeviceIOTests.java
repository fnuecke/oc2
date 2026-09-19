/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.MinecraftBootstrap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOCallbacks;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.vm.context.VMRuntime;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.sedna.api.Sizes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftBootstrap.class)
public final class ItemHandlerDeviceIOTests {
    private static final int GET_SLOT_COUNT = 1;
    private static final int GET_SLOTS = 2;
    private static final int GET_SLOT_LIMIT = 3;
    private static final int GET_ITEM_NAME = 4;
    private static final int GET_ITEM_ID = 5;

    private static final int REG_SELECT = 0;
    private static final int REG_FUNCTION = 1;
    private static final int REG_DATA = 2;
    private static final int REG_STATUS = 3;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int STATUS_ERROR = 0b1000_0000;
    private static final int CONTROL_EXECUTE = 0x01;

    private static final int SLOT_RECORD_SIZE = 4;
    private static final int RESULT_BUFFER_SIZE = IOCallback.MAX_DATA_SIZE;
    private static final int MAX_SLOT_RECORDS = RESULT_BUFFER_SIZE / SLOT_RECORD_SIZE;

    // --------------------------------------------------------------------- //

    @Test
    public void airIsItemIdZero() {
        assertEquals(0, BuiltInRegistries.ITEM.getId(Items.AIR));
    }

    @Test
    public void itemIdsFitInSixteenBits() {
        assertTrue(BuiltInRegistries.ITEM.size() <= 0xFFFF,
            "item registry outgrew the u16 the guest protocol uses");
    }

    @Test
    public void deviceIsNamedItems() {
        assertEquals("ITEMS", new ObjectDevice(new ItemHandlerDevice(new ArrayItemHandler(1))).getIOName());
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
    public void slotLimitClampsToByte() throws Throwable {
        assertArrayEquals(new byte[]{(byte) 255}, invoke(new ArrayItemHandler(1, 1000), GET_SLOT_LIMIT, 0));
    }

    @Test
    public void onlyRegistryLookupsRunOffTheServerThread() {
        assertTrue(isSynchronized(GET_SLOT_COUNT), "reading the slot count touches the inventory");
        assertTrue(isSynchronized(GET_SLOTS), "reading slots touches the inventory");
        assertTrue(isSynchronized(GET_SLOT_LIMIT), "reading a slot limit touches the inventory");
        assertFalse(isSynchronized(GET_ITEM_NAME), "a name lookup should not cost a tick");
        assertFalse(isSynchronized(GET_ITEM_ID), "an id lookup should not cost a tick");
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
    public void missingArgumentsReachTheGuestAsAnArgumentError() {
        final IODeviceBusAdapter adapter = adapterFor(new ArrayItemHandler(4));
        adapter.store(REG_SELECT, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_FUNCTION, GET_SLOTS, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_STATUS, CONTROL_EXECUTE, Sizes.SIZE_8_LOG2);
        adapter.tick();

        assertEquals(STATUS_ERROR, (int) adapter.load(REG_STATUS, Sizes.SIZE_8_LOG2));
        assertEquals(0x05, (int) adapter.load(REG_DATA, Sizes.SIZE_8_LOG2),
            "too few arguments is the guest's mistake, not an internal error");
    }

    @Test
    public void slotLimitIsReported() throws Throwable {
        assertArrayEquals(new byte[]{64}, invoke(new ArrayItemHandler(2), GET_SLOT_LIMIT, 1));
    }

    @Test
    public void slotLimitRejectsSlotPastTheEnd() {
        assertThrows(IllegalArgumentException.class, () -> invoke(new ArrayItemHandler(2), GET_SLOT_LIMIT, 2));
    }

    @Test
    public void itemNameRejectsIdPastTheRegistry() {
        final int id = BuiltInRegistries.ITEM.size();
        assertTrue(id <= 0xFFFF);

        assertThrows(IllegalArgumentException.class,
            () -> invoke(new ArrayItemHandler(1), GET_ITEM_NAME, id & 0xFF, id >>> 8));
    }

    @Test
    public void itemIdResolvesFromName() throws Throwable {
        final byte[] results = invoke(new ArrayItemHandler(1), GET_ITEM_ID, "minecraft:redstone");

        assertEquals(BuiltInRegistries.ITEM.getId(Items.REDSTONE), (results[0] & 0xFF) | ((results[1] & 0xFF) << 8));
    }

    @Test
    public void itemIdAcceptsBareNameAsMinecraft() throws Throwable {
        assertArrayEquals(invoke(new ArrayItemHandler(1), GET_ITEM_ID, "minecraft:redstone"),
            invoke(new ArrayItemHandler(1), GET_ITEM_ID, "redstone"));
    }

    @Test
    public void itemIdAcceptsATerminatedName() throws Throwable {
        assertArrayEquals(invoke(new ArrayItemHandler(1), GET_ITEM_ID, "minecraft:redstone"),
            invoke(new ArrayItemHandler(1), GET_ITEM_ID, "minecraft:redstone\0"));
    }

    @Test
    public void itemIdRejectsUnknownName() {
        assertThrows(IllegalArgumentException.class,
            () -> invoke(new ArrayItemHandler(1), GET_ITEM_ID, "minecraft:definitely_not_an_item"));
    }

    @Test
    public void itemIdRejectsMalformedName() {
        assertThrows(IllegalArgumentException.class,
            () -> invoke(new ArrayItemHandler(1), GET_ITEM_ID, "NOT A RESOURCE LOCATION"));
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
        assertEquals(0x05, (int) adapter.load(REG_DATA, Sizes.SIZE_8_LOG2));
    }

    @Test
    public void itemNameReachesGuestThroughRegisters() {
        final int id = BuiltInRegistries.ITEM.getId(Items.REDSTONE);

        final IODeviceBusAdapter adapter = adapterFor(new ArrayItemHandler(1));
        adapter.store(REG_SELECT, 0, Sizes.SIZE_8_LOG2);
        adapter.store(REG_FUNCTION, GET_ITEM_NAME, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, id & 0xFF, Sizes.SIZE_8_LOG2);
        adapter.store(REG_DATA, id >>> 8, Sizes.SIZE_8_LOG2);
        adapter.store(REG_STATUS, CONTROL_EXECUTE, Sizes.SIZE_8_LOG2);

        final StringBuilder name = new StringBuilder();
        while ((adapter.load(REG_STATUS, Sizes.SIZE_8_LOG2) & STATUS_DATA_AVAILABLE) != 0) {
            name.append((char) adapter.load(REG_DATA, Sizes.SIZE_8_LOG2));
        }

        assertEquals("minecraft:redstone", name.toString());
    }

    // --------------------------------------------------------------------- //

    private static boolean isSynchronized(final int code) {
        return IOCallbacks.collectMethods(new ItemHandlerDevice(new ArrayItemHandler(1))).stream()
            .filter(m -> m.getCode() == code)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no function with code " + code))
            .isSynchronized();
    }

    private static IODeviceBusAdapter adapterFor(final ItemHandler handler) {
        final ObjectDevice device = new ObjectDevice(new ItemHandlerDevice(handler));
        final DeviceBusController controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(Set.of(device));
        when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(UUID.randomUUID()));

        final IODeviceBusAdapter adapter = new IODeviceBusAdapter(mock(VMRuntime.class));
        adapter.rebuild(controller);
        return adapter;
    }

    private static byte[] invoke(final ItemHandler handler, final int code, final int... arguments) throws Throwable {
        final byte[] bytes = new byte[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            bytes[i] = (byte) arguments[i];
        }
        return invoke(handler, code, bytes);
    }

    private static byte[] invoke(final ItemHandler handler, final int code, final String arguments) throws Throwable {
        return invoke(handler, code, arguments.getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] invoke(final ItemHandler handler, final int code, final byte[] arguments) throws Throwable {
        final IOMethod method = IOCallbacks.collectMethods(new ItemHandlerDevice(handler)).stream()
            .filter(m -> m.getCode() == code)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no function with code " + code));

        final ByteArrayOutputStream results = new ByteArrayOutputStream();
        method.invoke(new ByteArrayInputStream(arguments), results);
        return results.toByteArray();
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
