/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOCallbacks;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.vm.context.VMRuntime;
import li.cil.oc2.common.blockentity.RedstoneInterfaceBlockEntity;
import li.cil.oc2.common.bus.device.rpc.item.RedstoneInterfaceCardItemDevice;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.sedna.api.Sizes;
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

    private static final int STATUS_BUSY = 0b0000_0001;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int STATUS_ERROR = 0b1000_0000;

    private static final int CONTROL_ABORT = 0x00;
    private static final int CONTROL_EXECUTE = 0x01;

    private static final int ERROR_NO_SUCH_DEVICE = 0x01;
    private static final int ERROR_NO_SUCH_FUNCTION = 0x02;
    private static final int ERROR_INVALID_ARGUMENTS = 0x05;

    private static final int FUNCTION_ECHO = 1;
    private static final int FUNCTION_SYNCHRONIZED = 2;
    private static final int FUNCTION_THROWS_ILLEGAL_ARGUMENT = 3;
    private static final int FUNCTION_THROWS_INTERNAL = 4;

    private static final UUID DEVICE_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID LOWER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private IODeviceBusAdapter adapter;
    private DeviceBusController controller;
    private TestTarget target;
    private ObjectDevice subject;

    @BeforeEach
    public void setupEach() {
        adapter = new IODeviceBusAdapter(mock(VMRuntime.class));
        controller = mock(DeviceBusController.class);
        target = new TestTarget();
        subject = new ObjectDevice(target, "test");
        setDevices(subject);
        adapter.rebuild(controller);
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
    public void aRescanDoesNotRetargetASelectedDevice() {
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
    public void devicesSharingAnIdentifierEnumerateOnce() {
        final ObjectDevice other = new ObjectDevice(new OtherTarget(), "other");
        when(controller.getDevices()).thenReturn(Set.of(subject, other));
        when(controller.getDeviceIdentifiers(subject)).thenReturn(Set.of(DEVICE_UUID));
        when(controller.getDeviceIdentifiers(other)).thenReturn(Set.of(DEVICE_UUID));
        adapter.rebuild(controller);

        assertEquals(1, adapter.getDescriptions().size(),
            "two devices sharing an identifier must not both claim a sub-index");
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

        final IODeviceBusAdapter restored = new IODeviceBusAdapter(mock(VMRuntime.class));
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
    public void redstoneDevicesExposeTheirFunctions() {
        for (final Class<?> type : List.of(RedstoneInterfaceBlockEntity.class, RedstoneInterfaceCardItemDevice.class)) {
            assertTrue(IOCallbacks.hasMethods(type), type.getSimpleName() + " provides no IO functions");
            assertEquals("REDSTN", IOCallbacks.getName(type));
        }
    }

    // --------------------------------------------------------------------- //

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

    // --------------------------------------------------------------------- //

    @IOName("OTHER")
    public static final class OtherTarget {
        @IOCallback(value = FUNCTION_ECHO, synchronize = false)
        public void echo(final InputStream arguments, final OutputStream results) throws Exception {
            results.write(arguments.read() + 100);
        }
    }

    @IOName("TEST")
    public static final class TestTarget {
        public int synchronizedCalls;

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
    }
}
