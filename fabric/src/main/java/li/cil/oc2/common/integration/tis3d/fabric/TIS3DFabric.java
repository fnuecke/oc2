/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.tis3d.fabric;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.fabric.CapabilitiesImpl;
import li.cil.oc2.common.integration.ModIntegration;
import li.cil.oc2.common.integration.tis3d.TIS3D;
import li.cil.tis3d.api.platform.FabricProviderInitializer;

public final class TIS3DFabric implements FabricProviderInitializer {
    @Override
    public void registerProviders() {
        ModIntegration.TIS3D.run(() -> {
            TIS3D.registerProviders();
            CapabilitiesImpl
                .block(Capabilities.NETWORK_INTERFACE)
                .registerForBlockEntities(TIS3D::networkInterfaceFor, TIS3D.casingBlockEntityType());
        });
    }
}
