/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.item.NetworkTunnelItem;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public final class NetworkTunnelDevice extends AbstractNetworkInterfaceDevice {
    public NetworkTunnelDevice(final ItemStack identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        return null;
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        final VMDeviceLoadResult result = super.mount(context);
        if (result.wasSuccessful()) {
            NetworkTunnelItem.getTunnelId(identity).ifPresent(id ->
                TunnelManager.registerEndpoint(id, getNetworkInterface()));
        }
        return result;
    }

    @Override
    public void unmount() {
        super.unmount();
        TunnelManager.unregisterEndpoint(getNetworkInterface());
    }

    ///////////////////////////////////////////////////////////////

    public static final class TunnelManager {
        private static final int BYTES_PER_TICK = 32 * 1024 / TickUtils.toTicks(Duration.ofSeconds(1)); // bytes / sec -> bytes / tick
        private static final int MIN_ETHERNET_FRAME_SIZE = 42;

        private static final BiMap<UUID, Set<NetworkInterface>> TUNNELS = HashBiMap.create();

        public static void registerEndpoint(final UUID id, final NetworkInterface networkInterface) {
            TUNNELS.computeIfAbsent(id, unused -> new HashSet<>())
                .add(networkInterface);
        }

        public static void unregisterEndpoint(final NetworkInterface networkInterface) {
            for (final Set<NetworkInterface> tunnel : TUNNELS.values()) {
                tunnel.remove(networkInterface);
            }
        }

        public static void initialize() {
            TickEvent.SERVER_PRE.register(server -> pumpMessages());
            LifecycleEvent.SERVER_STOPPED.register(server -> TUNNELS.clear());
        }

        private static void pumpMessages() {
            final Iterator<Set<NetworkInterface>> iterator = TUNNELS.values().iterator();
            while (iterator.hasNext()) {
                final Set<NetworkInterface> tunnel = iterator.next();
                if (tunnel.isEmpty()) {
                    iterator.remove();
                } else {
                    pumpMessages(tunnel);
                }
            }
        }

        private static void pumpMessages(final Collection<NetworkInterface> tunnel) {
            for (final NetworkInterface source : tunnel) {
                int byteBudget = BYTES_PER_TICK;
                byte[] frame;
                while ((frame = source.readEthernetFrame()) != null && byteBudget > 0) {
                    byteBudget -= Math.max(frame.length, MIN_ETHERNET_FRAME_SIZE); // Avoid bogus packets messing with us.
                    for (final NetworkInterface destination : tunnel) {
                        if (destination != source) {
                            destination.writeEthernetFrame(source, frame, 1);
                        }
                    }
                }
            }
        }
    }
}
