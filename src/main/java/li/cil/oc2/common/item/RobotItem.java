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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import static li.cil.oc2.common.Constants.*;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;

public final class RobotItem extends ModItem {
    @Override
    public void fillItemCategory(final CreativeModeTab group, final NonNullList<ItemStack> items) {
        if (allowdedIn(group)) {
            items.add(getRobotWithFlash());
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level world, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
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

        return InteractionResult.sidedSuccess(world.isClientSide());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initializeClient(final Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                return new RobotItemStackRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    private ItemStack getRobotWithFlash() {
        final ItemStack robot = new ItemStack(this);

        final CompoundTag itemsTag = NBTUtils.getOrCreateChildTag(robot.getOrCreateTag(), API.MOD_ID, ITEMS_TAG_NAME);
        itemsTag.put(DeviceTypes.FLASH_MEMORY.getRegistryName().toString(), makeInventoryTag(
            new ItemStack(Items.FLASH_MEMORY_CUSTOM.get())
        ));

        return robot;
    }
}
