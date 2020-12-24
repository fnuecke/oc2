package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

public final class ItemStackUtils {
    public static final String MOD_NBT_TAG_NAME = API.MOD_ID;

    @Nullable
    public static CompoundNBT getModDataTag(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getChildTag(MOD_NBT_TAG_NAME);
    }

    public static CompoundNBT getOrCreateModDataTag(final ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException();
        }

        return stack.getOrCreateChildTag(MOD_NBT_TAG_NAME);
    }

    public static Optional<ItemEntity> spawnAsEntity(final World world, final BlockPos pos, final ItemStack stack) {
        if (world.isRemote() || stack.isEmpty()) {
            return Optional.empty();
        }

        final Random rng = world.rand;

        final float tx = 0.5f * (rng.nextFloat() - 1.0f);
        final float ty = 0.5f * (rng.nextFloat() - 1.0f);
        final float tz = 0.5f * (rng.nextFloat() - 1.0f);
        final double px = pos.getX() + 0.5 + tx;
        final double py = pos.getY() + 0.5 + ty;
        final double pz = pos.getZ() + 0.5 + tz;

        final ItemEntity entity = new ItemEntity(world, px, py, pz, stack);
        entity.setDefaultPickupDelay();
        world.addEntity(entity);

        return Optional.of(entity);
    }

    public static Optional<ItemEntity> spawnAsEntity(final World world, final BlockPos pos, final ItemStack stack, final Direction direction) {
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
        final double px = pos.getX() + 0.5 + tx;
        final double py = pos.getY() + 0.5 + ty;
        final double pz = pos.getZ() + 0.5 + tz;

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
