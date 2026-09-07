/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.item.TooltipUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.List;

public final class ChargerBlock extends HalfTransparentBlock implements EntityBlock {
    public ChargerBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    // --------------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip, final TooltipFlag advanced) {
        super.appendHoverText(stack, context, tooltip, advanced);
        if (Config.chargersUseEnergy()) {
            TooltipUtils.addEnergyConsumption(0, Config.chargerEnergyPerTick, tooltip);
        }
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rot) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rot.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    // --------------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.CHARGER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.CHARGER.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HorizontalDirectionalBlock.FACING);
    }
}
