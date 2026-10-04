/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOBusContext;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.LifecycleAwareDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.InterruptController;
import li.cil.sedna.api.device.bus.DeviceDescription;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class IODeviceBusAdapterTests {
    private static final int REG_SELECT = 0;
    private static final int REG_FUNCTION = 1;
    private static final int REG_DATA = 2;
    private static final int REG_STATUS = 3;
    private static final int REG_EVENT_CONTROL = 4;
    private static final int REG_EVENT_DATA = 5;

    private static final int STATUS_BUSY = 0b0000_0001;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int STATUS_ERROR = 0b1000_0000;

    private static final int EVENT_QUEUE = 0b0000_0001;
    private static final int EVENT_INTERRUPT = 0b0000_0010;
    private static final int EVENT_OVERFLOW = 0b0100_0000;
    private static final int EVENT_PENDING = 0b1000_0000;
    private static final int NO_EVENT = 0xFF;

    private static final int INTERRUPT = 5;

    private static final int CONTROL_ABORT = 0x00;
    private static final int CONTROL_EXECUTE = 0x01;

    private static final int ERROR_NO_SUCH_DEVICE = 0x01;
    private static final int ERROR_NO_SUCH_FUNCTION = 0x02;
    private static final int ERROR_INVALID_ARGUMENTS = 0x05;

    private static final int FUNCTION_ECHO = 1;
    private static final int FUNCTION_SYNCHRONIZED = 2;
    private static final int FUNCTION_THROWS_ILLEGAL_ARGUMENT = 3;
    private static final int FUNCTION_THROWS_INTERNAL = 4;
    private static final int FUNCTION_PRICED = 5;

    private static final UUID DEVICE_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID LOWER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private IODeviceBusAdapter adapter;
    private TestInterruptController interrupts;
    private DeviceBusController controller;
    private TestTarget target;
    private ObjectDevice subject;
    private int energy;

    @BeforeEach
    public void setupEach() {
        energy = Integer.MAX_VALUE;
        adapter = new IODeviceBusAdapter(this::consumeEnergy);
        interrupts = new TestInterruptController();
        adapter.getInterrupt().set(INTERRUPT, interrupts);
        controller = mock(DeviceBusController.class);
        target = new TestTarget();
        subject = new ObjectDevice(target, "test");
        setDevices(subject);
        adapter.rebuild(controller);
        adapter.mountDevices();
    }

    // --------------------------------------------------------------------- //

    @Test
    public void enumeratesDevicesWithFunctions() {
        final List<DeviceDescription> descriptions = adapter.getDescriptions();
        assertEquals(1, descriptions.size());

        final DeviceDescription description = descriptions.get(0);
        assertEquals(IODeviceBusAdapter.DEVICE_CLASS, description.deviceClass());
        assertEquals("TEST", description.name());
        assertEquals(0, description.attributes());
        assertEquals(DEVICE_UUID.toString(), description.id());
    }

    @Test
    public void devicesWithoutFunctionsAreHidden() {
        setDevices(new ObjectDevice(new Object(), "empty"));
        adapter.rebuild(controller);

        assertTrue(adapter.getDescriptions().isEmpty());
    }

    @Test
    public void deviceReachableTwiceEnumeratesOnce() {
        final ObjectDevice device = new ObjectDevice(target, "test");
        when(controller.getDevices()).thenReturn(Set.of(device));
        when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(
            UUID.fromString("00000000-0000-0000-0000-00000000000f"), DEVICE_UUID));
        adapter.rebuild(controller);

        assertEquals(1, adapter.getDescriptions().size());
        assertEquals(DEVICE_UUID.toString(), adapter.getDescriptions().get(0).id(),
            "the lowest identifier should be the stable one");
    }

    @Test
    public void rescanDoesNotRetargetASelectedDevice() {
        write(REG_SELECT, 0);

        final ObjectDevice other = new ObjectDevice(new OtherTarget(), "other");
        when(controller.getDevices()).thenReturn(Set.of(subject, other));
        when(controller.getDeviceIdentifiers(subject)).thenReturn(Set.of(DEVICE_UUID));
        when(controller.getDeviceIdentifiers(other)).thenReturn(Set.of(LOWER_UUID));
        adapter.rebuild(controller);

        assertEquals("OTHER", adapter.getDescriptions().get(0).name(), "the inserted device should sort first");
        assertEquals("TEST", adapter.getDescriptions().get(1).name());

        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 1);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(2, read(REG_DATA), "the call must still reach the device that was selected, not index 0");
    }

    @Test
    public void devicesSharingAnIdentifierEnumerateSeparately() {
        final ObjectDevice other = new ObjectDevice(new OtherTarget(), "other");
        when(controller.getDevices()).thenReturn(Set.of(subject, other));
        when(controller.getDeviceIdentifiers(subject)).thenReturn(Set.of(DEVICE_UUID));
        when(controller.getDeviceIdentifiers(other)).thenReturn(Set.of(DEVICE_UUID));
        adapter.rebuild(controller);

        assertEquals(2, adapter.getDescriptions().size(), "each device in a group should get its own sub-index");

        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 1);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(101, read(REG_DATA), "index 0 should reach the first device of the group");

        write(REG_SELECT, 1);
        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 1);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(2, read(REG_DATA), "index 1 should reach the second device of the group");
    }

    @Test
    public void unsynchronizedTransactionRoundTrips() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 41);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_DATA_AVAILABLE, read(REG_STATUS));
        assertEquals(42, read(REG_DATA));
        assertEquals(0, read(REG_STATUS), "no data should remain");
    }

    @Test
    public void synchronizedCallCompletesOnTick() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_BUSY, read(REG_STATUS));
        assertEquals(0, read(REG_DATA), "results must not be readable while busy");

        adapter.tick();

        assertEquals(STATUS_DATA_AVAILABLE, read(REG_STATUS));
        assertEquals(0x5A, read(REG_DATA));
    }

    @Test
    public void pricedCallDrainsEnergyOnTick() {
        energy = 5;
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_PRICED);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(5, energy, "energy was drained off the server thread");

        adapter.tick();

        assertEquals(3, energy);
        assertEquals(1, target.pricedCalls);
        assertEquals(STATUS_DATA_AVAILABLE, read(REG_STATUS));
    }

    @Test
    public void pricedCallFailsWithoutEnergy() {
        energy = 1;
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_PRICED);
        write(REG_STATUS, CONTROL_EXECUTE);

        adapter.tick();

        assertEquals(1, energy);
        assertEquals(0, target.pricedCalls);
        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_INVALID_ARGUMENTS, read(REG_DATA));
    }

    @Test
    public void tickWithoutPendingCallDoesNothing() {
        adapter.tick();
        assertEquals(0, read(REG_STATUS));
        assertEquals(0, target.synchronizedCalls);
    }

    @Test
    public void abortMidStreamDiscardsTransaction() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 41);
        write(REG_STATUS, CONTROL_ABORT);

        assertEquals(0, read(REG_STATUS));

        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 1);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(2, read(REG_DATA));
    }

    @Test
    public void abortedSynchronizedCallDiscardsItsResult() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);
        write(REG_STATUS, CONTROL_ABORT);

        assertEquals(STATUS_BUSY, read(REG_STATUS), "the device stays busy until the call drains");

        adapter.tick();

        assertEquals(0, read(REG_STATUS), "the abandoned result must not be delivered");

        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 7);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(8, read(REG_DATA));
    }

    @Test
    public void writesWhileBusyAreIgnored() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);

        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_DATA, 99);

        assertEquals(STATUS_BUSY, read(REG_STATUS), "busy is the whole signal; error must not also be set");

        adapter.tick();

        assertEquals(STATUS_DATA_AVAILABLE, read(REG_STATUS), "the ignored writes must not have disturbed the call");
        assertEquals(0x5A, read(REG_DATA));
    }

    @Test
    public void unknownSubDeviceFails() {
        write(REG_SELECT, 5);
        write(REG_FUNCTION, FUNCTION_ECHO);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_NO_SUCH_DEVICE, read(REG_DATA));
    }

    @Test
    public void unknownFunctionFails() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, 99);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_NO_SUCH_FUNCTION, read(REG_DATA));
    }

    @Test
    public void reservedFunctionCodeFaults() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, IOCallback.RESERVED_CODE);

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_NO_SUCH_FUNCTION, read(REG_DATA));
    }

    @Test
    public void executeWithoutFunctionFails() {
        write(REG_SELECT, 0);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_NO_SUCH_FUNCTION, read(REG_DATA));
    }

    @Test
    public void deviceRejectingArgumentsReportsInvalidArguments() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_THROWS_ILLEGAL_ARGUMENT);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_INVALID_ARGUMENTS, read(REG_DATA));
    }

    @Test
    public void deviceFailingInternallyIsNotReportedAsBadArguments() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_THROWS_INTERNAL);
        write(REG_STATUS, CONTROL_EXECUTE);

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertNotEquals(ERROR_INVALID_ARGUMENTS, read(REG_DATA));
    }

    @Test
    public void argumentOverflowFails() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_ECHO);
        for (int i = 0; i < 257; i++) {
            write(REG_DATA, 0);
        }

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_INVALID_ARGUMENTS, read(REG_DATA));
    }

    @Test
    public void resetClearsPendingTransaction() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(STATUS_BUSY, read(REG_STATUS));

        adapter.reset();

        assertEquals(0, read(REG_STATUS), "a reset machine must not poll a busy bit forever");
    }

    @Test
    public void pendingSynchronizedCallSurvivesSaveAndLoad() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);
        assertEquals(STATUS_BUSY, read(REG_STATUS));

        final CompoundTag tag = assertDoesNotThrow(() -> NBTSerialization.serialize(adapter));

        final IODeviceBusAdapter restored = new IODeviceBusAdapter(amount -> true);
        assertDoesNotThrow(() -> NBTSerialization.deserialize(tag, restored));
        restored.rebuild(controller);

        assertEquals(STATUS_BUSY, (int) restored.load(REG_STATUS, Sizes.SIZE_8_LOG2));

        restored.tick();

        assertEquals(STATUS_DATA_AVAILABLE, (int) restored.load(REG_STATUS, Sizes.SIZE_8_LOG2));
        assertEquals(0x5A, (int) restored.load(REG_DATA, Sizes.SIZE_8_LOG2));
    }

    @Test
    public void pendingCallForVanishedDeviceFailsCleanly() {
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);

        setDevices();
        adapter.rebuild(controller);
        adapter.tick();

        assertEquals(STATUS_ERROR, read(REG_STATUS));
        assertEquals(ERROR_NO_SUCH_DEVICE, read(REG_DATA));
    }

    @Test
    public void eventsAreDroppedUntilTheGuestOptsIn() {
        assertFalse(target.context.sendEvent(1, 2));
        assertEquals(0, read(REG_EVENT_CONTROL));
        assertEquals(NO_EVENT, read(REG_EVENT_DATA));
    }

    @Test
    public void eventRecordRoundTrips() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        assertTrue(target.context.sendEvent(0x12, 0xABCD));

        assertEquals(EVENT_QUEUE | EVENT_PENDING, read(REG_EVENT_CONTROL));
        assertEquals(0, read(REG_EVENT_DATA), "the first byte is the device index");
        assertEquals(0x12, read(REG_EVENT_DATA));
        assertEquals(0xCD, read(REG_EVENT_DATA));
        assertEquals(0xAB, read(REG_EVENT_DATA));
        assertEquals(EVENT_QUEUE, read(REG_EVENT_CONTROL), "reading the last byte removes the record");
        assertEquals(NO_EVENT, read(REG_EVENT_DATA));
    }

    @Test
    public void eventsAreDeliveredInOrder() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        target.context.sendEvent(1, 0);
        target.context.sendEvent(2, 0);

        assertArrayEquals(new int[]{0, 1, 0, 0}, readEvent());
        assertArrayEquals(new int[]{0, 2, 0, 0}, readEvent());
    }

    @Test
    public void interruptIsRaisedWhileEventsArePending() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        target.context.sendEvent(1, 0);
        adapter.step();
        assertEquals(0, interrupts.raised, "without the interrupt bit events are only queued");

        write(REG_EVENT_CONTROL, EVENT_QUEUE | EVENT_INTERRUPT);
        assertEquals(1 << INTERRUPT, interrupts.raised, "enabling with an event pending raises the line");

        readEvent();
        assertEquals(0, interrupts.raised, "draining the queue lowers the line");

        target.context.sendEvent(1, 0);
        assertEquals(0, interrupts.raised, "the line is raised on the worker thread");
        adapter.step();
        assertEquals(1 << INTERRUPT, interrupts.raised);

        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        assertEquals(0, interrupts.raised, "disabling the interrupt lowers the line");
    }

    @Test
    public void overflowIsReportedUntilControlIsWritten() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        int accepted = 0;
        while (target.context.sendEvent(1, accepted)) {
            accepted++;
        }

        assertEquals(16, accepted);
        assertEquals(EVENT_QUEUE | EVENT_OVERFLOW | EVENT_PENDING, read(REG_EVENT_CONTROL));

        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        assertEquals(EVENT_QUEUE | EVENT_PENDING, read(REG_EVENT_CONTROL), "writing control clears overflow, not the queue");
        assertArrayEquals(new int[]{0, 1, 0, 0}, readEvent(), "the oldest events are kept");
    }

    @Test
    public void disablingTheQueueDiscardsEvents() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        target.context.sendEvent(1, 0);
        write(REG_EVENT_CONTROL, 0);
        write(REG_EVENT_CONTROL, EVENT_QUEUE);

        assertEquals(EVENT_QUEUE, read(REG_EVENT_CONTROL));
    }

    @Test
    public void eventRegistersDoNotDisturbABusyTransaction() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE | EVENT_INTERRUPT);
        write(REG_SELECT, 0);
        write(REG_FUNCTION, FUNCTION_SYNCHRONIZED);
        write(REG_STATUS, CONTROL_EXECUTE);

        target.context.sendEvent(1, 0);
        assertArrayEquals(new int[]{0, 1, 0, 0}, readEvent());

        adapter.tick();

        assertEquals(STATUS_DATA_AVAILABLE, read(REG_STATUS));
        assertEquals(0x5A, read(REG_DATA));
    }

    @Test
    public void eventIndexFollowsARescan() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        target.context.sendEvent(1, 0);

        final ObjectDevice other = new ObjectDevice(new OtherTarget(), "other");
        when(controller.getDevices()).thenReturn(Set.of(subject, other));
        when(controller.getDeviceIdentifiers(subject)).thenReturn(Set.of(DEVICE_UUID));
        when(controller.getDeviceIdentifiers(other)).thenReturn(Set.of(LOWER_UUID));
        adapter.rebuild(controller);

        assertArrayEquals(new int[]{1, 1, 0, 0}, readEvent(), "the event must name the device's current index");
    }

    @Test
    public void removedDeviceIsUnmountedAndItsEventsDropped() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE | EVENT_INTERRUPT);
        final IOBusContext context = target.context;
        context.sendEvent(1, 0);
        adapter.step();

        setDevices();
        adapter.rebuild(controller);

        assertNull(target.context, "the device should have been unmounted");
        assertEquals(EVENT_QUEUE | EVENT_INTERRUPT, read(REG_EVENT_CONTROL));
        assertEquals(0, interrupts.raised);
        assertFalse(context.sendEvent(1, 0), "a stale context must not queue events");
    }

    @Test
    public void partiallyReadEventSurvivesItsDeviceLeaving() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        target.context.sendEvent(1, 0x0302);
        target.context.sendEvent(2, 0);
        assertEquals(0, read(REG_EVENT_DATA));

        setDevices();
        adapter.rebuild(controller);

        assertEquals(1, read(REG_EVENT_DATA), "the rest of the started event must stay aligned");
        assertEquals(0x02, read(REG_EVENT_DATA));
        assertEquals(0x03, read(REG_EVENT_DATA));
        assertEquals(EVENT_QUEUE, read(REG_EVENT_CONTROL), "the unstarted event of the gone device is dropped");
    }

    @Test
    public void unmountInvalidatesContexts() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE);
        final IOBusContext context = target.context;

        adapter.unmountDevices();

        assertNull(target.context);
        assertFalse(context.sendEvent(1, 0));

        adapter.mountDevices();
        assertTrue(target.context.sendEvent(1, 0));
    }

    @Test
    public void eventOutOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> target.context.sendEvent(0x100, 0));
        assertThrows(IllegalArgumentException.class, () -> target.context.sendEvent(0, 0x10000));
    }

    @Test
    public void resetClearsEvents() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE | EVENT_INTERRUPT);
        target.context.sendEvent(1, 0);
        adapter.step();

        adapter.reset();

        assertEquals(0, read(REG_EVENT_CONTROL));
        assertEquals(0, interrupts.raised);
        assertFalse(target.context.sendEvent(1, 0), "a reset machine has not opted in");
    }

    @Test
    public void pendingEventsSurviveSaveAndLoad() {
        write(REG_EVENT_CONTROL, EVENT_QUEUE | EVENT_INTERRUPT);
        target.context.sendEvent(3, 0x0102);
        read(REG_EVENT_DATA);

        final CompoundTag tag = assertDoesNotThrow(() -> NBTSerialization.serialize(adapter));

        final IODeviceBusAdapter restored = new IODeviceBusAdapter(amount -> true);
        assertDoesNotThrow(() -> NBTSerialization.deserialize(tag, restored));
        restored.rebuild(controller);

        assertEquals(EVENT_QUEUE | EVENT_INTERRUPT | EVENT_PENDING, (int) restored.load(REG_EVENT_CONTROL, Sizes.SIZE_8_LOG2));
        assertEquals(3, (int) restored.load(REG_EVENT_DATA, Sizes.SIZE_8_LOG2), "the partially read record continues");
        assertEquals(0x02, (int) restored.load(REG_EVENT_DATA, Sizes.SIZE_8_LOG2));
        assertEquals(0x01, (int) restored.load(REG_EVENT_DATA, Sizes.SIZE_8_LOG2));
        assertEquals(EVENT_QUEUE | EVENT_INTERRUPT, (int) restored.load(REG_EVENT_CONTROL, Sizes.SIZE_8_LOG2));
    }

    // --------------------------------------------------------------------- //

    private boolean consumeEnergy(final int amount) {
        if (amount > energy) {
            return false;
        }
        energy -= amount;
        return true;
    }

    private void setDevices(final Device... devices) {
        when(controller.getDevices()).thenReturn(Set.of(devices));
        for (final Device device : devices) {
            when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(DEVICE_UUID));
        }
    }

    private void write(final int register, final int value) {
        adapter.store(register, value, Sizes.SIZE_8_LOG2);
    }

    private int read(final int register) {
        return (int) adapter.load(register, Sizes.SIZE_8_LOG2);
    }

    private int[] readEvent() {
        return new int[]{read(REG_EVENT_DATA), read(REG_EVENT_DATA), read(REG_EVENT_DATA), read(REG_EVENT_DATA)};
    }

    // --------------------------------------------------------------------- //

    @IODeviceDescription(name = "TEST")
    public static final class TestTarget implements LifecycleAwareDevice {
        public int synchronizedCalls;
        public int pricedCalls;
        public IOBusContext context;

        @Override
        public void onIODeviceMounted(final IOBusContext context) {
            this.context = context;
        }

        @Override
        public void onIODeviceUnmounted(final IOBusContext context) {
            this.context = null;
        }

        @IOCallback(value = FUNCTION_ECHO, synchronize = false)
        public void echo(final InputStream arguments, final OutputStream results) throws Exception {
            results.write(arguments.read() + 1);
        }

        @IOCallback(FUNCTION_SYNCHRONIZED)
        public void synchronizedCall(final OutputStream results) throws Exception {
            synchronizedCalls++;
            results.write(0x5A);
        }

        @IOCallback(value = FUNCTION_THROWS_ILLEGAL_ARGUMENT, synchronize = false)
        public void rejectsArguments() {
            throw new IllegalArgumentException("nope");
        }

        @IOCallback(value = FUNCTION_THROWS_INTERNAL, synchronize = false)
        public void failsInternally() {
            throw new UnsupportedOperationException("boom");
        }

        @IOCallback(value = FUNCTION_PRICED, energy = 2)
        public void priced(final OutputStream results) throws Exception {
            pricedCalls++;
            results.write(1);
        }
    }

    @IODeviceDescription(name = "OTHER")
    public static final class OtherTarget {
        @IOCallback(value = FUNCTION_ECHO, synchronize = false)
        public void echo(final InputStream arguments, final OutputStream results) throws Exception {
            results.write(arguments.read() + 100);
        }
    }

    private static final class TestInterruptController implements InterruptController {
        public int raised;

        @Override
        public void raiseInterrupts(final int mask) {
            raised |= mask;
        }

        @Override
        public void lowerInterrupts(final int mask) {
            raised &= ~mask;
        }

        @Override
        public int getRaisedInterrupts() {
            return raised;
        }
    }
}
