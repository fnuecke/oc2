/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.layout.Axis;
import li.cil.oc2.client.gui.layout.ProgressBarNode;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.LongSupplier;

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

@Environment(EnvType.CLIENT)
public final class EnergyNode extends ProgressBarNode {
    private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/energy_bar");
    private static final int WIDTH = 12, HEIGHT = 26;

    // --------------------------------------------------------------------- //

    public EnergyNode(final LongSupplier energy, final LongSupplier capacity) {
        super(Axis.VERTICAL, () -> energy.getAsLong() / (double) capacity.getAsLong(), ProgressBarNode.sprite(FILL));
        tooltip(() -> List.of(energyTooltip(energy, capacity)));
    }

    public EnergyNode(final AbstractMachineContainer menu) {
        this(menu::getEnergy, menu::getEnergyCapacity);
        tooltip(() -> List.of(
            energyTooltip(menu::getEnergy, menu::getEnergyCapacity),
            Component.translatable(Constants.TOOLTIP_ENERGY_CONSUMPTION,
                withFormat(String.valueOf(menu.getEnergyConsumption()), ChatFormatting.GREEN))));
    }

    // --------------------------------------------------------------------- //
    // Node

    @Override
    protected int contentWidth() {
        return WIDTH;
    }

    @Override
    protected int contentHeight() {
        return HEIGHT;
    }

    // --------------------------------------------------------------------- //

    private static Component energyTooltip(final LongSupplier energy, final LongSupplier capacity) {
        return Component.translatable(Constants.TOOLTIP_ENERGY,
            withFormat(energy.getAsLong() + "/" + capacity.getAsLong(), ChatFormatting.GREEN));
    }
}
