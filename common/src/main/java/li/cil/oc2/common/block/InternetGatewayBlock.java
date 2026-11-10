/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.client.gui.InternetGatewayConfigurationScreen;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class InternetGatewayBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<InternetGatewayBlock> CODEC = simpleCodec(InternetGatewayBlock::new);
    private static final double PARTICLES_MIN = 3 / 16.0;
    private static final double PARTICLES_MAX = 13 / 16.0;

    // --------------------------------------------------------------------- //

    public InternetGatewayBlock(final Properties properties) {
        super(properties
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends InternetGatewayBlock> codec() {
        return CODEC;
    }

    // --------------------------------------------------------------------- //

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final InternetGatewayBlockEntity gateway) {
            if (level.isClientSide()) {
                InternetGatewayConfigurationScreen.open(gateway);
            }

            return InteractionResult.SUCCESS;
        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof final InternetGatewayBlockEntity gateway) || !gateway.isOperational()) {
            return;
        }

        level.addParticle(ParticleTypes.REVERSE_PORTAL,
            pos.getX() + Mth.lerp(random.nextDouble(), PARTICLES_MIN, PARTICLES_MAX),
            pos.getY() + 1.0,
            pos.getZ() + Mth.lerp(random.nextDouble(), PARTICLES_MIN, PARTICLES_MAX),
            0.0, 0.03, 0.0);
    }

    // --------------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.INTERNET_GATEWAY.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.INTERNET_GATEWAY.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
