package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.renderer.tileentity.RobotItemStackRenderer;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.energy.EnergyStorageItemStack;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.entity.robot.RobotActions;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.SoundType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

import static li.cil.oc2.common.Constants.*;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;

public final class RobotItem extends ModItem {
    public RobotItem() {
        super(createProperties().setISTER(() -> RobotItemStackRenderer::new));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemCategory(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (allowdedIn(group)) {
            items.add(getRobotWithFlash());
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final World world, final List<ITextComponent> tooltip, final ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.robotEnergyPerTick, tooltip);
        TooltipUtils.addEntityEnergyInformation(stack, tooltip);
        TooltipUtils.addEntityInventoryInformation(stack, tooltip);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(final ItemStack stack, @Nullable final CompoundNBT nbt) {
        if (Config.robotsUseEnergy()) {
            return new EnergyStorageItemStack(stack, Config.robotEnergyStorage, MOD_TAG_NAME, ENERGY_TAG_NAME);
        } else {
            return null;
        }
    }

    @Override
    public ActionResultType useOn(final ItemUseContext context) {
        final World world = context.getLevel();
        final BlockPos pos = context.getClickedPos();

        final Vector3d position;
        if (world.getBlockState(pos).canBeReplaced(new BlockItemUseContext(context))) {
            position = Vector3d.atCenterOf(pos);
        } else {
            position = Vector3d.atCenterOf(pos.relative(context.getClickedFace()));
        }

        final RobotEntity robot = Entities.ROBOT.get().create(context.getLevel());
        robot.moveTo(position.x, position.y - robot.getBbHeight() * 0.5f, position.z,
                Direction.fromYRot(context.getRotation()).getOpposite().toYRot(), 0);
        if (!world.noCollision(robot)) {
            return super.useOn(context);
        }

        if (!world.isClientSide()) {
            RobotActions.initializeData(robot);
            robot.importFromItemStack(context.getItemInHand());

            world.addFreshEntity(robot);
            WorldUtils.playSound(world, new BlockPos(position), SoundType.METAL, SoundType::getPlaceSound);

            if (!context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }

        context.getPlayer().awardStat(Stats.ITEM_USED.get(this));

        return ActionResultType.sidedSuccess(world.isClientSide());
    }

    ///////////////////////////////////////////////////////////////////

    private ItemStack getRobotWithFlash() {
        final ItemStack robot = new ItemStack(this);

        final CompoundNBT itemsTag = NBTUtils.getOrCreateChildTag(robot.getOrCreateTag(), API.MOD_ID, ITEMS_TAG_NAME);
        itemsTag.put(DeviceTypes.FLASH_MEMORY.getRegistryName().toString(), makeInventoryTag(
                new ItemStack(Items.FLASH_MEMORY_CUSTOM.get())
        ));

        return robot;
    }
}
