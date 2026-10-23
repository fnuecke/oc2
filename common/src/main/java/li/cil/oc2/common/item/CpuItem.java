/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.common.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class CpuItem extends ModItem {
    private final ArchitectureType architectureType;

    // --------------------------------------------------------------------- //

    public CpuItem(final Properties properties, final ArchitectureType architectureType) {
        super(properties);
        this.architectureType = architectureType;
    }

    // --------------------------------------------------------------------- //

    public ArchitectureType getArchitectureType() {
        return architectureType;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.cpuEnergyPerTick(architectureType), tooltip);
    }
}
