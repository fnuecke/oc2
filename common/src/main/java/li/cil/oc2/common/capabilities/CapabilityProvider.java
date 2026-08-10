/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface CapabilityProvider {
    @Nullable
    <T> T getCapability(CapabilityType<T> capability, @Nullable Direction side);
}
