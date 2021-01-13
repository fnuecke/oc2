package li.cil.oc2.common.item;

import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.entity.robot.RobotActions;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.SoundType;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class RobotItem extends Item {
    public RobotItem(final Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final World world = context.getWorld();
        final BlockPos pos = context.getPos();

        final Vector3d position;
        if (world.getBlockState(pos).isReplaceable(new BlockItemUseContext(context))) {
            position = Vector3d.copyCentered(pos);
        } else {
            position = Vector3d.copyCentered(pos.offset(context.getFace()));
        }

        final RobotEntity robot = Entities.ROBOT.get().create(context.getWorld());
        robot.setLocationAndAngles(position.getX(), position.getY() - robot.getHeight() * 0.5f, position.getZ(),
                Direction.fromAngle(context.getPlacementYaw()).getOpposite().getHorizontalAngle(), 0);
        if (!world.hasNoCollisions(robot)) {
            return super.onItemUse(context);
        }

        RobotActions.initializeData(robot);

        if (!world.isRemote()) {
            WorldUtils.playSound(world, new BlockPos(position), SoundType.METAL, SoundType::getPlaceSound);
            world.addEntity(robot);
            if (!context.getPlayer().isCreative()) {
                context.getItem().shrink(1);
            }
        }

        context.getPlayer().addStat(Stats.ITEM_USED.get(this));

        return ActionResultType.SUCCESS;
    }
}
