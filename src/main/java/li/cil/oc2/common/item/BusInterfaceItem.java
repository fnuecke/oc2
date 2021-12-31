package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.BusCableBlock.ConnectionType;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

public final class BusInterfaceItem extends ModBlockItem {
    public BusInterfaceItem() {
        super(Blocks.BUS_CABLE.get());
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final @Nullable Level level, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.busInterfaceEnergyPerTick, tooltip);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Vec3 localHitPos = context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()));
        final Direction side = Direction.getNearest(localHitPos.x, localHitPos.y, localHitPos.z);
        final InteractionResult result = tryAddToBlock(context, side);
        return result.consumesAction() ? result : super.useOn(context);
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        final InteractionResult result = tryAddToBlock(context, context.getClickedFace().getOpposite());
        return result.consumesAction() ? result : super.place(context);
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    public void fillItemCategory(final CreativeModeTab group, final NonNullList<ItemStack> items) {
        if (allowdedIn(group)) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public void registerBlocks(final Map<Block, Item> map, final Item item) {
    }

    @Override
    public void removeFromBlockToItemMap(final Map<Block, Item> map, final Item item) {
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    protected BlockState getPlacementState(final BlockPlaceContext context) {
        final BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }

        final EnumProperty<ConnectionType> connectionTypeProperty =
                BusCableBlock.FACING_TO_CONNECTION_MAP.get(context.getClickedFace().getOpposite());
        return state
                .setValue(BusCableBlock.HAS_CABLE, false)
                .setValue(connectionTypeProperty, ConnectionType.INTERFACE);
    }

    ///////////////////////////////////////////////////////////////////

    private static InteractionResult tryAddToBlock(final UseOnContext context, final Direction side) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);

        if (!BusCableBlock.addInterface(level, pos, state, side)) {
            return InteractionResult.PASS;
        }

        final Player player = context.getPlayer();
        final ItemStack stack = context.getItemInHand();

        if (player instanceof ServerPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, stack);
        }

        LevelUtils.playSound(level, pos, state.getSoundType(level, pos, player), SoundType::getPlaceSound);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
