/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public final class ProjectorBlock extends FlippableOrientableBlock implements EntityBlock, EnergyConsumingBlock {
    public static final MapCodec<ProjectorBlock> CODEC = MapCodec.unit(ProjectorBlock::new);

    @Override
    protected MapCodec<? extends ProjectorBlock> codec() {
        return CODEC;
    }

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // We bake the visual indents on the front and sides into the collision shape, to prevent stuff being
    // placeable on those sides, such as network connectors, torches, etc.
    private static final VoxelShape NEG_Z_SHAPE = Shapes.join(Shapes.block(), Shapes.or(
        Shapes.box(0 / 16f, 2 / 16f, 2 / 16f, 1 / 16f, 6 / 16f, 14 / 16f),
        Shapes.box(15 / 16f, 2 / 16f, 2 / 16f, 16 / 16f, 6 / 16f, 14 / 16f),
        Shapes.box(4 / 16f, 4 / 16f, 0 / 16f, 12 / 16f, 12 / 16f, 2 / 16f)
    ), (a, b) -> a && !b);

    public ProjectorBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(LIT) ? 8 : 0)
            .strength(1.5f, 6.0f), FlippableOrientation.NORTH, NEG_Z_SHAPE);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getEnergyConsumption() {
        if (Config.projectorsUseEnergy()) {
            return Config.projectorEnergyPerTick;
        } else {
            return 0;
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.PROJECTOR.get().create(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.PROJECTOR.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
