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

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

@Environment(EnvType.CLIENT)
public final class EnergyNode extends ProgressBarNode {
    private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/energy_bar");
    private static final int WIDTH = 12, HEIGHT = 26;

    // --------------------------------------------------------------------- //

    public EnergyNode(final AbstractMachineContainer menu) {
        super(Axis.VERTICAL, () -> menu.getEnergy() / (double) menu.getEnergyCapacity(), ProgressBarNode.sprite(FILL));
        tooltip(() -> List.of(
            Component.translatable(Constants.TOOLTIP_ENERGY,
                withFormat(menu.getEnergy() + "/" + menu.getEnergyCapacity(), ChatFormatting.GREEN)),
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
}
