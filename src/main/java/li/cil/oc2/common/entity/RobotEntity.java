package li.cil.oc2.common.entity;

import li.cil.oc2.common.entity.robot.*;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public final class RobotEntity extends Entity {
    public static int MAX_QUEUED_ACTIONS = 16;

    private static final String COMMAND_PROCESSOR_TAG_NAME = "commands";

    private static final DataParameter<Boolean> IS_RUNNING = EntityDataManager.createKey(RobotEntity.class, DataSerializers.BOOLEAN);

    ///////////////////////////////////////////////////////////////////

    private final AnimationState animationState = new AnimationState();
    private final CommandProcessor commandProcessor = new CommandProcessor();

    ///////////////////////////////////////////////////////////////////

    public RobotEntity(final EntityType<?> type, final World world) {
        super(type, world);
        this.preventEntitySpawning = true;
        setNoGravity(true);
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    public AnimationState getAnimationState() {
        return animationState;
    }

    public boolean isRunning() {
        return dataManager.get(IS_RUNNING);
    }

    public void start() {
        // todo start vm
    }

    public void stop() {
        // todo stop vm

        commandProcessor.clear();
    }

    @Override
    public void tick() {
        super.tick();

        commandProcessor.tick();
    }

    @Override
    public ActionResultType processInitialInteract(final PlayerEntity player, final Hand hand) {
        final ItemStack stack = player.getHeldItem(hand);
        if (Wrenches.isWrench(stack)) {
            if (!world.isRemote()) {
                if (player.isSneaking()) {
                    remove();
                    WorldUtils.playSound(world, getPosition(), SoundType.METAL, SoundType::getBreakSound);
                } else {
                    // todo open container
                }
            }
        } else {
            if (player.isSneaking()) {
                // TODO start machine
                if (!world.isRemote()) {
                    dataManager.set(IS_RUNNING, !dataManager.get(IS_RUNNING));
                }
            } else {
//                if (rand.nextBoolean()) {
//                    commandProcessor.move(MovementDirection.values()[rand.nextInt(MovementDirection.values().length)]);
//                } else {
//                    commandProcessor.rotate(rand.nextBoolean() ? RotationDirection.LEFT : RotationDirection.RIGHT);
                commandProcessor.rotate(RotationDirection.RIGHT);
//                }
                // TODO open terminal + inventory screen
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollide(final Entity entity) {
        return entity != this;
    }

    @Override
    public void applyEntityCollision(final Entity entity) {
    }

    @Override
    public boolean func_241845_aY() {
        return true;
    }

    @Override
    public boolean shouldSpawnRunningEffects() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void registerData() {
        getDataManager().register(IS_RUNNING, false);
        RobotActions.registerData(getDataManager());
    }

    @Override
    protected void writeAdditional(final CompoundNBT compound) {
        compound.put(COMMAND_PROCESSOR_TAG_NAME, commandProcessor.serialize());
    }

    @Override
    protected void readAdditional(final CompoundNBT compound) {
        commandProcessor.deserialize(compound.getCompound(COMMAND_PROCESSOR_TAG_NAME));
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void doBlockCollisions() {
    }

    ///////////////////////////////////////////////////////////////////

    private static float lerpClamped(final float from, final float to, final float delta) {
        if (from < to) {
            return Math.min(from + delta, to);
        } else if (from > to) {
            return Math.max(from - delta, to);
        } else {
            return from;
        }
    }

    private static float remapFrom01To(final float x, final float a1, final float b1) {
        if (a1 == b1) {
            return a1;
        } else {
            return x * (b1 - a1) + a1;
        }
    }

    ///////////////////////////////////////////////////////////////////

    public final class AnimationState {
        private static final float TOP_IDLE_Y = -2f / 16f;
        private static final float BASE_IDLE_Y = -1f / 16f;

        private static final float TRANSLATION_SPEED = 0.005f;
        private static final float ROTATION_SPEED = 1f;
        private static final float MAX_ROTATION = 5f;
        private static final float MIN_ROTATION_SPEED = 0.055f;
        private static final float MAX_ROTATION_SPEED = 0.060f;
        private static final float HOVER_ANIMATION_SPEED = 0.01f;

        public float topRenderOffsetY = TOP_IDLE_Y;
        public float baseRenderOffsetY = BASE_IDLE_Y;
        public float topRenderRotationY;
        public float topRenderTargetRotationY;
        public float topRenderRotationSpeed;
        public float topRenderHover = -(hashCode() & 0xFFFF); // init to "random" to avoid synchronous hovering

        public void update(final float deltaTime, final Random random) {
            if (isRunning() || commandProcessor.hasQueuedActions()) {
                topRenderHover = topRenderHover + deltaTime * HOVER_ANIMATION_SPEED;
                final float topOffsetY = MathHelper.sin(topRenderHover) / 32f;

                topRenderOffsetY = lerpClamped(topRenderOffsetY, topOffsetY, deltaTime * TRANSLATION_SPEED);
                baseRenderOffsetY = lerpClamped(baseRenderOffsetY, topOffsetY, deltaTime * TRANSLATION_SPEED);

                topRenderRotationY = lerpClamped(topRenderRotationY, topRenderTargetRotationY, deltaTime * topRenderRotationSpeed);
                if (topRenderRotationY == topRenderTargetRotationY) {
                    topRenderTargetRotationY = remapFrom01To(random.nextFloat(), -MAX_ROTATION, MAX_ROTATION);
                    topRenderRotationSpeed = remapFrom01To(random.nextFloat(), MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
                }
            } else {
                topRenderOffsetY = lerpClamped(topRenderOffsetY, TOP_IDLE_Y, deltaTime * TRANSLATION_SPEED * 2);
                baseRenderOffsetY = lerpClamped(baseRenderOffsetY, BASE_IDLE_Y, deltaTime * TRANSLATION_SPEED);

                topRenderRotationY = lerpClamped(topRenderRotationY, 0, deltaTime * ROTATION_SPEED);
            }
        }
    }

    private final class CommandProcessor {
        private static final String QUEUE_TAG_NAME = "queue";
        private static final String ACTION_TAG_NAME = "action";

        private final Queue<AbstractRobotAction> queue = new ArrayDeque<>(MAX_QUEUED_ACTIONS);
        @Nullable private AbstractRobotAction action;

        public boolean hasQueuedActions() {
            return action != null || !queue.isEmpty();
        }

        public boolean move(final MovementDirection direction) {
            return addAction(new RobotMovementAction(direction));
        }

        public boolean rotate(final RotationDirection direction) {
            return addAction(new RobotRotationAction(direction));
        }

        public void tick() {
            if (getEntityWorld().isRemote()) {
                RobotActions.performClient(RobotEntity.this);
            } else {
                if (action != null) {
                    if (action.perform(RobotEntity.this)) {
                        action = null;
                    }
                }
                if (action == null) {
                    action = queue.poll();
                    if (action != null) {
                        action.initialize(RobotEntity.this);
                    }
                }
            }
        }

        public void clear() {
            queue.clear();
        }

        public CompoundNBT serialize() {
            final CompoundNBT tag = new CompoundNBT();

            final ListNBT queueTag = new ListNBT();
            for (final AbstractRobotAction action : queue) {
                queueTag.add(RobotActions.serialize(action));
            }
            tag.put(QUEUE_TAG_NAME, queueTag);

            if (action != null) {
                tag.put(ACTION_TAG_NAME, RobotActions.serialize(action));
            }

            return tag;
        }

        public void deserialize(final CompoundNBT tag) {
            queue.clear();
            action = null;

            final ListNBT queueTag = tag.getList(QUEUE_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < queueTag.size(); i++) {
                final AbstractRobotAction action = RobotActions.deserialize(queueTag.getCompound(i));
                if (action != null) {
                    queue.add(action);
                }
            }

            if (tag.contains(ACTION_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                action = RobotActions.deserialize(tag.getCompound(ACTION_TAG_NAME));
                action.initialize(RobotEntity.this);
            }
        }

        ///////////////////////////////////////////////////////////////////

        private boolean addAction(final AbstractRobotAction action) {
            if (getEntityWorld().isRemote()) {
                return false;
            }

            if (!isRunning()) {
                return false;
            }

            if (queue.size() < MAX_QUEUED_ACTIONS) {
                queue.add(action);
                return true;
            } else {
                return false;
            }
        }
    }
}
