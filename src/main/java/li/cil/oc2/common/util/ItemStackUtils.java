package li.cil.oc2.common.util;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

import static li.cil.oc2.common.Constants.MOD_TAG_NAME;

public final class ItemStackUtils {
    public static CompoundNBT getModDataTag(final ItemStack stack) {
        return NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME);
    }

    public static CompoundNBT getOrCreateModDataTag(final ItemStack stack) {
        return NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), MOD_TAG_NAME);
    }

    public static Optional<ItemEntity> spawnAsEntity(final World world, final BlockPos pos, final ItemStack stack) {
        return spawnAsEntity(world, Vector3d.copyCentered(pos), stack);
    }

    public static Optional<ItemEntity> spawnAsEntity(final World world, final Vector3d pos, final ItemStack stack) {
        if (world.isRemote() || stack.isEmpty()) {
            return Optional.empty();
        }

        final Random rng = world.rand;

        final float tx = 0.5f * (rng.nextFloat() - 1.0f);
        final float ty = 0.5f * (rng.nextFloat() - 1.0f);
        final float tz = 0.5f * (rng.nextFloat() - 1.0f);
        final double px = pos.getX() + tx;
        final double py = pos.getY() + ty;
        final double pz = pos.getZ() + tz;

        final ItemEntity entity = new ItemEntity(world, px, py, pz, stack);
        entity.setDefaultPickupDelay();
        world.addEntity(entity);

        return Optional.of(entity);
    }

    public static Optional<ItemEntity> spawnAsEntity(final World world, final BlockPos pos, final ItemStack stack, @Nullable final Direction direction) {
        return spawnAsEntity(world, Vector3d.copyCentered(pos), stack, direction);
    }

    public static Optional<ItemEntity> spawnAsEntity(final World world, final Vector3d pos, final ItemStack stack, @Nullable final Direction direction) {
        if (direction == null) {
            return spawnAsEntity(world, pos, stack);
        }

        if (world.isRemote() || stack.isEmpty()) {
            return Optional.empty();
        }

        final Random rng = world.rand;

        final float ox = direction.getXOffset();
        final float oy = direction.getYOffset();
        final float oz = direction.getZOffset();
        final float tx = 0.1f * (rng.nextFloat() - 0.5f) + ox * 0.65f;
        final float ty = 0.1f * (rng.nextFloat() - 0.5f) + oy * 0.75f + (ox + oz) * 0.25f;
        final float tz = 0.1f * (rng.nextFloat() - 0.5f) + oz * 0.65f;
        final double px = pos.getX() + tx;
        final double py = pos.getY() + ty;
        final double pz = pos.getZ() + tz;

        final ItemEntity entity = new ItemEntity(world, px, py, pz, stack);

        entity.setMotion(
                0.0125 * (rng.nextDouble() - 0.5) + ox * 0.03,
                0.0125 * (rng.nextDouble() - 0.5) + oy * 0.08 + (ox + oz) * 0.03,
                0.0125 * (rng.nextDouble() - 0.5) + oz * 0.03
        );

        entity.setDefaultPickupDelay();
        world.addEntity(entity);

        return Optional.of(entity);
    }
}
