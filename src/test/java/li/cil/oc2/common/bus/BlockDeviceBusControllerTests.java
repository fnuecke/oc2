/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.sedna.api.device.serial.SerialDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.OngoingStubbing;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class BlockDeviceBusControllerTests {
    public static final ResourceLocation TEST_PROVIDER_REGISTRY_NAME = new ResourceLocation(API.MOD_ID, "test");

    private static MockedStatic<Capabilities> capabilitiesMock;
    private static MockedStatic<Providers> providersMock;
    private static MockedStatic<LevelUtils> levelUtilsMock;

    private FakeLevel fakeLevel;
    private LevelAccessor level;

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @BeforeEach
    public void setupEach() {
        capabilitiesMock = mockStatic(Capabilities.class);
        registerCapability(capabilitiesMock.when(Capabilities::energyStorage));
        registerCapability(capabilitiesMock.when(Capabilities::fluidHandler));
        registerCapability(capabilitiesMock.when(Capabilities::itemHandler));
        registerCapability(capabilitiesMock.when(Capabilities::deviceBusElement));
        registerCapability(capabilitiesMock.when(Capabilities::device));
        registerCapability(capabilitiesMock.when(Capabilities::redstoneEmitter));
        registerCapability(capabilitiesMock.when(Capabilities::networkInterface));
        registerCapability(capabilitiesMock.when(Capabilities::terminalUserProvider));
        registerCapability(capabilitiesMock.when(Capabilities::robot));

        providersMock = mockStatic(Providers.class);
        final IForgeRegistry<BlockDeviceProvider> blockDeviceProviderRegistry = createBlockDeviceProviderRegistry();
        providersMock.when(Providers::blockDeviceProviderRegistry).thenReturn(blockDeviceProviderRegistry);
        final IForgeRegistry<ItemDeviceProvider> itemDeviceProviderRegistry = createItemDeviceProviderRegistry();
        providersMock.when(Providers::itemDeviceProviderRegistry).thenReturn(itemDeviceProviderRegistry);

        levelUtilsMock = mockStatic(LevelUtils.class);
        levelUtilsMock.when(() -> LevelUtils.getBlockName(any(), any())).thenReturn("test_block");

        fakeLevel = new FakeLevel();
        level = fakeLevel.getLevel();
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerCapability(final OngoingStubbing<Capability<T>> stubbing) {
        final Capability<T> capability = mock(Capability.class);
        stubbing.thenReturn(capability);
    }

    @AfterEach
    public void teardownEach() {
        capabilitiesMock.close();
        providersMock.close();
        levelUtilsMock.close();
    }

    @Test
    public void busTouchingUnloadedChunkStaysIncomplete() {
        final BlockPos posAtChunkEdge = new BlockPos(0, 0, 0);
        final BlockDeviceBusController busController = new TestBusControllerBlockEntity(posAtChunkEdge).getBusController();

        busController.scan();

        assertEquals(CommonDeviceBusController.BusState.INCOMPLETE, busController.getState());
    }

    @Test
    public void busNotTouchingUnloadedChunkCompletes() {
        final BlockPos posInsideChunk = new BlockPos(8, 0, 8);
        final BlockDeviceBusController busController = new TestBusControllerBlockEntity(posInsideChunk).getBusController();

        busController.scan();

        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());
    }

    @Test
    public void busControllerIgnoresNonAccessibleBusElements() {
        final BlockPos controllerPos = new BlockPos(8, 0, 8);
        final TestBusControllerBlockEntity busController = new TestBusControllerBlockEntity(controllerPos);

        final BlockPos elementPos = controllerPos.east();
        final TestBusElementBlockEntity busElement = new TestBusElementBlockEntity(elementPos);
        busElement.setSideEnabled(Direction.WEST, false);

        busController.getBusController().scan();

        assertEquals(CommonDeviceBusController.BusState.READY, busController.getBusController().getState());

        assertFalse(busElement.getBusElement().getControllers().contains(busController.getBusController()));
        assertFalse(busController.getBusController().getElements().contains(busElement.getBusElement()));
    }

    @Test
    public void busControllerDetectsBusElements() {
        final BlockPos controllerPos = new BlockPos(8, 0, 8);
        final BlockDeviceBusController busController = new TestBusControllerBlockEntity(controllerPos).getBusController();

        final BlockPos elementPos = controllerPos.east();
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);
        final DeviceBusElement busElement = busElementInfo.getBusElement();

        busController.scan();

        assertTrue(busElement.getControllers().contains(busController));
        assertTrue(busController.getElements().contains(busElement));
    }

    @Test
    public void devicesInUnloadedChunksAreMarkedAsUnloaded() {
        final BlockPos elementPos = new BlockPos(0, 0, 0);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);
        final TestBlockDeviceBusElement busElement = busElementInfo.getBusElement();

        busElement.updateDevicesForNeighbor(Direction.WEST);
        verify(busElement, atLeastOnce()).setEntriesForGroupUnloaded(Direction.WEST.get3DDataValue());
    }

    @Test
    public void unloadedDeviceIsRemovedFromElement() {
        final BlockPos elementPos = new BlockPos(0, 0, 8);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);

        final BlockPos devicePos = elementPos.west();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);
        assertTrue(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));

        fakeLevel.setChunkLoaded(new ChunkPos(devicePos), false);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);
        assertFalse(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));
    }

    @Test
    public void devicesInLoadedChunksAreCollected() {
        final BlockPos elementPos = new BlockPos(0, 0, 0);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);
        final TestBlockDeviceBusElement busElement = busElementInfo.getBusElement();

        final BlockPos devicePos = elementPos.east();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        busElement.updateDevicesForNeighbor(Direction.EAST);
        verify(busElement, atLeastOnce()).setEntriesForGroup(eq(Direction.EAST.get3DDataValue()), any());
        assertTrue(busElement.getDevices().contains(deviceBlockEntity.getObjectDevice()));
    }

    @Test
    public void equalDevicesAreIgnored() {
        final BlockPos elementPos = new BlockPos(0, 0, 0);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);
        final TestBlockDeviceBusElement busElement = busElementInfo.getBusElement();

        final BlockPos devicePos = elementPos.east();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        busElement.updateDevicesForNeighbor(Direction.EAST);

        assertTrue(busElement.getDevices().contains(deviceBlockEntity.getObjectDevice()));

        final ObjectDevice equalDevice = new ObjectDevice(deviceBlockEntity.getTestDevice());
        deviceBlockEntity.setObjectDevice(equalDevice);

        busElement.updateDevicesForNeighbor(Direction.EAST);

        assertTrue(busElement.getDevices().contains(deviceBlockEntity.getObjectDevice()));
        assertNotSame(busElement.getDevices().stream().findFirst().orElseThrow(), equalDevice);
    }

    @Test
    public void busControllerDetectsDevices() {
        final BlockPos controllerPos = new BlockPos(8, 0, 8);
        final BlockDeviceBusController busController = new TestBusControllerBlockEntity(controllerPos).getBusController();

        final BlockPos elementPos = controllerPos.east();
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);
        when(busElementInfo.getBlockEntity().getCapability(eq(Capabilities.deviceBusElement()), any())).thenReturn(LazyOptional.of(busElementInfo::getBusElement));

        final BlockPos devicePos = elementPos.east();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.EAST);
        busController.scan();

        assertTrue(busController.getDevices().contains(deviceBlockEntity.getObjectDevice()));
    }

    @Test
    public void devicesGetSerializedWhenUnloadedAndDeserializedWhenLoaded() {
        final BlockPos controllerPos = new BlockPos(1, 0, 8);
        final BlockDeviceBusController busController = new TestBusControllerBlockEntity(controllerPos).getBusController();

        final BlockPos elementPos = controllerPos.west();
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);
        when(busElementInfo.getBlockEntity().getCapability(eq(Capabilities.deviceBusElement()), any())).thenReturn(LazyOptional.of(busElementInfo::getBusElement));

        final BlockPos devicePos = elementPos.west();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        fakeLevel.setChunkLoaded(new ChunkPos(devicePos), false);
        busController.scheduleBusScan();

        final RPCDeviceBusAdapter rpcDeviceBusAdapter = new RPCDeviceBusAdapter(mock(SerialDevice.class));
        busController.onBeforeDeviceScan.add(rpcDeviceBusAdapter::pause);
        busController.onAfterDeviceScan.add(event -> rpcDeviceBusAdapter.resume(busController, event.didDevicesChange()));

        busController.scan();

        // Reminder: missing chunk -> bus scan cannot complete.
        assertEquals(CommonDeviceBusController.BusState.INCOMPLETE, busController.getState());

        final ObjectDevice objectDevice = spy(deviceBlockEntity.getObjectDevice());
        deviceBlockEntity.setObjectDevice(objectDevice);

        // Initialize with unloaded chunk.
        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        assertFalse(busController.getDevices().contains(objectDevice));

        verify(objectDevice, never()).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, never()).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());

        // Load device chunk.
        fakeLevel.setChunkLoaded(new ChunkPos(devicePos), true);
        busController.scheduleBusScan();
        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.READY, busController.getState());

        assertTrue(busController.getDevices().contains(objectDevice));

        rpcDeviceBusAdapter.mountDevices();

        verify(objectDevice, times(1)).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, never()).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any()); // no state to deserialize

        // Unload device chunk.
        fakeLevel.setChunkLoaded(new ChunkPos(devicePos), false);
        busController.scheduleBusScan();
        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        busController.scan();
        assertEquals(CommonDeviceBusController.BusState.INCOMPLETE, busController.getState());

        assertFalse(busController.getDevices().contains(objectDevice));

        verify(objectDevice, times(1)).mount();
        verify(objectDevice, times(1)).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, times(1)).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());
    }

    // Different load states and how removals effect state. Adds are uninteresting,
    // because we need a fully loaded state before anything happens here.

    // Loaded: [ ] Controller, [ ] Element, [ ] Device
    //  -> No interaction possible.

    // Loaded: [ ] Controller, [ ] Element, [x] Device
    //  -> Removing Device:
    //      -> Provider#dispose() when Element is loaded.

    @Test
    public void providerDisposeIsCalledWhenDeviceIsRemovedWhileElementIsUnloaded() {
        final BlockPos elementPos = new BlockPos(0, 0, 8);
        TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);

        final BlockPos devicePos = elementPos.west();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        final ObjectDevice objectDevice = spy(deviceBlockEntity.getObjectDevice());
        deviceBlockEntity.setObjectDevice(objectDevice);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        assertTrue(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));
        verify(objectDevice, never()).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, never()).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());

        final CompoundTag data = busElementInfo.getBusElement().save();
        verify(objectDevice, times(1)).serializeNBT();

        fakeLevel.setChunkLoaded(new ChunkPos(elementPos), false);
        fakeLevel.removeBlockEntity(elementPos);

        fakeLevel.removeBlockEntity(devicePos);

        fakeLevel.setChunkLoaded(new ChunkPos(elementPos), true);
        busElementInfo = new TestBusElementBlockEntity(elementPos);
        busElementInfo.getBusElement().load(data);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        final BlockDeviceProvider provider = Providers.blockDeviceProviderRegistry().getValue(TEST_PROVIDER_REGISTRY_NAME);
        verify(provider, times(1)).unmount(any(), any());
    }

    // Loaded: [ ] Controller, [x] Element, [ ] Device
    //  -> Removing Element:
    //      -> Provider#dispose()

    @Test
    public void providerDisposeIsCalledWhenElementIsRemovedWhileDeviceIsUnloaded() {
        final BlockPos elementPos = new BlockPos(0, 0, 8);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);

        final BlockPos devicePos = elementPos.west();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        final ObjectDevice objectDevice = spy(deviceBlockEntity.getObjectDevice());
        deviceBlockEntity.setObjectDevice(objectDevice);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        assertTrue(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));
        verify(objectDevice, never()).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, never()).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());

        fakeLevel.setChunkLoaded(new ChunkPos(devicePos), false);
        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);
        assertFalse(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));
        verify(objectDevice, never()).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, times(1)).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());

        fakeLevel.removeBlockEntity(elementPos);
        busElementInfo.getBusElement().setRemoved();

        final BlockDeviceProvider provider = Providers.blockDeviceProviderRegistry().getValue(TEST_PROVIDER_REGISTRY_NAME);
        verify(provider, times(1)).unmount(any(), any());
    }

    // Loaded: [ ] Controller, [x] Element, [x] Device
    //  -> Removing Element:
    //      -> Device#dispose()

    @Test
    public void deviceIsDisposedWhenElementIsRemoved() {
        final BlockPos elementPos = new BlockPos(8, 0, 8);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);

        final BlockPos devicePos = elementPos.west();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        final ObjectDevice objectDevice = spy(deviceBlockEntity.getObjectDevice());
        deviceBlockEntity.setObjectDevice(objectDevice);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        assertTrue(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));
        verify(objectDevice, never()).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, never()).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());

        fakeLevel.removeBlockEntity(elementPos);
        busElementInfo.getBusElement().setRemoved();

        verify(objectDevice, times(1)).dispose();

        final BlockDeviceProvider provider = Providers.blockDeviceProviderRegistry().getValue(TEST_PROVIDER_REGISTRY_NAME);
        verify(provider, never()).unmount(any(), any());
    }

    //  -> Removing Device:
    //      -> Device#dispose()

    @Test
    public void deviceIsDisposedWhenDeviceIsRemoved() {
        final BlockPos elementPos = new BlockPos(8, 0, 8);
        final TestBusElementBlockEntity busElementInfo = new TestBusElementBlockEntity(elementPos);

        final BlockPos devicePos = elementPos.west();
        final TestDeviceBlockEntity deviceBlockEntity = new TestDeviceBlockEntity(devicePos);

        final ObjectDevice objectDevice = spy(deviceBlockEntity.getObjectDevice());
        deviceBlockEntity.setObjectDevice(objectDevice);

        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        assertTrue(busElementInfo.getBusElement().getDevices().contains(deviceBlockEntity.getObjectDevice()));
        verify(objectDevice, never()).mount();
        verify(objectDevice, never()).unmount();
        verify(objectDevice, never()).dispose();
        verify(objectDevice, never()).serializeNBT();
        verify(objectDevice, never()).deserializeNBT(any());

        fakeLevel.removeBlockEntity(devicePos);
        busElementInfo.getBusElement().updateDevicesForNeighbor(Direction.WEST);

        verify(objectDevice, times(1)).dispose();

        final BlockDeviceProvider provider = Providers.blockDeviceProviderRegistry().getValue(TEST_PROVIDER_REGISTRY_NAME);
        verify(provider, never()).unmount(any(), any());
    }

    // Loaded: [x] Controller, [ ] Element, [ ] Device
    //  -> Removing Controller:
    //      -> Edge-case: suspended Devices will *not* be disposed. If a new controller
    //         picks them up, they will resume under the assumption that they're managed
    //         by the same controller as before their previous unmount.

    // Loaded: [x] Controller, [ ] Element, [x] Device
    //  -> Removing Controller:
    //      -> Edge-case: suspended Devices will *not* be disposed. If a new controller
    //         picks them up, they will resume under the assumption that they're managed
    //         by the same controller as before their previous unmount.
    //  -> Removing Device:
    //      -> Provider#dispose() when Element is loaded.

    // Same as providerDisposeIsCalledWhenDeviceIsRemovedWhileElementIsUnloaded()

    // Loaded: [x] Controller, [x] Element, [ ] Device
    //  -> Removing Controller:
    //      -> Edge-case: suspended Devices will *not* be disposed. If a new controller
    //         picks them up, they will resume under the assumption that they're managed
    //         by the same controller as before their previous unmount.
    //  -> Removing Element:
    //      -> Provider#dispose()

    // Same as providerDisposeIsCalledWhenElementIsRemovedWhileDeviceIsUnloaded()

    // Loaded: [x] Controller, [x] Element, [x] Device
    //  -> Removing Controller:
    //      -> Stop VM if running.
    //  -> Stopping VM:
    //      -> Device#unmount(), Device#dispose()

    // Handled in Computer/Robot, too much pain to try to mock this.

    //  -> Removing Element:
    //      -> Device#unmount() if mounted, Device#dispose()
    //  -> Removing Device:
    //      -> Device#unmount() if mounted, Device#dispose()
    //  -> Unloading Controller, Element or Device:
    //      -> Device#unmount() if mounted.

    // TODO

    // Last case (all loaded) is the only case where the bus can be complete, so
    // also the only case where Devices can possibly be mounted.

    // In all but the last case (all loaded) it makes no difference if there's more
    // loaded/unloaded elements in the chain. However, in the last case it does:
    //  -> Removing intermediate element:
    //      -> Edge-case: suspended Devices will *not* be disposed. If a new controller
    //         picks them up, they will resume under the assumption that they're managed
    //         by the same controller as before their previous unmount.


    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    private static IForgeRegistry<BlockDeviceProvider> createBlockDeviceProviderRegistry() {
        final IForgeRegistry<BlockDeviceProvider> registry = mock(IForgeRegistry.class);

        final Map<ResourceLocation, BlockDeviceProvider> blockDeviceProviders = new HashMap<>();
        blockDeviceProviders.put(TEST_PROVIDER_REGISTRY_NAME, spy(new TestBlockDeviceProvider()));

        when(registry.getValues()).thenReturn(blockDeviceProviders.values());
        when(registry.getValue(notNull())).then(a -> blockDeviceProviders.get(a.<ResourceLocation>getArgument(0)));

        return registry;
    }

    @SuppressWarnings("unchecked")
    private static IForgeRegistry<ItemDeviceProvider> createItemDeviceProviderRegistry() {
        final IForgeRegistry<ItemDeviceProvider> registry = mock(IForgeRegistry.class);

        final Map<ResourceLocation, ItemDeviceProvider> itemDeviceProviders = new HashMap<>();
        itemDeviceProviders.put(TEST_PROVIDER_REGISTRY_NAME, spy(new TestItemDeviceProvider()));

        when(registry.getValues()).thenReturn(itemDeviceProviders.values());
        when(registry.getValue(notNull())).then(a -> itemDeviceProviders.get(a.<ResourceLocation>getArgument(0)));

        return registry;
    }

    ///////////////////////////////////////////////////////////////////

    private static final class FakeLevel {
        private final LevelAccessor level = mock(LevelAccessor.class);
        private final HashMap<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        private final HashSet<ChunkPos> loadedChunks = new HashSet<>();

        public FakeLevel() {
            when(level.getBlockEntity(any())).then(a -> blockEntities.get(a.<BlockPos>getArgument(0)));

            when(level.hasChunk(anyInt(), anyInt())).then(a -> {
                final int chunkX = a.getArgument(0);
                final int chunkZ = a.getArgument(1);
                return loadedChunks.contains(new ChunkPos(chunkX, chunkZ));
            });
        }

        public LevelAccessor getLevel() {
            return level;
        }

        public void addBlockEntity(final BlockEntity blockEntity) {
            blockEntities.put(blockEntity.getBlockPos(), blockEntity);
        }

        public void removeBlockEntity(final BlockPos pos) {
            blockEntities.remove(pos);
        }

        public void setChunkLoaded(final ChunkPos chunkPos, final boolean loaded) {
            if (loaded) {
                loadedChunks.add(chunkPos);
            } else {
                loadedChunks.remove(chunkPos);
            }
        }
    }

    private class TestBlockEntity {
        private final BlockEntity blockEntity;

        public TestBlockEntity(final BlockPos pos) {
            blockEntity = mock(BlockEntity.class);
            when(blockEntity.getBlockPos()).thenReturn(pos);
            when(blockEntity.getCapability(any(), any())).thenReturn(LazyOptional.empty());
            when(blockEntity.getCapability(any())).thenCallRealMethod();

            fakeLevel.addBlockEntity(blockEntity);
            fakeLevel.setChunkLoaded(new ChunkPos(pos), true);
        }

        public BlockEntity getBlockEntity() {
            return blockEntity;
        }
    }

    private class TestBusElementBlockEntity extends TestBlockEntity {
        private final TestBlockDeviceBusElement busElement;
        private final boolean[] enabledSides = new boolean[Constants.BLOCK_FACE_COUNT];

        public TestBusElementBlockEntity(final BlockPos pos) {
            super(pos);
            busElement = spy(new TestBlockDeviceBusElement(level, pos));
            when(getBlockEntity().getCapability(eq(Capabilities.deviceBusElement()), any())).then(a -> {
                if (enabledSides[a.<Direction>getArgument(1).get3DDataValue()]) {
                    return LazyOptional.of(() -> busElement);
                } else {
                    return LazyOptional.empty();
                }
            });
            Arrays.fill(enabledSides, true);
        }

        public TestBlockDeviceBusElement getBusElement() {
            return busElement;
        }

        public void setSideEnabled(final Direction side, final boolean value) {
            enabledSides[side.get3DDataValue()] = value;
        }
    }

    private class TestBusControllerBlockEntity extends TestBusElementBlockEntity {
        private final BlockDeviceBusController busController;

        public TestBusControllerBlockEntity(final BlockPos pos) {
            super(pos);
            busController = new BlockDeviceBusController(getBusElement(), 0, getBlockEntity());
        }

        public BlockDeviceBusController getBusController() {
            return busController;
        }
    }

    private class TestDeviceBlockEntity extends TestBlockEntity {
        private final TestDevice testDevice;
        private ObjectDevice objectDevice;

        public TestDeviceBlockEntity(final BlockPos pos) {
            super(pos);
            testDevice = new TestDevice();
            objectDevice = new ObjectDevice(testDevice);
            when(getBlockEntity().getCapability(eq(Capabilities.device()), any())).thenReturn(LazyOptional.of(() -> objectDevice));
        }

        public TestDevice getTestDevice() {
            return testDevice;
        }

        public ObjectDevice getObjectDevice() {
            return objectDevice;
        }

        public void setObjectDevice(final ObjectDevice device) {
            this.objectDevice = device;
        }
    }

    private static final class TestBlockDeviceBusElement extends AbstractBlockDeviceBusElement {
        private final LevelAccessor level;
        private final BlockPos blockPos;

        public TestBlockDeviceBusElement(final LevelAccessor level, final BlockPos blockPos) {
            this.level = level;
            this.blockPos = blockPos;
        }

        @Override
        public LevelAccessor getLevel() {
            return level;
        }

        @Override
        public BlockPos getPosition() {
            return blockPos;
        }
    }

    private static class TestBlockDeviceProvider implements BlockDeviceProvider {
        @Override
        public BlockDeviceProvider setRegistryName(final ResourceLocation name) {
            return this;
        }

        @Nullable
        @Override
        public ResourceLocation getRegistryName() {
            return TEST_PROVIDER_REGISTRY_NAME;
        }

        @Override
        public Class<BlockDeviceProvider> getRegistryType() {
            return BlockDeviceProvider.class;
        }

        @Override
        public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
            final LevelAccessor level = query.getLevel();
            final BlockEntity blockEntity = level.getBlockEntity(query.getQueryPosition());
            if (blockEntity != null) {
                final Optional<Device> optional = blockEntity.getCapability(Capabilities.device()).resolve();
                return optional.map(Invalidatable::of).orElseGet(Invalidatable::empty);
            }
            return Invalidatable.empty();
        }
    }

    private static class TestItemDeviceProvider implements ItemDeviceProvider {
        @Override
        public ItemDeviceProvider setRegistryName(final ResourceLocation name) {
            return this;
        }

        @Nullable
        @Override
        public ResourceLocation getRegistryName() {
            return TEST_PROVIDER_REGISTRY_NAME;
        }

        @Override
        public Class<ItemDeviceProvider> getRegistryType() {
            return ItemDeviceProvider.class;
        }

        @Override
        public Optional<ItemDevice> getDevice(final ItemDeviceQuery query) {
            return Optional.empty();
        }
    }

    public static class TestDevice {
        @Callback
        public int test() {
            return 42;
        }
    }
}
