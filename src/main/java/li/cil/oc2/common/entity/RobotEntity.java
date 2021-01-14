package li.cil.oc2.common.entity;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.client.gui.RobotTerminalScreen;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.container.RobotContainer;
import li.cil.oc2.common.entity.robot.*;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.*;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.WorldUtils;
import li.cil.oc2.common.vm.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

public final class RobotEntity extends Entity {
    public static final DataParameter<BlockPos> TARGET_POSITION = EntityDataManager.createKey(RobotEntity.class, DataSerializers.BLOCK_POS);
    public static final DataParameter<Direction> TARGET_DIRECTION = EntityDataManager.createKey(RobotEntity.class, DataSerializers.DIRECTION);

    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String BUS_ELEMENT_TAG_NAME = "bus_element";
    private static final String COMMAND_PROCESSOR_TAG_NAME = "commands";

    private static final int MAX_QUEUED_ACTIONS = 16;
    private static final int MAX_QUEUED_RESULTS = 16;

    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 2;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int MODULE_SLOTS = 4;

    ///////////////////////////////////////////////////////////////////

    private final Consumer<ChunkEvent.Unload> chunkUnloadListener = this::handleChunkUnload;
    private final Consumer<WorldEvent.Unload> worldUnloadListener = this::handleWorldUnload;
    private final BlockPos.Mutable mutablePosition = new BlockPos.Mutable();

    private final AnimationState animationState = new AnimationState();
    private final RobotActionProcessor actionProcessor = new RobotActionProcessor();
    private final Terminal terminal = new Terminal();
    private final RobotVirtualMachineState state;
    private final RobotItemStackHandlers items = new RobotItemStackHandlers();
    private final RobotBusElement busElement = new RobotBusElement();

    ///////////////////////////////////////////////////////////////////

    public RobotEntity(final EntityType<?> type, final World world) {
        super(type, world);
        this.preventEntitySpawning = true;
        setNoGravity(true);

        final RobotBusController busController = new RobotBusController(busElement);
        state = new RobotVirtualMachineState(busController, new CommonVirtualMachine(busController));
        state.virtualMachine.rtcMinecraft.setWorld(world);

        items.busElement.addDevice(new ObjectDevice(new RobotDevice(), "robot"));
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    public AnimationState getAnimationState() {
        return animationState;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public VirtualMachineState getState() {
        return state;
    }

    public VirtualMachineItemStackHandlers getItemStackHandlers() {
        return items;
    }

    public void start() {
        final World world = getEntityWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        state.start();
    }

    public void stop() {
        final World world = getEntityWorld();
        if (world == null || world.isRemote()) {
            return;
        }

        state.stop();
    }

    @Override
    public void tick() {
        final World world = getEntityWorld();
        final boolean isClient = world.isRemote();

        if (firstUpdate) {
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

        if (!isClient) {
            state.tick();
        }

        actionProcessor.tick();

        if (!isClient && world instanceof ServerWorld) {
            final VoxelShape shape = VoxelShapes.create(getBoundingBox());
            final CubeCoordinateIterator iterator = getBlockPosIterator();
            while (iterator.hasNext()) {
                final int x = iterator.getX();
                final int y = iterator.getY();
                final int z = iterator.getZ();
                mutablePosition.setPos(x, y, z);
                final BlockState blockState = world.getBlockState(mutablePosition);
                if (blockState.isAir(world, mutablePosition)) {
                    continue;
                }

                final VoxelShape blockShape = blockState.getCollisionShape(world, mutablePosition);
                if (VoxelShapes.compare(shape, blockShape.withOffset(x, y, z), IBooleanFunction.AND)) {
                    final TileEntity tileEntity = blockState.hasTileEntity() ? world.getTileEntity(mutablePosition) : null;
                    final LootContext.Builder builder = new LootContext.Builder((ServerWorld) world)
                            .withRandom(world.rand)
                            .withParameter(LootParameters.field_237457_g_, Vector3d.copyCentered(mutablePosition))
                            .withParameter(LootParameters.TOOL, ItemStack.EMPTY)
                            .withNullableParameter(LootParameters.BLOCK_ENTITY, tileEntity)
                            .withNullableParameter(LootParameters.THIS_ENTITY, this);
                    final List<ItemStack> drops = blockState.getDrops(builder);
                    world.setBlockState(mutablePosition, Blocks.AIR.getDefaultState(), 3);
                    for (final ItemStack drop : drops) {
                        Block.spawnAsEntity(world, mutablePosition, drop);
                    }
                }
            }
        }
    }

    private CubeCoordinateIterator getBlockPosIterator() {
        final AxisAlignedBB bounds = getBoundingBox();
        return new CubeCoordinateIterator(
                MathHelper.floor(bounds.minX), MathHelper.floor(bounds.minY), MathHelper.floor(bounds.minZ),
                MathHelper.floor(bounds.maxX), MathHelper.floor(bounds.maxY), MathHelper.floor(bounds.maxZ)
        );
    }

    @Override
    public ActionResultType processInitialInteract(final PlayerEntity player, final Hand hand) {
        final ItemStack stack = player.getHeldItem(hand);
        if (Wrenches.isWrench(stack)) {
            if (!world.isRemote() && player instanceof ServerPlayerEntity) {
                if (player.isSneaking()) {
                    remove();
                    WorldUtils.playSound(world, getPosition(), SoundType.METAL, SoundType::getBreakSound);
                } else {
                    openContainerScreen(player);
                }
            }
        } else {
            if (player.isSneaking()) {
                start();
            } else if (world.isRemote()) {
                openTerminalScreen();
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(final boolean keepData) {
        super.remove(keepData);

        handleUnload();

        // Full unload to release out-of-nbt persisted runtime-only data such as ram.
        state.virtualMachine.vmAdapter.unload();

        // TODO drop self as item
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
        RobotActions.registerData(getDataManager());
    }

    @Override
    protected void writeAdditional(final CompoundNBT tag) {
        tag.put(STATE_TAG_NAME, state.serialize());
        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.put(COMMAND_PROCESSOR_TAG_NAME, actionProcessor.serialize());
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.serialize());
        tag.put(Constants.INVENTORY_TAG_NAME, items.serialize());
    }

    @Override
    protected void readAdditional(final CompoundNBT tag) {
        state.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        actionProcessor.deserialize(tag.getCompound(COMMAND_PROCESSOR_TAG_NAME));
        busElement.deserialize(tag.getCompound(BUS_ELEMENT_TAG_NAME));

        if (tag.contains(Constants.INVENTORY_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            items.deserialize(tag.getCompound(Constants.INVENTORY_TAG_NAME));
        }
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void doBlockCollisions() {
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    private void requestInitialState() {
        Network.INSTANCE.sendToServer(new RobotInitializationRequestMessage(this));
    }

    @OnlyIn(Dist.CLIENT)
    private void openTerminalScreen() {
        Minecraft.getInstance().displayGuiScreen(new RobotTerminalScreen(this, getName()));
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
        if (event.getWorld() != getEntityWorld()) {
            return;
        }

        final ChunkPos chunkPos = new ChunkPos(getPosition());
        if (!Objects.equals(chunkPos, event.getChunk().getPos())) {
            return;
        }

        unregisterListeners();
        handleUnload();
    }

    private void handleWorldUnload(final WorldEvent.Unload event) {
        if (event.getWorld() != getEntityWorld()) {
            return;
        }

        unregisterListeners();
        handleUnload();
    }

    private void handleUnload() {
        state.joinVirtualMachine();
        state.virtualMachine.vmAdapter.suspend();
        state.busController.dispose();
    }

    private void openContainerScreen(final PlayerEntity player) {
        NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return getName();
            }

            @Override
            public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
                return new RobotContainer(id, RobotEntity.this, inventory);
            }
        }, b -> b.writeVarInt(getEntityId()));
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

        public void update(final float deltaTime, final Random random) {
            if (getState().isRunning() || actionProcessor.hasQueuedActions()) {
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

    private static final class RobotActionProcessorResult {
        private static final String ACTION_ID_TAG_NAME = "action_id";
        private static final String RESULT_TAG_NAME = "result";

        public int actionId;
        public RobotActionResult result;

        public RobotActionProcessorResult(final int actionId, final RobotActionResult result) {
            this.actionId = actionId;
            this.result = result;
        }

        public RobotActionProcessorResult(final CompoundNBT tag) {
            deserialize(tag);
        }

        public CompoundNBT serialize() {
            final CompoundNBT tag = new CompoundNBT();

            tag.putInt(ACTION_ID_TAG_NAME, actionId);
            NBTUtils.putEnum(tag, RESULT_TAG_NAME, result);

            return tag;
        }

        public void deserialize(final CompoundNBT tag) {
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
            if (getEntityWorld().isRemote()) {
                RobotActions.performClient(RobotEntity.this);
            } else {
                if (action != null) {
                    final RobotActionResult result = action.perform(RobotEntity.this);
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
                        action.initialize(RobotEntity.this);
                    }
                }
            }
        }

        public void clear() {
            queue.clear();
            results.clear();
            lastActionId = 0;
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

            final ListNBT resultsTag = new ListNBT();
            for (final RobotActionProcessorResult result : results) {
                resultsTag.add(result.serialize());
            }
            tag.put(RESULTS_TAG_NAME, resultsTag);

            tag.putInt(LAST_ACTION_ID_TAG_NAME, lastActionId);

            return tag;
        }

        public void deserialize(final CompoundNBT tag) {
            queue.clear();
            results.clear();

            final ListNBT queueTag = tag.getList(QUEUE_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < Math.min(queueTag.size(), MAX_QUEUED_ACTIONS - 1); i++) {
                final AbstractRobotAction action = RobotActions.deserialize(queueTag.getCompound(i));
                if (action != null) {
                    queue.add(action);
                }
            }

            action = RobotActions.deserialize(tag.getCompound(ACTION_TAG_NAME));

            final ListNBT resultsTag = tag.getList(RESULTS_TAG_NAME, NBTTagIds.TAG_COMPOUND);
            for (int i = 0; i < Math.min(resultsTag.size(), MAX_QUEUED_RESULTS); i++) {
                final RobotActionProcessorResult result = new RobotActionProcessorResult(resultsTag.getCompound(i));
                if (result.actionId != 0) {
                    results.add(result);
                }
            }

            lastActionId = tag.getInt(LAST_ACTION_ID_TAG_NAME);
        }

        private boolean addAction(final AbstractRobotAction action) {
            if (getEntityWorld().isRemote()) {
                return false;
            }

            if (!getState().isRunning()) {
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

    private final class RobotItemStackHandlers extends AbstractVirtualMachineItemStackHandlers {
        public RobotItemStackHandlers() {
            super(
                    GroupDefinition.of(DeviceTypes.MEMORY, MEMORY_SLOTS),
                    GroupDefinition.of(DeviceTypes.HARD_DRIVE, HARD_DRIVE_SLOTS),
                    GroupDefinition.of(DeviceTypes.FLASH_MEMORY, FLASH_MEMORY_SLOTS),
                    GroupDefinition.of(DeviceTypes.ROBOT_MODULE, MODULE_SLOTS)
            );
        }

        @Override
        protected List<ItemDeviceInfo> getDevices(final ItemStack stack) {
            return Devices.getDevices(RobotEntity.this, stack);
        }
    }

    private final class RobotBusElement extends AbstractDeviceBusElement {
        private static final String DEVICE_ID_TAG_NAME = "device_id";

        private final Device device = new ObjectDevice(new RobotDevice(), "robot");
        private UUID deviceId = UUID.randomUUID();

        @Override
        public Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors() {
            return Optional.of(Collections.singleton(LazyOptional.of(() -> items.busElement)));
        }

        @Override
        public Collection<Device> getLocalDevices() {
            return Collections.singleton(device);
        }

        @Override
        public Optional<UUID> getDeviceIdentifier(final Device device) {
            if (device == this.device) {
                return Optional.of(deviceId);
            }
            return super.getDeviceIdentifier(device);
        }

        public CompoundNBT serialize() {
            final CompoundNBT tag = new CompoundNBT();
            tag.putUniqueId(DEVICE_ID_TAG_NAME, deviceId);
            return tag;
        }

        public void deserialize(final CompoundNBT tag) {
            if (tag.hasUniqueId(DEVICE_ID_TAG_NAME)) {
                deviceId = tag.getUniqueId(DEVICE_ID_TAG_NAME);
            }
        }
    }

    private final class RobotBusController extends AbstractDeviceBusController {
        public RobotBusController(final DeviceBusElement root) {
            super(root);
        }

        @Override
        protected void onBeforeScan() {
            state.reload();
            state.virtualMachine.rpcAdapter.pause();
        }

        @Override
        protected void onAfterDeviceScan(final boolean didDevicesChange) {
            state.virtualMachine.rpcAdapter.resume(didDevicesChange);
        }

        @Override
        protected void onDevicesAdded(final Collection<Device> devices) {
            state.virtualMachine.vmAdapter.addDevices(devices);
        }

        @Override
        protected void onDevicesRemoved(final Collection<Device> devices) {
            state.virtualMachine.vmAdapter.removeDevices(devices);
        }
    }

    private final class RobotVirtualMachineRunner extends AbstractTerminalVirtualMachineRunner {
        public RobotVirtualMachineRunner(final CommonVirtualMachine virtualMachine, final Terminal terminal) {
            super(virtualMachine, terminal);
        }

        @Override
        protected void sendTerminalUpdateToClient(final ByteBuffer output) {
            final RobotTerminalOutputMessage message = new RobotTerminalOutputMessage(RobotEntity.this, output);
            Network.sendToClientsTrackingEntity(message, RobotEntity.this);
        }
    }

    private final class RobotVirtualMachineState extends AbstractVirtualMachineState<RobotBusController, CommonVirtualMachine> {
        private RobotVirtualMachineState(final RobotBusController busController, final CommonVirtualMachine virtualMachine) {
            super(busController, virtualMachine);
            virtualMachine.vmAdapter.setDefaultAddressProvider(items::getDefaultDeviceAddress);
        }

        @Override
        protected AbstractTerminalVirtualMachineRunner createRunner() {
            return new RobotVirtualMachineRunner(virtualMachine, terminal);
        }

        @Override
        public void stopRunnerAndReset() {
            super.stopRunnerAndReset();

            actionProcessor.clear();
        }

        @Override
        protected void handleBusStateChanged(final AbstractDeviceBusController.BusState value) {
            Network.sendToClientsTrackingEntity(new RobotBusStateMessage(RobotEntity.this), RobotEntity.this);
        }

        @Override
        protected void handleRunStateChanged(final RunState value) {
            Network.sendToClientsTrackingEntity(new RobotRunStateMessage(RobotEntity.this), RobotEntity.this);
        }

        @Override
        protected void handleBootErrorChanged(@Nullable final ITextComponent value) {
            Network.sendToClientsTrackingEntity(new RobotBootErrorMessage(RobotEntity.this), RobotEntity.this);
        }
    }

    public final class RobotDevice {
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
