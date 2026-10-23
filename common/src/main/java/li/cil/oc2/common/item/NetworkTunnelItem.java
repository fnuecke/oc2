/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.container.NetworkTunnelContainer;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.TextFormatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static li.cil.oc2.common.util.TranslationUtils.key;

public final class NetworkTunnelItem extends ModItem {
    private static final String TUNNEL_ID_TAG_NAME = "tunnel";
    private static final String TUNNEL_ID_TEXT = key("tooltip.{mod}.network_tunnel_id");

    // --------------------------------------------------------------------- //

    public NetworkTunnelItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    // --------------------------------------------------------------------- //

    public static Optional<UUID> getTunnelId(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        return tag.read(TUNNEL_ID_TAG_NAME, UUIDUtil.CODEC);
    }

    public static void setTunnelId(final ItemStack stack, final UUID value) {
        ItemStackUtils.modifyModDataTag(stack, tag -> tag.store(TUNNEL_ID_TAG_NAME, UUIDUtil.CODEC, value));
    }

    // --------------------------------------------------------------------- //

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        getTunnelId(stack).ifPresent(id -> {
            final String idString = StringUtil.truncateStringIfNecessary(id.toString(), 8 + 3, true);
            final MutableComponent idComponent = TextFormatUtils.withFormat(idString, ChatFormatting.GREEN);
            tooltip.accept(Component.translatable(TUNNEL_ID_TEXT, idComponent).withStyle(ChatFormatting.GRAY));
        });
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            openContainerScreen(serverPlayer, hand);
        }

        return InteractionResult.SUCCESS;
    }

    // --------------------------------------------------------------------- //

    private void openContainerScreen(final ServerPlayer player, final InteractionHand hand) {
        NetworkTunnelContainer.createServer(player, hand);
    }
}
