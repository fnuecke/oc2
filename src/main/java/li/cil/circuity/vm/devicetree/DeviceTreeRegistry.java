package li.cil.circuity.vm.devicetree;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.devicetree.DeviceTree;
import li.cil.circuity.api.vm.devicetree.DeviceTreeProvider;
import li.cil.circuity.vm.device.UART16550A;
import li.cil.circuity.vm.devicetree.provider.*;
import li.cil.circuity.vm.riscv.device.R5CoreLocalInterrupter;
import li.cil.circuity.vm.riscv.device.R5HostTargetInterface;
import li.cil.circuity.vm.riscv.device.R5PlatformLevelInterruptController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public final class DeviceTreeRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Class<? extends Device>, DeviceTreeProvider> providers = new HashMap<>();
    private static final Map<Class<? extends Device>, DeviceTreeProvider> providerCache = new HashMap<>();

    static {
        addProvider(MemoryMappedDevice.class, MemoryMappedDeviceProvider.INSTANCE);
        addProvider(InterruptSource.class, InterruptSourceProvider.INSTANCE);
        addProvider(PhysicalMemory.class, PhysicalMemoryProvider.INSTANCE);
        addProvider(R5HostTargetInterface.class, HostTargetInterfaceProvider.INSTANCE);
        addProvider(R5PlatformLevelInterruptController.class, PlatformLevelInterruptControllerProvider.INSTANCE);
        addProvider(R5CoreLocalInterrupter.class, CoreLocalInterrupterProvider.INSTANCE);
        addProvider(UART16550A.class, UART16550AProvider.INSTANCE);
        // VirtIOProvider.INSTANCE
    }

    public static void addProvider(final Class<? extends Device> clazz, final DeviceTreeProvider provider) {
        providers.put(clazz, provider);
        providerCache.clear();
    }

    private static void visitBaseTypes(@Nullable final Class<?> clazz, final Consumer<Class<?>> visitor) {
        if (clazz == null) {
            return;
        }

        visitor.accept(clazz);
        visitBaseTypes(clazz.getSuperclass(), visitor);

        final Class<?>[] interfaces = clazz.getInterfaces();
        for (final Class<?> iface : interfaces) {
            visitor.accept(iface);
            visitBaseTypes(iface, visitor);
        }
    }

    @Nullable
    public static DeviceTreeProvider getProvider(final Device device) {
        final Class<? extends Device> deviceClass = device.getClass();
        if (providerCache.containsKey(deviceClass)) {
            return providerCache.get(deviceClass);
        }

        final List<DeviceTreeProvider> relevant = new ArrayList<>();
        final Set<Class<?>> seen = new HashSet<>();
        visitBaseTypes(deviceClass, c -> {
            if (seen.add(c) && providers.containsKey(c)) {
                relevant.add(providers.get(c));
            }
        });

        if (relevant.size() == 0) {
            return null;
        }

        if (relevant.size() == 1) {
            return relevant.get(0);
        }

        // Flip things around so when iterating in visit() we go from least to most specific provider.
        Collections.reverse(relevant);

        return new DeviceTreeProvider() {
            @Override
            public Optional<String> getName(final Device device) {
                for (int i = relevant.size() - 1; i >= 0; i--) {
                    final Optional<String> name = relevant.get(i).getName(device);
                    if (name.isPresent()) {
                        return name;
                    }
                }

                return Optional.empty();
            }

            @Override
            public Optional<DeviceTree> createNode(final DeviceTree root, final MemoryMap memoryMap, final Device device, final String deviceName) {
                for (int i = relevant.size() - 1; i >= 0; i--) {
                    final Optional<DeviceTree> node = relevant.get(i).createNode(root, memoryMap, device, deviceName);
                    if (node.isPresent()) {
                        return node;
                    }
                }

                return Optional.empty();
            }

            @Override
            public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
                for (final DeviceTreeProvider provider : relevant) {
                    provider.visit(node, memoryMap, device);
                }
            }
        };
    }

    public static void visit(final DeviceTree root, final MemoryMap memoryMap, final Device device) {
        final DeviceTreeProvider provider = getProvider(device);
        if (provider == null) {
            LOGGER.warn("No provider for device [{}].", device);
            return;
        }

        final Optional<String> name = provider.getName(device);
        if (!name.isPresent()) {
            LOGGER.warn("Failed obtaining name for device [{}].", device);
            return;
        }

        final Optional<DeviceTree> node = provider.createNode(root, memoryMap, device, name.get());
        if (!node.isPresent()) {
            LOGGER.warn("Failed obtaining node for device [{}].", device);
            return;
        }

        provider.visit(node.get(), memoryMap, device);
    }

    public static DeviceTree create(final MemoryMap mmu) {
        return new DeviceTreeImpl(mmu);
    }
}
