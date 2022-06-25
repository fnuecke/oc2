package li.cil.oc2.common.block;

import li.cil.oc2.common.blockentity.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InternetGatewayBlock extends HalfTransparentBlock implements EntityBlock {

    public InternetGatewayBlock() {
        super(Properties.of(Material.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntities.INTERNET_GATEWAY.get().create(pos, state);
    }
    
}
