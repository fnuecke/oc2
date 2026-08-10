/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.RobotActions;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static li.cil.oc2.common.Constants.*;
import static li.cil.oc2.common.bus.device.DeviceTypes.key;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;

public final class RobotItem extends ModItem implements CreativeTabItemProvider {
    @Override
    public void addCreativeTabItems(final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output) {
        output.accept(getRobotWithFlash(parameters.holders()));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.robotEnergyPerTick, tooltip);
        TooltipUtils.addEntityEnergyInformation(stack, tooltip);
        TooltipUtils.addEntityInventoryInformation(stack, tooltip);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();

        final Vec3 position;
        if (level.getBlockState(pos).canBeReplaced(new BlockPlaceContext(context))) {
            position = Vec3.atCenterOf(pos);
        } else {
            position = Vec3.atCenterOf(pos.relative(context.getClickedFace()));
        }

        final Robot robot = Entities.ROBOT.get().create(context.getLevel());
        if (robot == null) {
            return InteractionResult.FAIL;
        }

        robot.moveTo(position.x, position.y - robot.getBbHeight() * 0.5f, position.z,
            Direction.fromYRot(context.getRotation()).getOpposite().toYRot(), 0);
        if (!level.noCollision(robot)) {
            return super.useOn(context);
        }

        if (!level.isClientSide()) {
            RobotActions.initializeData(robot);
            robot.importFromItemStack(context.getItemInHand());

            level.addFreshEntity(robot);
            LevelUtils.playSound(level, BlockPos.containing(position), SoundType.METAL, SoundType::getPlaceSound);

            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }

        if (context.getPlayer() != null) {
            context.getPlayer().awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }


    ///////////////////////////////////////////////////////////////////

    private ItemStack getRobotWithFlash(final HolderLookup.Provider provider) {
        final ItemStack robot = new ItemStack(this);

        ItemStackUtils.modifyModDataTag(robot, tag -> {
            final CompoundTag itemsTag = NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME);
            itemsTag.put(key(DeviceTypes.FLASH_MEMORY), makeInventoryTag(provider,
                new ItemStack(Items.FLASH_MEMORY_CUSTOM.get())
            ));
        });

        return robot;
    }
}
