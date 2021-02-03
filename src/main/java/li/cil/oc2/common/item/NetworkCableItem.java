package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.tileentity.NetworkConnectorTileEntity;
import li.cil.oc2.common.tileentity.NetworkConnectorTileEntity.ConnectionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
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
    public ActionResult<ItemStack> onItemRightClick(final World world, final PlayerEntity player, final Hand hand) {
        if (player.isSneaking()) {
            if (player instanceof ServerPlayerEntity) {
                LINK_STARTS.remove(player);
            }

            return ActionResult.resultSuccess(player.getHeldItem(hand));
        }

        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final PlayerEntity player = context.getPlayer();
        if (player == null) {
            return super.onItemUse(context);
        }

        final ItemStack stack = player.getHeldItem(context.getHand());
        if (stack.isEmpty() || stack.getItem() != this) {
            return super.onItemUse(context);
        }

        final World world = context.getWorld();
        final BlockPos currentPos = context.getPos();

        final TileEntity currentTileEntity = world.getTileEntity(currentPos);
        if (!(currentTileEntity instanceof NetworkConnectorTileEntity)) {
            return super.onItemUse(context);
        }

        if (!world.isRemote() && player instanceof ServerPlayerEntity) {
            final BlockPos startPos = LINK_STARTS.remove(player);
            if (startPos == null || Objects.equals(startPos, currentPos)) {
                if (((NetworkConnectorTileEntity) currentTileEntity).canConnectMore()) {
                    LINK_STARTS.put((ServerPlayerEntity) player, currentPos);
                } else {
                    player.sendStatusMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_FULL), true);
                }
            } else {
                final TileEntity startTileEntity = world.getTileEntity(startPos);
                if (!(startTileEntity instanceof NetworkConnectorTileEntity)) {
                    // Starting connector was removed in the meantime.
                    return super.onItemUse(context);
                }

                final NetworkConnectorTileEntity connectorA = (NetworkConnectorTileEntity) startTileEntity;
                final NetworkConnectorTileEntity connectorB = (NetworkConnectorTileEntity) currentTileEntity;

                final ConnectionResult connectionResult = NetworkConnectorTileEntity.connect(connectorA, connectorB);
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
                        player.sendStatusMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_FULL), true);
                        break;
                    case FAILURE_TOO_FAR:
                        LINK_STARTS.put((ServerPlayerEntity) player, startPos);
                        player.sendStatusMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_TOO_FAR), true);
                        break;
                    case FAILURE_OBSTRUCTED:
                        LINK_STARTS.put((ServerPlayerEntity) player, startPos);
                        player.sendStatusMessage(new TranslationTextComponent(Constants.CONNECTOR_ERROR_OBSTRUCTED), true);
                        break;
                }
            }
        }

        return ActionResultType.SUCCESS;
    }
}
