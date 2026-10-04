/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.MinecraftBootstrap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.object.IOCallbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.oc2.common.bus.TestIOInvocation;
import li.cil.oc2.common.vm.AbstractVMItemStackHandlers;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.bus.DeviceDescription;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MinecraftBootstrap.class)
public final class SystemDeviceIOTests {
    private static final int GET_ITEM_NAME = 1;
    private static final int GET_ITEM_ID = 2;
    private static final int GET_FLUID_NAME = 3;
    private static final int GET_FLUID_ID = 4;
    private static final int GET_BLOCK_NAME = 5;
    private static final int GET_BLOCK_ID = 6;

    private static final int REG_SELECT = 0;
    private static final int REG_FUNCTION = 1;
    private static final int REG_DATA = 2;
    private static final int REG_STATUS = 3;
    private static final int STATUS_DATA_AVAILABLE = 0b0000_0010;
    private static final int CONTROL_EXECUTE = 0x01;

    // --------------------------------------------------------------------- //

    @Test
    public void deviceIsNamedSystem() {
        assertEquals("SYSTEM", new ObjectDevice(new SystemDevice()).getIOName());
    }

    @Test
    public void systemIsMidLevelDeviceZero() {
        final AbstractVMItemStackHandlers handlers = new AbstractVMItemStackHandlers() {
            @Override
            protected ItemDeviceQuery makeQuery(final ItemStack stack) {
                throw new UnsupportedOperationException();
            }
        };
        final Device system = handlers.busElement.getLocalDevices().iterator().next();
        final UUID systemId = handlers.busElement.getDeviceIdentifier(system).orElseThrow();
        final ObjectDevice low = new ObjectDevice(new SystemDevice());
        final ObjectDevice zero = new ObjectDevice(new SystemDevice());

        final DeviceBusController controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(Set.of(low, system, zero));
        when(controller.getDeviceIdentifiers(system)).thenReturn(Set.of(systemId));
        when(controller.getDeviceIdentifiers(low)).thenReturn(Set.of(new UUID(Long.MIN_VALUE, Long.MIN_VALUE + 1)));
        when(controller.getDeviceIdentifiers(zero)).thenReturn(Set.of(new UUID(0, 0)));

        final IODeviceBusAdapter adapter = new IODeviceBusAdapter(amount -> true);
        adapter.rebuild(controller);

        final DeviceDescription first = adapter.getDescriptions().getFirst();
        assertEquals(0, first.attributes());
        assertEquals(systemId.toString(), first.id());
    }

    @Test
    public void lookupsRunOffTheServerThread() {
        for (final IOMethod method : IOCallbacks.collectMethods(new SystemDevice())) {
            assertFalse(method.isSynchronized(), "a lookup should not cost a tick");
        }
    }

    @Test
    public void itemNameResolvesFromId() throws Throwable {
        assertEquals("minecraft:redstone", name(GET_ITEM_NAME, BuiltInRegistries.ITEM.getId(Items.REDSTONE)));
    }

    @Test
    public void itemNameRejectsIdPastTheRegistry() {
        final int id = BuiltInRegistries.ITEM.size();
        assertTrue(id <= 0xFFFF);

        assertThrows(IllegalArgumentException.class, () -> name(GET_ITEM_NAME, id));
    }

    @Test
    public void itemIdResolvesFromName() throws Throwable {
        assertEquals(BuiltInRegistries.ITEM.getId(Items.REDSTONE), id(GET_ITEM_ID, "minecraft:redstone"));
    }

    @Test
    public void itemIdAcceptsBareNameAsMinecraft() throws Throwable {
        assertEquals(id(GET_ITEM_ID, "minecraft:redstone"), id(GET_ITEM_ID, "redstone"));
    }

    @Test
    public void itemIdAcceptsATerminatedName() throws Throwable {
        assertEquals(id(GET_ITEM_ID, "minecraft:redstone"), id(GET_ITEM_ID, "minecraft:redstone\0"));
    }

    @Test
    public void itemIdRejectsUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> id(GET_ITEM_ID, "minecraft:definitely_not_an_item"));
    }

    @Test
    public void itemIdRejectsMalformedName() {
        assertThrows(IllegalArgumentException.class, () -> id(GET_ITEM_ID, "NOT A RESOURCE LOCATION"));
    }

    @Test
    public void fluidLookupsRoundTrip() throws Throwable {
        final int id = id(GET_FLUID_ID, "water");
        assertEquals(BuiltInRegistries.FLUID.getId(Fluids.WATER), id);
        assertEquals("minecraft:water", name(GET_FLUID_NAME, id));
    }

    @Test
    public void fluidIdRejectsUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> id(GET_FLUID_ID, "minecraft:redstone"));
    }

    @Test
    public void blockLookupsRoundTrip() throws Throwable {
        final int id = id(GET_BLOCK_ID, "stone");
        assertEquals(BuiltInRegistries.BLOCK.getId(Blocks.STONE), id);
        assertEquals("minecraft:stone", name(GET_BLOCK_NAME, id));
    }

    @Test
    public void blockNameRejectsIdPastTheRegistry() {
        assertThrows(IllegalArgumentException.class, () -> name(GET_BLOCK_NAME, BuiltInRegistries.BLOCK.size()));
    }

    @Test
    public void nameReachesGuestThroughRegisters() {
        final int id = BuiltInRegistries.ITEM.getId(Items.REDSTONE);

        final ObjectDevice device = new ObjectDevice(new SystemDevice());
        final DeviceBusController controller = mock(DeviceBusController.class);
        when(controller.getDevices()).thenReturn(Set.of(device));
        when(controller.getDeviceIdentifiers(device)).thenReturn(Set.of(UUID.randomUUID()));

        final IODeviceBusAdapter adapter = new IODeviceBusAdapter(amount -> true);
        adapter.rebuild(controller);
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

    private static String name(final int code, final int id) throws Throwable {
        return new String(invoke(code, new byte[]{(byte) id, (byte) (id >>> 8)}), StandardCharsets.US_ASCII);
    }

    private static int id(final int code, final String name) throws Throwable {
        final byte[] results = invoke(code, name.getBytes(StandardCharsets.US_ASCII));
        assertEquals(2, results.length);
        return (results[0] & 0xFF) | ((results[1] & 0xFF) << 8);
    }

    private static byte[] invoke(final int code, final byte[] arguments) throws Throwable {
        return TestIOInvocation.invoke(TestIOInvocation.method(new SystemDevice(), code), arguments);
    }
}
