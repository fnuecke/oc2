/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.container.NetworkTunnelContainer;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.TextFormatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static li.cil.oc2.common.util.TranslationUtils.key;

public final class NetworkTunnelItem extends ModItem {
    private static final String TUNNEL_ID_TAG_NAME = "tunnel";
    private static final String TUNNEL_ID_TEXT = key("tooltip.{mod}.network_tunnel_id");

    ///////////////////////////////////////////////////////////////////

    public NetworkTunnelItem() {
        super(createProperties().stacksTo(1));
    }

    ///////////////////////////////////////////////////////////////////

    public static Optional<UUID> getTunnelId(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (tag.hasUUID(TUNNEL_ID_TAG_NAME)) {
            return Optional.of(tag.getUUID(TUNNEL_ID_TAG_NAME));
        } else {
            return Optional.empty();
        }
    }

    public static void setTunnelId(final ItemStack stack, final UUID value) {
        ItemStackUtils.getOrCreateModDataTag(stack).putUUID(TUNNEL_ID_TAG_NAME, value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        getTunnelId(stack).ifPresent(id -> {
            final String idString = StringUtil.truncateStringIfNecessary(id.toString(), 8 + 3, true);
            final MutableComponent idComponent = TextFormatUtils.withFormat(idString, ChatFormatting.GREEN);
            tooltip.add(Component.translatable(TUNNEL_ID_TEXT, idComponent).withStyle(ChatFormatting.GRAY));
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            openContainerScreen(serverPlayer, hand);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    ///////////////////////////////////////////////////////////////////

    private void openContainerScreen(final ServerPlayer player, final InteractionHand hand) {
        NetworkTunnelContainer.createServer(player, hand);
    }
}
