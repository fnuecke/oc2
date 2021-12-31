package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.WeakHashMap;

public final class NetworkCableItem extends ModItem {
    private static final WeakHashMap<ServerPlayer, BlockPos> LINK_STARTS = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer) {
                LINK_STARTS.remove(player);
            }

            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        final ItemStack stack = player.getItemInHand(context.getHand());
        if (stack.isEmpty() || stack.getItem() != this) {
            return super.useOn(context);
        }

        final Level level = context.getLevel();
        final BlockPos currentPos = context.getClickedPos();

        final BlockEntity currentBlockEntity = level.getBlockEntity(currentPos);
        if (!(currentBlockEntity instanceof final NetworkConnectorBlockEntity currentConnector)) {
            return super.useOn(context);
        }

        if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
            final BlockPos startPos = LINK_STARTS.remove(player);
            if (startPos == null || Objects.equals(startPos, currentPos)) {
                if (currentConnector.canConnectMore()) {
                    LINK_STARTS.put(serverPlayer, currentPos);
                } else {
                    player.displayClientMessage(new TranslatableComponent(Constants.CONNECTOR_ERROR_FULL), true);
                }
            } else {
                final BlockEntity startBlockEntity = level.getBlockEntity(startPos);
                if (!(startBlockEntity instanceof final NetworkConnectorBlockEntity startConnector)) {
                    // Starting connector was removed in the meantime.
                    return super.useOn(context);
                }

                final ConnectionResult connectionResult = NetworkConnectorBlockEntity.connect(startConnector, currentConnector);
                switch (connectionResult) {
                    case SUCCESS:
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        break;

                    case FAILURE:
                        LINK_STARTS.put((ServerPlayer) player, startPos);
                        break;
                    case FAILURE_FULL:
                        LINK_STARTS.put((ServerPlayer) player, startPos);
                        player.displayClientMessage(new TranslatableComponent(Constants.CONNECTOR_ERROR_FULL), true);
                        break;
                    case FAILURE_TOO_FAR:
                        LINK_STARTS.put((ServerPlayer) player, startPos);
                        player.displayClientMessage(new TranslatableComponent(Constants.CONNECTOR_ERROR_TOO_FAR), true);
                        break;
                    case FAILURE_OBSTRUCTED:
                        LINK_STARTS.put((ServerPlayer) player, startPos);
                        player.displayClientMessage(new TranslatableComponent(Constants.CONNECTOR_ERROR_OBSTRUCTED), true);
                        break;
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
