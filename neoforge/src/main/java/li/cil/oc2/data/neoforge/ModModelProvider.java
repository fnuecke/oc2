/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import com.mojang.math.Quadrant;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.API;
import li.cil.oc2.client.model.neoforge.BusCableModel;
import li.cil.oc2.client.renderer.entity.RobotSpecialRenderer;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.FlippableOrientableBlock;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.item.ColoredItem;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.List;

public final class ModModelProvider extends ModelProvider {
    private static final Identifier CABLE_MODEL = modelLocation("block/cable_base");
    private static final Identifier CABLE_LINK_MODEL = modelLocation("block/cable_link");
    private static final Identifier CABLE_PLUG_MODEL = modelLocation("block/cable_plug");
    private static final Identifier CABLE_STRAIGHT_MODEL = modelLocation("block/cable_straight");
    private static final Identifier CHARGER_MODEL = modelLocation("block/charger");
    private static final Identifier COMPUTER_MODEL = modelLocation("block/computer");
    private static final Identifier DISK_DRIVE_MODEL = modelLocation("block/disk_drive");
    private static final Identifier FLASH_DRIVE_MODEL = modelLocation("block/flash_drive");
    private static final Identifier KEYBOARD_MODEL = modelLocation("block/keyboard");
    private static final Identifier NETWORK_CONNECTOR_MODEL = modelLocation("block/network_connector");
    private static final Identifier NETWORK_CONNECTOR_INDICATOR_MODEL = modelLocation("block/network_connector_indicator");
    private static final Identifier NETWORK_HUB_MODEL = modelLocation("block/network_hub");
    private static final Identifier INTERNET_GATEWAY_MODEL = modelLocation("block/internet_gateway");
    private static final Identifier PROJECTOR_MODEL = modelLocation("block/projector");
    private static final Identifier REDSTONE_INTERFACE_MODEL = modelLocation("block/redstone_interface");
    private static final Identifier TERMINAL_MODEL = modelLocation("block/terminal");
    private static final Identifier TRANSPOSER_MODEL = modelLocation("block/transposer");
    private static final Identifier TRANSPOSER_GLOW_MODEL = modelLocation("block/transposer_glow");

    // --------------------------------------------------------------------- //

    public ModModelProvider(final PackOutput output) {
        super(output, API.MOD_ID);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void registerModels(final BlockModelGenerators blocks, final ItemModelGenerators items) {
        horizontalBlock(blocks, Blocks.CHARGER, Items.CHARGER, CHARGER_MODEL);
        horizontalBlock(blocks, Blocks.COMPUTER, Items.COMPUTER, COMPUTER_MODEL);
        cubeBlock(blocks, Blocks.CREATIVE_ENERGY, Items.CREATIVE_ENERGY);
        horizontalBlock(blocks, Blocks.DISK_DRIVE, Items.DISK_DRIVE, DISK_DRIVE_MODEL);
        orientableBlock(blocks, Blocks.FLASH_DRIVE, Items.FLASH_DRIVE, FLASH_DRIVE_MODEL);
        horizontalBlock(blocks, Blocks.KEYBOARD, Items.KEYBOARD, KEYBOARD_MODEL);
        networkConnector(blocks);
        horizontalBlock(blocks, Blocks.NETWORK_HUB, Items.NETWORK_HUB, NETWORK_HUB_MODEL);
        horizontalBlock(blocks, Blocks.INTERNET_GATEWAY, Items.INTERNET_GATEWAY, INTERNET_GATEWAY_MODEL);
        flippableOrientableBlock(blocks, Blocks.PROJECTOR, Items.PROJECTOR, PROJECTOR_MODEL);
        horizontalBlock(blocks, Blocks.REDSTONE_INTERFACE, Items.REDSTONE_INTERFACE, REDSTONE_INTERFACE_MODEL);
        orientableBlock(blocks, Blocks.TERMINAL, Items.TERMINAL, TERMINAL_MODEL);
        transposer(blocks);

        busCable(blocks);

        for (final RegistrySupplier<? extends Item> item : List.of(
            Items.WRENCH, Items.MANUAL, Items.NETWORK_CABLE,
            Items.CPU_RISCV, Items.CPU_Z80,
            Items.MEMORY_SMALL, Items.MEMORY_MEDIUM, Items.MEMORY_LARGE,
            Items.REDSTONE_INTERFACE_CARD, Items.SERIAL_INTERFACE_CARD, Items.NETWORK_INTERFACE_CARD,
            Items.NETWORK_TUNNEL_CARD, Items.FILE_IMPORT_EXPORT_CARD, Items.SOUND_CARD,
            Items.INVENTORY_OPERATIONS_MODULE, Items.TANK_OPERATIONS_MODULE, Items.SCANNER_MODULE, Items.CRAFTING_MODULE, Items.BLOCK_OPERATIONS_MODULE, Items.NETWORK_TUNNEL_MODULE,
            Items.TRANSISTOR, Items.CIRCUIT_BOARD)) {
            flatItem(blocks, item, "item/" + item.getId().getPath());
        }

        tintedItem(blocks, Items.HARD_DRIVE_SMALL, "item/hard_drive_base", "item/hard_drive_tint");
        tintedItem(blocks, Items.HARD_DRIVE_MEDIUM, "item/hard_drive_base", "item/hard_drive_tint");
        tintedItem(blocks, Items.HARD_DRIVE_LARGE, "item/hard_drive_base", "item/hard_drive_tint");
        tintedItem(blocks, Items.FLASH_MEMORY, "item/flash_memory_base", "item/flash_memory_tint");
        tintedItem(blocks, Items.FLOPPY, "item/floppy_base", "item/floppy_tint");

        blocks.itemModelOutput.accept(Items.ROBOT.get(), ItemModelUtils.specialModel(
            Identifier.withDefaultNamespace("item/template_shulker_box"), new RobotSpecialRenderer.Unbaked()));
    }

    // --------------------------------------------------------------------- //

    private static void busCable(final BlockModelGenerators blocks) {
        final MultiPartGenerator generator = MultiPartGenerator.multiPart(Blocks.BUS_CABLE.get());

        // NB: We use a custom block state model to replace the base part with straight parts and
        //     insert supports where appropriate, as well as for replacing it with a facade block model.
        generator.with(MultiVariant.of(new CustomBlockStateModelBuilder.Simple(
            new BusCableModel(BlockModelGenerators.plainVariant(CABLE_MODEL).toUnbaked()))));

        BusCableBlock.FACING_TO_CONNECTION_MAP.forEach((direction, connectionType) -> {
            final Quadrant x = switch (direction) {
                case UP -> Quadrant.R90;
                case DOWN -> Quadrant.R270;
                default -> Quadrant.R0;
            };
            final VariantMutator rotation = VariantMutator.X_ROT.withValue(x)
                .then(VariantMutator.Y_ROT.withValue(quadrant((int) direction.toYRot())));

            generator.with(new ConditionBuilder()
                    .term(connectionType, BusCableBlock.ConnectionType.CABLE)
                    .term(BusCableBlock.HAS_FACADE, false),
                BlockModelGenerators.plainVariant(CABLE_LINK_MODEL).with(rotation));
            generator.with(new ConditionBuilder()
                    .term(connectionType, BusCableBlock.ConnectionType.INTERFACE)
                    .term(BusCableBlock.HAS_FACADE, false),
                BlockModelGenerators.plainVariant(CABLE_PLUG_MODEL).with(rotation));
        });

        blocks.blockStateOutput.accept(generator);

        final Identifier cableItemModel = ExtendedModelTemplateBuilder.builder()
            .parent(CABLE_STRAIGHT_MODEL)
            .transform(ItemDisplayContext.GUI, t -> t.rotation(30, 225, 0).scale(0.75f))
            .transform(ItemDisplayContext.GROUND, t -> t.translation(0, 3, 0).scale(0.75f))
            .transform(ItemDisplayContext.FIXED, t -> t.rotation(0, 180, 0).scale(1.0f))
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, t -> t.rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.75f))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, t -> t.rotation(0, 45, 0).scale(0.75f))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, t -> t.rotation(0, 225, 0).scale(0.75f))
            .build()
            .create(modelLocation("item/" + Items.BUS_CABLE.getId().getPath()), new TextureMapping(), blocks.modelOutput);
        blocks.itemModelOutput.accept(Items.BUS_CABLE.get(), ItemModelUtils.plainModel(cableItemModel));

        final Identifier interfaceItemModel = ExtendedModelTemplateBuilder.builder()
            .parent(CABLE_PLUG_MODEL)
            .transform(ItemDisplayContext.GUI, t -> t.rotation(30, 315, 0).translation(2, 1, 0).scale(0.75f))
            .transform(ItemDisplayContext.GROUND, t -> t.translation(0, 3, -5).scale(0.75f))
            .transform(ItemDisplayContext.FIXED, t -> t.rotation(0, 180, 0).translation(0, 0, 4).scale(1.0f))
            .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, t -> t.rotation(75, 180, 0).translation(0, -1, 0).scale(0.75f))
            .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, t -> t.rotation(0, 180, 0).translation(0, 0, 2).scale(0.75f))
            .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, t -> t.rotation(0, 180, 0).translation(0, 0, 2).scale(0.75f))
            .build()
            .create(modelLocation("item/" + Items.BUS_INTERFACE.getId().getPath()), new TextureMapping(), blocks.modelOutput);
        blocks.itemModelOutput.accept(Items.BUS_INTERFACE.get(), ItemModelUtils.plainModel(interfaceItemModel));
    }

    private static void networkConnector(final BlockModelGenerators blocks) {
        final MultiPartGenerator generator = MultiPartGenerator.multiPart(Blocks.NETWORK_CONNECTOR.get());
        for (final AttachFace face : AttachFace.values()) {
            for (final Direction facing : Direction.Plane.HORIZONTAL) {
                final int x = face == AttachFace.CEILING ? 180 : face == AttachFace.WALL ? 90 : 0;
                final int y = (int) facing.toYRot() + 180 + (face == AttachFace.CEILING ? 180 : 0);
                generator.with(new ConditionBuilder()
                        .term(BlockStateProperties.ATTACH_FACE, face)
                        .term(BlockStateProperties.HORIZONTAL_FACING, facing),
                    rotated(NETWORK_CONNECTOR_MODEL, x, y));
                generator.with(new ConditionBuilder()
                        .term(BlockStateProperties.ATTACH_FACE, face)
                        .term(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .term(NetworkConnectorBlock.CONNECTED, true),
                    rotated(NETWORK_CONNECTOR_INDICATOR_MODEL, x, y));
            }
        }
        blocks.blockStateOutput.accept(generator);

        final Identifier itemModel = ExtendedModelTemplateBuilder.builder()
            .parent(NETWORK_CONNECTOR_MODEL)
            .transform(ItemDisplayContext.GUI, t -> t.rotation(30, 315, 0).translation(0, 2, 0).scale(0.75f, 0.75f, 0.75f))
            .transform(ItemDisplayContext.FIXED, t -> t.rotation(270, 0, 0).translation(0, 0, -5).scale(1, 1, 1))
            .build()
            .create(modelLocation("item/" + Items.NETWORK_CONNECTOR.getId().getPath()), new TextureMapping(), blocks.modelOutput);
        blocks.itemModelOutput.accept(Items.NETWORK_CONNECTOR.get(), ItemModelUtils.plainModel(itemModel));
    }

    private static void transposer(final BlockModelGenerators blocks) {
        blocks.blockStateOutput.accept(MultiPartGenerator.multiPart(Blocks.TRANSPOSER.get())
            .with(BlockModelGenerators.plainVariant(TRANSPOSER_MODEL))
            .with(BlockModelGenerators.plainVariant(TRANSPOSER_GLOW_MODEL)));
        blocks.itemModelOutput.accept(Items.TRANSPOSER.get(), ItemModelUtils.plainModel(TRANSPOSER_MODEL));
    }

    private static <T extends OrientableBlock> void orientableBlock(final BlockModelGenerators blocks, final RegistrySupplier<T> block,
                                                                    final RegistrySupplier<Item> item, final Identifier model) {
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
            .with(PropertyDispatch.initial(OrientableBlock.FACING, OrientableBlock.ROTATION)
                .generate((facing, rotation) -> {
                    final int x = switch (facing) {
                        case DOWN -> 90;
                        case UP -> 270;
                        default -> 0;
                    };
                    final int y = facing.getAxis().isVertical() ? rotation * 90 : (int) facing.getOpposite().toYRot();
                    return rotated(model, x, y);
                })));
        blocks.itemModelOutput.accept(item.get(), ItemModelUtils.plainModel(model));
    }

    private static <T extends FlippableOrientableBlock> void flippableOrientableBlock(final BlockModelGenerators blocks, final RegistrySupplier<T> block,
                                                                                      final RegistrySupplier<Item> item, final Identifier model) {
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
            .with(PropertyDispatch.initial(FlippableOrientableBlock.ORIENTATION)
                .generate(orientation -> rotated(model, orientation.getRotationX(), orientation.getRotationY()))));
        blocks.itemModelOutput.accept(item.get(), ItemModelUtils.plainModel(model));
    }

    private static <T extends Block> void horizontalBlock(final BlockModelGenerators blocks, final RegistrySupplier<T> block,
                                                          final RegistrySupplier<Item> item, final Identifier model) {
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
            .with(PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING)
                .generate(facing -> rotated(model, 0, (int) facing.toYRot() + 180))));
        blocks.itemModelOutput.accept(item.get(), ItemModelUtils.plainModel(model));
    }

    private static <T extends Block> void cubeBlock(final BlockModelGenerators blocks, final RegistrySupplier<T> block,
                                                    final RegistrySupplier<Item> item) {
        final Identifier model = ModelTemplates.CUBE_ALL.create(block.get(), TextureMapping.cube(block.get()), blocks.modelOutput);
        blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block.get(), BlockModelGenerators.plainVariant(model)));
        blocks.itemModelOutput.accept(item.get(), ItemModelUtils.plainModel(model));
    }

    private static void flatItem(final BlockModelGenerators blocks, final RegistrySupplier<? extends Item> item, final String texture) {
        final Identifier model = ModelTemplates.FLAT_ITEM.create(
            modelLocation("item/" + item.getId().getPath()),
            new TextureMapping().put(TextureSlot.LAYER0, modelLocation(texture)),
            blocks.modelOutput);
        blocks.itemModelOutput.accept(item.get(), ItemModelUtils.plainModel(model));
    }

    private static void tintedItem(final BlockModelGenerators blocks, final RegistrySupplier<? extends Item> item,
                                   final String baseTexture, final String tintTexture) {
        final Identifier model = ModelTemplates.TWO_LAYERED_ITEM.create(
            modelLocation("item/" + item.getId().getPath()),
            new TextureMapping()
                .put(TextureSlot.LAYER0, modelLocation(baseTexture))
                .put(TextureSlot.LAYER1, modelLocation(tintTexture)),
            blocks.modelOutput);
        final int defaultColor = ((ColoredItem) item.get()).getColor(new ItemStack(item.get()));
        blocks.itemModelOutput.accept(item.get(), ItemModelUtils.tintedModel(model,
            ItemModelUtils.constantTint(-1), new Dye(defaultColor)));
    }

    private static MultiVariant rotated(final Identifier model, final int x, final int y) {
        return BlockModelGenerators.plainVariant(model)
            .with(VariantMutator.X_ROT.withValue(quadrant(x)))
            .with(VariantMutator.Y_ROT.withValue(quadrant(y)));
    }

    private static Quadrant quadrant(final int degrees) {
        return Quadrant.values()[Math.floorMod(degrees / 90, 4)];
    }

    private static Identifier modelLocation(final String path) {
        return Identifier.fromNamespaceAndPath(API.MOD_ID, path);
    }
}
