package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.tileentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.tileentity.NetworkConnectorBlockEntity.ConnectionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.WeakHashMap;

public final class NetworkCableItem extends ModItem {
    private static final WeakHashMap<ServerPlayerEntity, BlockPos> LINK_STARTS = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    @Override
    public ActionResult<ItemStack> use(final World world, final PlayerEntity player, final Hand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayerEntity) {
                LINK_STARTS.remove(player);
            }

            return ActionResult.success(player.getItemInHand(hand));
        }

        return super.use(world, player, hand);
    }

    @Override
    public ActionResultType useOn(final ItemUseContext context) {
        final PlayerEntity player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        final ItemStack stack = player.getItemInHand(context.getHand());
        if (stack.isEmpty() || stack.getItem() != this) {
            return super.useOn(context);
        }

        final World world = context.getLevel();
        final BlockPos currentPos = context.getClickedPos();

        final BlockEntity currentBlockEntity = world.getBlockEntity(currentPos);
        if (!(currentBlockEntity instanceof NetworkConnectorBlockEntity)) {
            return super.useOn(context);
        }

        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            final BlockPos startPos = LINK_STARTS.remove(player);
            if (startPos == null || Objects.equals(startPos, currentPos)) {
                if (((NetworkConnectorBlockEntity) currentBlockEntity).canConnectMore()) {
                    LINK_STARTS.put((ServerPlayerEntity) player, currentPos);
                } else {
                    player.displayClientMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_FULL), true);
                }
            } else {
                final BlockEntity startBlockEntity = world.getBlockEntity(startPos);
                if (!(startBlockEntity instanceof NetworkConnectorBlockEntity)) {
                    // Starting connector was removed in the meantime.
                    return super.useOn(context);
                }

                final NetworkConnectorBlockEntity connectorA = (NetworkConnectorBlockEntity) startBlockEntity;
                final NetworkConnectorBlockEntity connectorB = (NetworkConnectorBlockEntity) currentBlockEntity;

                final ConnectionResult connectionResult = NetworkConnectorBlockEntity.connect(connectorA, connectorB);
                switch (connectionResult) {
                    case SUCCESS:
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        break;

                    case FAILURE:
                        LINK_STARTS.put((ServerPlayerEntity) player, startPos);
                        break;
                    case FAILURE_FULL:
                        LINK_STARTS.put((ServerPlayerEntity) player, startPos);
                        player.displayClientMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_FULL), true);
                        break;
                    case FAILURE_TOO_FAR:
                        LINK_STARTS.put((ServerPlayerEntity) player, startPos);
                        player.displayClientMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_TOO_FAR), true);
                        break;
                    case FAILURE_OBSTRUCTED:
                        LINK_STARTS.put((ServerPlayerEntity) player, startPos);
                        player.displayClientMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_OBSTRUCTED), true);
                        break;
                }
            }
        }

        return ActionResultType.SUCCESS;
    }
}
