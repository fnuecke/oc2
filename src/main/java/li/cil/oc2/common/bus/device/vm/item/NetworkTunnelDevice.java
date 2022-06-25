/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.item.NetworkTunnelItem;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public final class NetworkTunnelDevice extends AbstractNetworkInterfaceDevice {
    public NetworkTunnelDevice(final ItemStack identity) {
        super(identity);
    }

    ///////////////////////////////////////////////////////////////

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        return LazyOptional.empty();
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

    @Mod.EventBusSubscriber
    private static final class TunnelManager {
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

        @SubscribeEvent
        public static void handleServerTick(final TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                pumpMessages();
            }
        }

        @SubscribeEvent
        public static void handleServerStopped(final ServerStoppedEvent event) {
            TUNNELS.clear();
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
