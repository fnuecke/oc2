package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import java.util.*;

import static java.util.Collections.emptySet;

public abstract class TileEntityDeviceBusController implements DeviceBusController {
    public enum State {
        SCAN_PENDING,
        INCOMPLETE,
        TOO_COMPLEX,
        MULTIPLE_CONTROLLERS,
        READY,
    }

    private static final int MAX_BUS_ELEMENT_COUNT = 128;
    private static final int TICKS_PER_SECOND = 20;
    private static final int INCOMPLETE_RETRY_INTERVAL = 10;
    private static final int TOO_COMPLEX_RETRY_INTERVAL = 5;

    private final TileEntity tileEntity;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final HashSet<Device> devices = new HashSet<>();
    private final HashMap<Device, Set<UUID>> deviceIds = new HashMap<>();

    private State state = State.SCAN_PENDING;
    private int scanDelay;

    protected TileEntityDeviceBusController(final TileEntity tileEntity) {
        this.tileEntity = tileEntity;
    }

    protected void onDevicesInvalid() {
    }

    protected void onDevicesValid() {
    }

    public State getState() {
        return state;
    }

    @Override
    public void scheduleBusScan() {
        onDevicesInvalid();

        for (final DeviceBusElement element : elements) {
            element.removeController(this);
        }

        elements.clear();
        devices.clear();
        deviceIds.clear();

        scanDelay = 0; // scan as soon as possible
        state = State.SCAN_PENDING;
    }

    @Override
    public void scanDevices() {
        onDevicesInvalid();

        devices.clear();
        deviceIds.clear();

        for (final DeviceBusElement element : elements) {
            for (final Device device : element.getLocalDevices()) {
                devices.add(device);
                element.getDeviceIdentifier(device).ifPresent(identifier -> deviceIds
                        .computeIfAbsent(device, unused -> new HashSet<>()).add(identifier));
            }
        }

        onDevicesValid();
    }

    @Override
    public Set<Device> getDevices() {
        return devices;
    }

    @Override
    public Set<UUID> getDeviceIdentifiers(final Device device) {
        return deviceIds.getOrDefault(device, emptySet());
    }

    public void scan() {
        if (scanDelay < 0) {
            return;
        }

        if (scanDelay-- > 0) {
            return;
        }

        assert scanDelay == -1;

        final World world = tileEntity.getWorld();
        if (world == null || world.isRemote()) {
            scanDelay = 0;
            return;
        }

        final Stack<ScanEdge> queue = new Stack<>();
        final HashSet<ScanEdge> seenEdges = new HashSet<>(); // to avoid duplicate edge scans
        final HashSet<BlockPos> busPositions = new HashSet<>(); // to track number of seen blocks for limit

        final Direction[] faces = Direction.values();
        for (final Direction face : faces) {
            final ScanEdge edgeIn = new ScanEdge(tileEntity.getPos(), face);
            queue.add(edgeIn);
            seenEdges.add(edgeIn);
        }

        // When we belong to a bus with multiple controllers we finish the scan and register
        // with all bus elements so that an element can easily trigger a scan on all connected
        // controllers -- without having to scan through the bus itself.
        boolean hasMultipleControllers = false;
        while (!queue.isEmpty()) {
            final ScanEdge edge = queue.pop();
            assert seenEdges.contains(edge);

            final ChunkPos chunkPos = new ChunkPos(edge.position);
            if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
                // If we have an unloaded chunk neighbor we cannot know whether our neighbor in that
                // chunk would cause a scan once it is loaded, so we'll just retry every so often.
                scanDelay = INCOMPLETE_RETRY_INTERVAL * TICKS_PER_SECOND;
                state = State.INCOMPLETE;
                elements.clear();
                return;
            }

            final TileEntity tileEntity = world.getTileEntity(edge.position);
            if (tileEntity == null) {
                for (final Direction face : faces) {
                    seenEdges.add(new ScanEdge(edge.position, face));
                }

                continue;
            }

            if (tileEntity.getCapability(Capabilities.DEVICE_BUS_CONTROLLER_CAPABILITY, edge.face)
                    .map(controller -> !Objects.equals(controller, this)).orElse(false)) {
                hasMultipleControllers = true;
            }

            final LazyOptional<DeviceBusElement> capability = tileEntity.getCapability(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, edge.face);
            if (capability.isPresent()) {
                if (busPositions.add(edge.position) && busPositions.size() > MAX_BUS_ELEMENT_COUNT) {
                    elements.clear();
                    scanDelay = TOO_COMPLEX_RETRY_INTERVAL * TICKS_PER_SECOND;
                    state = State.TOO_COMPLEX;
                    return; // This return is the reason this is not in the ifPresent below.
                }
            }

            capability.ifPresent(element -> {
                elements.add(element);

                for (final Direction face : faces) {
                    final LazyOptional<DeviceBusElement> otherCapability = tileEntity.getCapability(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, face);
                    otherCapability.ifPresent(otherElement -> {
                        final boolean isConnectedToIncomingEdge = Objects.equals(otherElement, element);
                        if (!isConnectedToIncomingEdge) {
                            return;
                        }

                        final ScanEdge edgeIn = new ScanEdge(edge.position, face);
                        seenEdges.add(edgeIn);

                        final ScanEdge edgeOut = new ScanEdge(edge.position.offset(face), face.getOpposite());
                        if (seenEdges.add(edgeOut)) {
                            queue.add(edgeOut);
                        }
                    });
                }
            });
        }

        for (final DeviceBusElement element : elements) {
            element.addController(this);
        }

        if (hasMultipleControllers) {
            state = State.MULTIPLE_CONTROLLERS;
            return;
        }

        scanDevices();

        state = State.READY;
    }

    private static final class ScanEdge {
        public final BlockPos position;
        public final Direction face;

        public ScanEdge(final BlockPos position, final Direction face) {
            this.position = position;
            this.face = face;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ScanEdge scanEdge = (ScanEdge) o;
            return position.equals(scanEdge.position) &&
                   face == scanEdge.face;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, face);
        }
    }
}
