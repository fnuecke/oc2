/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.FixedSizeItemStackHandler;
import li.cil.oc2.common.container.RobotInventoryContainer;
import li.cil.oc2.common.container.RobotTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.robot.*;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.*;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TerminalUtils;
import li.cil.oc2.common.vm.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.singleton;
import static li.cil.oc2.common.Constants.*;

public final class Robot extends Entity implements li.cil.oc2.api.capabilities.Robot, TerminalUserProvider {
    public static final EntityDataAccessor<BlockPos> TARGET_POSITION = SynchedEntityData.defineId(Robot.class, EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<Direction> TARGET_DIRECTION = SynchedEntityData.defineId(Robot.class, EntityDataSerializers.DIRECTION);
    public static final EntityDataAccessor<Byte> SELECTED_SLOT = SynchedEntityData.defineId(Robot.class, EntityDataSerializers.BYTE);

    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String BUS_ELEMENT_TAG_NAME = "bus_element";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String COMMAND_PROCESSOR_TAG_NAME = "commands";
    private static final String INVENTORY_TAG_NAME = "inventory";
    private static final String SELECTED_SLOT_TAG_NAME = "selected_slot";

    private static final int MAX_QUEUED_ACTIONS = 16;
    private static final int MAX_QUEUED_RESULTS = 16;

    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 2;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int MODULE_SLOTS = 4;
    private static final int INVENTORY_SIZE = 12;

    ///////////////////////////////////////////////////////////////////

    private final Consumer<ChunkEvent.Unload> chunkUnloadListener = this::handleChunkUnload;
    private final Consumer<LevelEvent.Unload> worldUnloadListener = this::handleWorldUnload;
    private final BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();

    private final AnimationState animationState = new AnimationState();
    private final RobotActionProcessor actionProcessor = new RobotActionProcessor();
    private final Terminal terminal = new Terminal();
    private final RobotVirtualMachine virtualMachine;
    private final RobotBusElement busElement = new RobotBusElement();
    private final RobotItemStackHandlers deviceItems = new RobotItemStackHandlers();
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.robotEnergyStorage);
    private final ItemStackHandler inventory = new FixedSizeItemStackHandler(INVENTORY_SIZE);
    private final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());
    private long lastPistonMovement;

    ///////////////////////////////////////////////////////////////////

    public Robot(final EntityType<?> type, final Level world) {
        super(type, world);
        this.blocksBuilding = true;
        setNoGravity(true);

        final CommonDeviceBusController busController = new CommonDeviceBusController(busElement, Config.robotEnergyPerTick);
        virtualMachine = new RobotVirtualMachine(busController);
        virtualMachine.state.builtinDevices.rtcMinecraft.setLevel(world);
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    public AnimationState getAnimationState() {
        return animationState;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public VMItemStackHandlers getItemStackHandlers() {
        return deviceItems;
    }

    @Override
    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public int getSelectedSlot() {
        return getEntityData().get(SELECTED_SLOT);
    }

    @Override
    public void setSelectedSlot(final int value) {
        getEntityData().set(SELECTED_SLOT, (byte) Mth.clamp(value, 0, INVENTORY_SIZE - 1));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        if (capability == Capabilities.itemHandler()) {
            return LazyOptional.of(() -> inventory).cast();
        }
        if (capability == Capabilities.energyStorage() && Config.robotsUseEnergy()) {
            return LazyOptional.of(() -> energy).cast();
        }
        if (capability == Capabilities.robot()) {
            return LazyOptional.of(() -> this).cast();
        }

        final LazyOptional<T> optional = super.getCapability(capability, side);
        if (optional.isPresent()) {
            return optional;
        }

        for (final Device device : virtualMachine.busController.getDevices()) {
            if (device instanceof final ICapabilityProvider capabilityProvider) {
                final LazyOptional<T> value = capabilityProvider.getCapability(capability, side);
                if (value.isPresent()) {
                    return value;
                }
            }
        }

        return LazyOptional.empty();
    }

    public long getLastPistonMovement() {
        return lastPistonMovement;
    }

    public void start() {
        if (!level.isClientSide()) {
            virtualMachine.start();
        }
    }

    public void stop() {
        if (!level.isClientSide()) {
            virtualMachine.stop();
        }
    }

    public void openTerminalScreen(final ServerPlayer player) {
        RobotTerminalContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void openInventoryScreen(final ServerPlayer player) {
        RobotInventoryContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void addTerminalUser(final Player player) {
        terminalUsers.add(player);
    }

    public void removeTerminalUser(final Player player) {
        terminalUsers.remove(player);
    }

    @Override
    public Iterable<Player> getTerminalUsers() {
        return terminalUsers;
    }

    public void dropSelf() {
        if (!isAlive()) {
            return;
        }

        final ItemStack stack = new ItemStack(Items.ROBOT.get());
        exportToItemStack(stack);
        spawnAtLocation(stack);

        discard();
        LevelUtils.playSound(level, blockPosition(), SoundType.METAL, SoundType::getBreakSound);
    }

    @Override
    public void tick() {
        final boolean isClient = level.isClientSide();

        if (firstTick) {
            if (isClient) {
                requestInitialState();
            } else {
                registerListeners();
                RobotActions.initializeData(this);
                if (actionProcessor.action != null) {
                    actionProcessor.action.initialize(this);
                }
            }
        }

        super.tick();

        if (isClient) {
            terminal.clientTick();
        }

        if (!isClient) {
            virtualMachine.tick();
        }

        actionProcessor.tick();

        if (!isClient && level instanceof final ServerLevel serverLevel) {
            final VoxelShape shape = Shapes.create(getBoundingBox());
            final Cursor3D iterator = getBlockPosIterator();
            while (iterator.advance()) {
                final int x = iterator.nextX();
                final int y = iterator.nextY();
                final int z = iterator.nextZ();
                mutablePosition.set(x, y, z);
                final BlockState blockState = serverLevel.getBlockState(mutablePosition);
                if (blockState.isAir() ||
                    blockState.is(Blocks.MOVING_PISTON) ||
                    blockState.is(Blocks.PISTON_HEAD)) {
                    continue;
                }

                final VoxelShape blockShape = blockState.getCollisionShape(serverLevel, mutablePosition);
                if (Shapes.joinIsNotEmpty(shape, blockShape.move(x, y, z), BooleanOp.AND)) {
                    final BlockEntity blockEntity = serverLevel.getBlockEntity(mutablePosition);
                    final LootContext.Builder builder = new LootContext.Builder(serverLevel)
                        .withRandom(serverLevel.random)
                        .withParameter(LootContextParams.THIS_ENTITY, this)
                        .withParameter(LootContextParams.ORIGIN, position())
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withParameter(LootContextParams.BLOCK_STATE, blockState)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
                    final List<ItemStack> drops = blockState.getDrops(builder);
                    serverLevel.setBlockAndUpdate(mutablePosition, Blocks.AIR.defaultBlockState());
                    for (final ItemStack drop : drops) {
                        Block.popResource(serverLevel, mutablePosition, drop);
                    }
                }
            }
        }
    }

    @Override
    public boolean skipAttackInteraction(final Entity entity) {
        if (entity instanceof Player player && player.isCreative()) {
            dropSelf();
        }
        return true;
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (Wrenches.isWrench(stack)) {
                if (player.isShiftKeyDown()) {
                    dropSelf();
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    openInventoryScreen(serverPlayer);
                }
            } else {
                if (player.isShiftKeyDown()) {
                    start();
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    openTerminalScreen(serverPlayer);
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void setRemoved(final RemovalReason reason) {
        super.setRemoved(reason);

        if (!level.isClientSide()) {
            // Full unload to release out-of-nbt persisted runtime-only data such as ram.
            virtualMachine.stop();
            virtualMachine.dispose();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canCollideWith(final Entity entity) {
        return entity != this;
    }

    @Override
    public void push(final Entity entity) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    public void exportToItemStack(final ItemStack stack) {
        final CompoundTag itemsTag = NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), MOD_TAG_NAME, ITEMS_TAG_NAME);
        deviceItems.saveItems(itemsTag); // Puts one tag per device type, as expected by TooltipUtils.
        itemsTag.put(INVENTORY_TAG_NAME, inventory.serializeNBT()); // Won't show up in tooltip.

        NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), MOD_TAG_NAME)
            .put(ENERGY_TAG_NAME, energy.serializeNBT());
    }

    public void importFromItemStack(final ItemStack stack) {
        final CompoundTag itemsTag = NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME, ITEMS_TAG_NAME);
        deviceItems.loadItems(itemsTag);
        inventory.deserializeNBT(itemsTag.getCompound(INVENTORY_TAG_NAME));

        energy.deserializeNBT(NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME, ENERGY_TAG_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void defineSynchedData() {
        final SynchedEntityData dataManager = getEntityData();
        dataManager.define(TARGET_POSITION, BlockPos.ZERO);
        dataManager.define(TARGET_DIRECTION, Direction.NORTH);
        dataManager.define(SELECTED_SLOT, (byte) 0);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        if (virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, virtualMachine.serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }

        tag.put(COMMAND_PROCESSOR_TAG_NAME, actionProcessor.serialize());
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.serialize());
        tag.put(ITEMS_TAG_NAME, deviceItems.saveItems());
        tag.put(DEVICES_TAG_NAME, deviceItems.saveDevices());
        tag.put(ENERGY_TAG_NAME, energy.serializeNBT());
        tag.put(INVENTORY_TAG_NAME, inventory.serializeNBT());
        tag.putByte(SELECTED_SLOT_TAG_NAME, getEntityData().get(SELECTED_SLOT));
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        actionProcessor.deserialize(tag.getCompound(COMMAND_PROCESSOR_TAG_NAME));
        busElement.deserialize(tag.getCompound(BUS_ELEMENT_TAG_NAME));
        deviceItems.loadItems(tag.getCompound(ITEMS_TAG_NAME));
        deviceItems.loadDevices(tag.getCompound(DEVICES_TAG_NAME));
        energy.deserializeNBT(tag.getCompound(ENERGY_TAG_NAME));
        inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG_NAME));
        setSelectedSlot(tag.getByte(SELECTED_SLOT_TAG_NAME));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void checkInsideBlocks() {
    }


    @Override
    protected Vec3 limitPistonMovement(final Vec3 pos) {
        lastPistonMovement = level.getGameTime();
        return super.limitPistonMovement(pos);
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    private void requestInitialState() {
        Network.sendToServer(new RobotInitializationRequestMessage(this));
    }

    private void registerListeners() {
        MinecraftForge.EVENT_BUS.addListener(chunkUnloadListener);
        MinecraftForge.EVENT_BUS.addListener(worldUnloadListener);
    }

    private void unregisterListeners() {
        MinecraftForge.EVENT_BUS.unregister(chunkUnloadListener);
        MinecraftForge.EVENT_BUS.unregister(worldUnloadListener);
    }

    private void handleChunkUnload(final ChunkEvent.Unload event) {
        if (event.getLevel() != level) {
            return;
        }

        final ChunkPos chunkPos = new ChunkPos(blockPosition());
        if (!Objects.equals(chunkPos, event.getChunk().getPos())) {
            return;
        }

        unregisterListeners();
        virtualMachine.suspend();
        virtualMachine.dispose();
    }

    private void handleWorldUnload(final LevelEvent.Unload event) {
        if (event.getLevel() != level) {
            return;
        }

        unregisterListeners();
        virtualMachine.suspend();
        virtualMachine.dispose();
    }

    private Cursor3D getBlockPosIterator() {
        final AABB bounds = getBoundingBox();
        return new Cursor3D(
            Mth.floor(bounds.minX), Mth.floor(bounds.minY), Mth.floor(bounds.minZ),
            Mth.floor(bounds.maxX), Mth.floor(bounds.maxY), Mth.floor(bounds.maxZ)
        );
    }

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

        public void update(final float deltaTime, final RandomSource random) {
            if (getVirtualMachine().isRunning() || actionProcessor.hasQueuedActions()) {
                topRenderHover = topRenderHover + deltaTime * HOVER_ANIMATION_SPEED;
                final float topOffsetY = Mth.sin(topRenderHover) / 32f;

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

    private static final class RobotActionProcessorResult {
        private static final String ACTION_ID_TAG_NAME = "action_id";
        private static final String RESULT_TAG_NAME = "result";

        public int actionId;
        public RobotActionResult result;

        public RobotActionProcessorResult(final int actionId, final RobotActionResult result) {
            this.actionId = actionId;
            this.result = result;
        }

        public RobotActionProcessorResult(final CompoundTag tag) {
            deserialize(tag);
        }

        public CompoundTag serialize() {
            final CompoundTag tag = new CompoundTag();

            tag.putInt(ACTION_ID_TAG_NAME, actionId);
            NBTUtils.putEnum(tag, RESULT_TAG_NAME, result);

            return tag;
        }

        public void deserialize(final CompoundTag tag) {
            actionId = tag.getInt(ACTION_ID_TAG_NAME);
            result = NBTUtils.getEnum(tag, RESULT_TAG_NAME, RobotActionResult.class);
        }
    }

    private final class RobotActionProcessor {
        private static final String QUEUE_TAG_NAME = "queue";
        private static final String ACTION_TAG_NAME = "action";
        private static final String RESULTS_TAG_NAME = "results";
        private static final String LAST_ACTION_ID_TAG_NAME = "last_action_id";

        private final Queue<AbstractRobotAction> queue = new ArrayDeque<>(MAX_QUEUED_ACTIONS - 1);
        @Nullable private AbstractRobotAction action;

        private final Queue<RobotActionProcessorResult> results = new ArrayDeque<>(MAX_QUEUED_RESULTS);
        private int lastActionId;

        public boolean hasQueuedActions() {
            return action != null || !queue.isEmpty();
        }

        public int getQueuedActionCount() {
            return (action != null ? 1 : 0) + queue.size();
        }

        public boolean move(final MovementDirection direction) {
            return addAction(new RobotMovementAction(direction));
        }

        public boolean rotate(final RotationDirection direction) {
            return addAction(new RobotRotationAction(direction));
        }

        public void tick() {
            if (level.isClientSide()) {
                RobotActions.performClient(Robot.this);
            } else {
                if (action != null) {
                    final RobotActionResult result = action.perform(Robot.this);
                    if (result != RobotActionResult.INCOMPLETE) {
                        synchronized (results) {
                            if (results.size() == MAX_QUEUED_RESULTS) {
                                results.remove();
                            }

                            results.add(new RobotActionProcessorResult(action.getId(), result));
                        }

                        action = null;
                    }
                }
                if (action == null) {
                    action = queue.poll();
                    if (action != null) {
                        action.initialize(Robot.this);
                    }
                }
                RobotActions.performServer(Robot.this, action);
            }
        }

        public void clear() {
            queue.clear();
            results.clear();
            lastActionId = 0;
        }

        public CompoundTag serialize() {
            final CompoundTag tag = new CompoundTag();

            final ListTag queueTag = new ListTag();
            for (final AbstractRobotAction action : queue) {
                queueTag.add(RobotActions.serialize(action));
            }
            tag.put(QUEUE_TAG_NAME, queueTag);

            if (action != null) {
                tag.put(ACTION_TAG_NAME, RobotActions.serialize(action));
            }

            final ListTag resultsTag = new ListTag();
            for (final RobotActionProcessorResult result : results) {
                resultsTag.add(result.serialize());
            }
            tag.put(RESULTS_TAG_NAME, resultsTag);

            tag.putInt(LAST_ACTION_ID_TAG_NAME, lastActionId);

            return tag;
        }

        public void deserialize(final CompoundTag tag) {
            queue.clear();
            results.clear();

            final ListTag queueTag = tag.getList(QUEUE_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < Math.min(queueTag.size(), MAX_QUEUED_ACTIONS - 1); i++) {
                final AbstractRobotAction action = RobotActions.deserialize(queueTag.getCompound(i));
                if (action != null) {
                    queue.add(action);
                }
            }

            action = RobotActions.deserialize(tag.getCompound(ACTION_TAG_NAME));

            final ListTag resultsTag = tag.getList(RESULTS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < Math.min(resultsTag.size(), MAX_QUEUED_RESULTS); i++) {
                final RobotActionProcessorResult result = new RobotActionProcessorResult(resultsTag.getCompound(i));
                if (result.actionId != 0) {
                    results.add(result);
                }
            }

            lastActionId = tag.getInt(LAST_ACTION_ID_TAG_NAME);
        }

        private boolean addAction(final AbstractRobotAction action) {
            if (level.isClientSide()) {
                return false;
            }

            if (!getVirtualMachine().isRunning()) {
                return false;
            }

            if (queue.size() < MAX_QUEUED_ACTIONS - 1) { // -1 for current action
                lastActionId = (lastActionId + 1) & 0x7FFFFFFF; // only positive ids; unlikely to ever wrap, but eh.
                action.setId(lastActionId);
                synchronized (queue) {
                    queue.add(action);
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private final class RobotItemStackHandlers extends AbstractVMItemStackHandlers {
        public RobotItemStackHandlers() {
            super(
                new GroupDefinition(DeviceTypes.MEMORY, MEMORY_SLOTS),
                new GroupDefinition(DeviceTypes.HARD_DRIVE, HARD_DRIVE_SLOTS),
                new GroupDefinition(DeviceTypes.FLASH_MEMORY, FLASH_MEMORY_SLOTS),
                new GroupDefinition(DeviceTypes.ROBOT_MODULE, MODULE_SLOTS)
            );
        }

        @Override
        protected ItemDeviceQuery makeQuery(final ItemStack stack) {
            return Devices.makeQuery(Robot.this, stack);
        }

        @Override
        protected void onChanged() {
            super.onChanged();
            if (!level.isClientSide()) {
                virtualMachine.busController.scheduleBusScan();
            }
        }
    }

    private final class RobotBusElement extends AbstractDeviceBusElement {
        private static final String DEVICE_ID_TAG_NAME = "device_id";

        private final Device device = new ObjectDevice(new RobotDevice(), "robot");
        private UUID deviceId = UUID.randomUUID();

        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return Optional.of(singleton(LazyOptional.of(() -> deviceItems.busElement)));
        }

        @Override
        public Collection<Device> getLocalDevices() {
            return singleton(device);
        }

        @Override
        public Optional<UUID> getDeviceIdentifier(final Device device) {
            if (device == this.device) {
                return Optional.of(deviceId);
            }
            return super.getDeviceIdentifier(device);
        }

        public CompoundTag serialize() {
            final CompoundTag tag = new CompoundTag();
            tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
            return tag;
        }

        public void deserialize(final CompoundTag tag) {
            if (tag.hasUUID(DEVICE_ID_TAG_NAME)) {
                deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
            }
        }
    }

    private final class RobotVMRunner extends AbstractTerminalVMRunner {
        public RobotVMRunner(final AbstractVirtualMachine virtualMachine, final Terminal terminal) {
            super(virtualMachine, terminal);
        }

        @Override
        protected void sendTerminalUpdateToClient(final ByteBuffer output) {
            Network.sendToClientsTrackingEntity(new RobotTerminalOutputMessage(Robot.this, output), Robot.this);
        }
    }

    private final class RobotVirtualMachine extends AbstractVirtualMachine {
        private RobotVirtualMachine(final CommonDeviceBusController busController) {
            super(busController);
            state.vmAdapter.setBaseAddressProvider(deviceItems::getDeviceAddressBase);
        }

        @Override
        protected boolean consumeEnergy(final int amount, final boolean simulate) {
            if (!Config.robotsUseEnergy()) {
                return true;
            }

            if (amount > energy.getEnergyStored()) {
                return false;
            }

            energy.extractEnergy(amount, simulate);
            return true;
        }

        @Override
        protected void stopRunnerAndReset() {
            super.stopRunnerAndReset();

            TerminalUtils.resetTerminal(terminal, output -> Network.sendToClientsTrackingEntity(
                new RobotTerminalOutputMessage(Robot.this, output), Robot.this));

            actionProcessor.clear();
        }

        @Override
        protected AbstractTerminalVMRunner createRunner() {
            return new RobotVMRunner(this, terminal);
        }

        @Override
        protected void handleBusStateChanged(final CommonDeviceBusController.BusState value) {
            Network.sendToClientsTrackingEntity(new RobotBusStateMessage(Robot.this, value), Robot.this);
        }

        @Override
        protected void handleRunStateChanged(final VMRunState value) {
            Network.sendToClientsTrackingEntity(new RobotRunStateMessage(Robot.this, value), Robot.this);
        }

        @Override
        protected void handleBootErrorChanged(@Nullable final Component value) {
            Network.sendToClientsTrackingEntity(new RobotBootErrorMessage(Robot.this, value), Robot.this);
        }
    }

    public final class RobotDevice {
        @Callback(synchronize = false)
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Callback(synchronize = false)
        public int getEnergyCapacity() {
            return energy.getMaxEnergyStored();
        }

        @Callback(synchronize = false)
        public int getSelectedSlot() {
            return Robot.this.getSelectedSlot();
        }

        @Callback(synchronize = false)
        public void setSelectedSlot(@Parameter("slot") final int slot) {
            Robot.this.setSelectedSlot(slot);
        }

        @Callback
        public ItemStack getStackInSlot(@Parameter("slot") final int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Callback(synchronize = false)
        public boolean move(@Parameter("direction") @Nullable final MovementDirection direction) {
            if (direction == null) throw new IllegalArgumentException();
            return actionProcessor.move(direction);
        }

        @Callback(synchronize = false)
        public boolean turn(@Parameter("direction") @Nullable final RotationDirection direction) {
            if (direction == null) throw new IllegalArgumentException();
            return actionProcessor.rotate(direction);
        }

        @Callback(synchronize = false)
        public int getLastActionId() {
            return actionProcessor.lastActionId;
        }

        @Callback(synchronize = false)
        public int getQueuedActionCount() {
            return actionProcessor.getQueuedActionCount();
        }

        @Nullable
        @Callback(synchronize = false)
        public RobotActionResult getActionResult(@Parameter("actionId") final int actionId) {
            final AbstractRobotAction currentAction = actionProcessor.action;
            if (currentAction != null && currentAction.getId() == actionId) {
                return RobotActionResult.INCOMPLETE;
            }
            synchronized (actionProcessor.queue) {
                for (final AbstractRobotAction action : actionProcessor.queue) {
                    if (action.getId() == actionId) {
                        return RobotActionResult.INCOMPLETE;
                    }
                }
            }
            synchronized (actionProcessor.results) {
                for (final RobotActionProcessorResult result : actionProcessor.results) {
                    if (result.actionId == actionId) {
                        return result.result;
                    }
                }
            }

            return null;
        }

        private RobotDevice() {
        }
    }
}
