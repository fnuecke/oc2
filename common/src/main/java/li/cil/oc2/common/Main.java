/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import li.cil.ceres.Ceres;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import li.cil.oc2.common.bus.device.rpc.RPCItemStackTagFilters;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import li.cil.oc2.common.bus.device.vm.item.NetworkTunnelDevice;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.ItemGroup;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import li.cil.oc2.common.network.ProjectorLoadBalancer;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.oc2.common.tags.BlockTags;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.util.ServerUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.vm.Allocator;
import li.cil.oc2.common.vm.provider.DeviceTreeProviders;
import li.cil.sedna.Sedna;

public final class Main {
    public static void initialize() {
        Ceres.initialize();
        Sedna.initialize();
        DeviceTreeProviders.initialize();
        Serializers.initialize();

        ConfigManager.add(Config::new);
        ConfigManager.initialize();

        RegistryUtils.begin();

        ItemTags.initialize();
        BlockTags.initialize();
        Blocks.initialize();
        Items.initialize();
        ItemGroup.initialize();
        BlockEntities.initialize();
        Entities.initialize();
        Containers.initialize();
        RecipeSerializers.initialize();
        SoundEvents.initialize();

        ProviderRegistry.initialize();
        DeviceTypes.initialize();
        BlockDeviceDataRegistry.initialize();
        FirmwareRegistry.initialize();
        RPCTypeAdapters.initialize();

        RegistryUtils.finish();

        ServerUtils.initialize();
        ServerScheduler.initialize();
        Allocator.initialize();
        BlobStorage.initialize();
        ProjectorLoadBalancer.initialize();
        FileSystems.initialize();
        RPCItemStackTagFilters.initialize();
        NetworkTunnelDevice.TunnelManager.initialize();
    }

    private Main() {
    }
}
