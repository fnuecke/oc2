/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import li.cil.oc2.common.util.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

public final class NetworkCableItem extends ModItem {
    private static final String LINK_START_TAG_NAME = API.MOD_ID + ":" + "network_cable_link_start";

    // --------------------------------------------------------------------- //

    public NetworkCableItem(final Properties properties) {
        super(properties);
    }

    // --------------------------------------------------------------------- //

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof final ServerPlayer serverPlayer) {
                final CompoundTag persistentData = EntityUtils.getPersistentData(serverPlayer);
                persistentData.remove(LINK_START_TAG_NAME);
            }

            return InteractionResult.SUCCESS;
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
            final CompoundTag persistentData = EntityUtils.getPersistentData(serverPlayer);
            final BlockPos startPos = persistentData.read(LINK_START_TAG_NAME, BlockPos.CODEC).orElse(null);
            persistentData.remove(LINK_START_TAG_NAME);
            if (startPos == null || Objects.equals(startPos, currentPos)) {
                if (currentConnector.canConnectMore()) {
                    persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, currentPos);
                } else {
                    player.displayClientMessage(Component.translatable(Constants.CONNECTOR_ERROR_FULL), true);
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
                        persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, startPos);
                        break;
                    case FAILURE_FULL:
                        persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, startPos);
                        player.displayClientMessage(Component.translatable(Constants.CONNECTOR_ERROR_FULL), true);
                        break;
                    case FAILURE_TOO_FAR:
                        persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, startPos);
                        player.displayClientMessage(Component.translatable(Constants.CONNECTOR_ERROR_TOO_FAR), true);
                        break;
                    case FAILURE_OBSTRUCTED:
                        persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, startPos);
                        player.displayClientMessage(Component.translatable(Constants.CONNECTOR_ERROR_OBSTRUCTED), true);
                        break;
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
