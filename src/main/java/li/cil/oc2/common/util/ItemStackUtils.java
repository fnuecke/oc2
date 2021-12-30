package li.cil.oc2.common.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

import static li.cil.oc2.common.Constants.MOD_TAG_NAME;

public final class ItemStackUtils {
    public static CompoundTag getModDataTag(final ItemStack stack) {
        return NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME);
    }

    public static CompoundTag getOrCreateModDataTag(final ItemStack stack) {
        return NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), MOD_TAG_NAME);
    }

    @Nullable
    public static BlockState getBlockState(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        final Block block = Block.byItem(stack.getItem());
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) {
            return null;
        }

        return block.defaultBlockState();
    }

    public static Optional<ItemEntity> spawnAsEntity(final Level world, final BlockPos pos, final ItemStack stack) {
        return spawnAsEntity(world, Vec3.atCenterOf(pos), stack);
    }

    public static Optional<ItemEntity> spawnAsEntity(final Level world, final Vec3 pos, final ItemStack stack) {
        if (world.isClientSide() || stack.isEmpty()) {
            return Optional.empty();
        }

        final Random rng = world.random;

        final float tx = 0.5f * (rng.nextFloat() - 1.0f);
        final float ty = 0.5f * (rng.nextFloat() - 1.0f);
        final float tz = 0.5f * (rng.nextFloat() - 1.0f);
        final double px = pos.x + tx;
        final double py = pos.y + ty;
        final double pz = pos.z + tz;

        final ItemEntity entity = new ItemEntity(world, px, py, pz, stack);
        entity.setDefaultPickUpDelay();
        world.addFreshEntity(entity);

        return Optional.of(entity);
    }

    public static Optional<ItemEntity> spawnAsEntity(final Level world, final BlockPos pos, final ItemStack stack, @Nullable final Direction direction) {
        return spawnAsEntity(world, Vec3.atCenterOf(pos), stack, direction);
    }

    public static Optional<ItemEntity> spawnAsEntity(final Level world, final Vec3 pos, final ItemStack stack, @Nullable final Direction direction) {
        if (direction == null) {
            return spawnAsEntity(world, pos, stack);
        }

        if (world.isClientSide || stack.isEmpty()) {
            return Optional.empty();
        }

        final Random rng = world.random;

        final float ox = direction.getStepX();
        final float oy = direction.getStepY();
        final float oz = direction.getStepZ();
        final float tx = 0.1f * (rng.nextFloat() - 0.5f) + ox * 0.65f;
        final float ty = 0.1f * (rng.nextFloat() - 0.5f) + oy * 0.75f + (ox + oz) * 0.25f;
        final float tz = 0.1f * (rng.nextFloat() - 0.5f) + oz * 0.65f;
        final double px = pos.x + tx;
        final double py = pos.y + ty;
        final double pz = pos.z + tz;

        final ItemEntity entity = new ItemEntity(world, px, py, pz, stack);

        entity.setDeltaMovement(
                0.0125 * (rng.nextDouble() - 0.5) + ox * 0.03,
                0.0125 * (rng.nextDouble() - 0.5) + oy * 0.08 + (ox + oz) * 0.03,
                0.0125 * (rng.nextDouble() - 0.5) + oz * 0.03
        );

        entity.setDefaultPickUpDelay();
        world.addFreshEntity(entity);

        return Optional.of(entity);
    }
}
