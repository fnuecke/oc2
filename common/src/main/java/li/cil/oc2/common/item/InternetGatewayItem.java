/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.inet.InternetConnection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class InternetGatewayItem extends ModBlockItem {
    public InternetGatewayItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    // --------------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (Config.internetGatewaysUseEnergy()) {
            TooltipUtils.addEnergyConsumption(0, InternetConnection.FRAME_QUEUE_SIZE * Config.internetGatewayEnergyPerPacket, tooltip);
        }
        if (!Config.internetEnabled) {
            tooltip.accept(Component.translatable(Constants.TOOLTIP_INTERNET_DISABLED)
                .withStyle(s -> s.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
        }
    }
}
