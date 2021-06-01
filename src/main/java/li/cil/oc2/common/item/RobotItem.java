package li.cil.oc2.common.item;

import li.cil.oc2.client.renderer.tileentity.RobotItemStackRenderer;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.energy.EnergyStorageItemStack;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.entity.robot.RobotActions;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static li.cil.oc2.common.Constants.ENERGY_TAG_NAME;
import static li.cil.oc2.common.Constants.MOD_TAG_NAME;

public final class RobotItem extends ModItem {
    public RobotItem() {
        super(createProperties().setISTER(() -> RobotItemStackRenderer::new));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void appendHoverText(final ItemStack stack, final @Nullable Level level, final List<Component> tooltip, final TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltip, tooltipFlag);
        TooltipUtils.addEnergyConsumption(Config.robotEnergyPerTick, tooltip);
        TooltipUtils.addEntityEnergyInformation(stack, tooltip);
        TooltipUtils.addEntityInventoryInformation(stack, tooltip);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final CompoundTag nbt) {
        if (Config.robotsUseEnergy()) {
            return new EnergyStorageItemStack(stack, Config.robotEnergyStorage, MOD_TAG_NAME, ENERGY_TAG_NAME);
        } else {
            return null;
        }
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level world = context.getLevel();
        final BlockPos pos = context.getClickedPos();

        final Vec3 position;
        if (world.getBlockState(pos).canBeReplaced(new BlockPlaceContext(context))) {
            position = Vec3.atCenterOf(pos);
        } else {
            position = Vec3.atCenterOf(pos.relative(context.getClickedFace()));
        }

        final RobotEntity robot = Entities.ROBOT.get().create(context.getLevel());
        robot.moveTo(position.x, position.y - robot.getBbHeight() * 0.5f, position.z,
                Direction.fromYRot(context.getRotation()).getOpposite().toYRot(), 0);
        if (!world.noCollision(robot)) {
            return super.useOn(context);
        }

        if (!world.isClientSide) {
            RobotActions.initializeData(robot);
            robot.importFromItemStack(context.getItemInHand());

            world.addFreshEntity(robot);
            WorldUtils.playSound(world, new BlockPos(position), SoundType.METAL, SoundType::getPlaceSound);

            if (!context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }

        context.getPlayer().awardStat(Stats.ITEM_USED.get(this));

        return InteractionResult.SUCCESS;
    }
}
