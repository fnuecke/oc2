/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockStateProvider extends BlockStateProvider {
    private static final ResourceLocation CABLE_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_base");
    private static final ResourceLocation CABLE_LINK_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_link");
    private static final ResourceLocation CABLE_PLUG_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_plug");
    private static final ResourceLocation CABLE_STRAIGHT_MODEL = new ResourceLocation(API.MOD_ID, "block/cable_straight");
    private static final ResourceLocation CHARGER_MODEL = new ResourceLocation(API.MOD_ID, "block/charger");
    private static final ResourceLocation COMPUTER_MODEL = new ResourceLocation(API.MOD_ID, "block/computer");
    private static final ResourceLocation DISK_DRIVE_MODEL = new ResourceLocation(API.MOD_ID, "block/disk_drive");
    private static final ResourceLocation KEYBOARD_MODEL = new ResourceLocation(API.MOD_ID, "block/keyboard");
    private static final ResourceLocation NETWORK_CONNECTOR_MODEL = new ResourceLocation(API.MOD_ID, "block/network_connector");
    private static final ResourceLocation NETWORK_HUB_MODEL = new ResourceLocation(API.MOD_ID, "block/network_hub");
    private static final ResourceLocation PROJECTOR_MODEL = new ResourceLocation(API.MOD_ID, "block/projector");
    private static final ResourceLocation REDSTONE_INTERFACE_MODEL = new ResourceLocation(API.MOD_ID, "block/redstone_interface");

    public ModBlockStateProvider(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(Blocks.CHARGER, Items.CHARGER, CHARGER_MODEL);
        horizontalBlock(Blocks.COMPUTER, Items.COMPUTER, COMPUTER_MODEL);
        simpleBlock(Blocks.CREATIVE_ENERGY, Items.CREATIVE_ENERGY);
        horizontalBlock(Blocks.DISK_DRIVE, Items.DISK_DRIVE, DISK_DRIVE_MODEL);
        horizontalBlock(Blocks.KEYBOARD, Items.KEYBOARD, KEYBOARD_MODEL);
        horizontalFaceBlock(Blocks.NETWORK_CONNECTOR, Items.NETWORK_CONNECTOR, NETWORK_CONNECTOR_MODEL)
            .transforms()
            .transform(ItemTransforms.TransformType.GUI)
            .rotation(30, 315, 0)
            .translation(0, 2, 0)
            .scale(0.75f, 0.75f, 0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIXED)
            .rotation(270, 0, 0)
            .translation(0, 0, -5)
            .scale(1, 1, 1)
            .end()
            .end();
        horizontalBlock(Blocks.NETWORK_HUB, Items.NETWORK_HUB, NETWORK_HUB_MODEL);
        horizontalBlock(Blocks.NETWORK_SWITCH, Items.NETWORK_SWITCH, NETWORK_HUB_MODEL);
        horizontalBlock(Blocks.PROJECTOR, Items.PROJECTOR, PROJECTOR_MODEL);
        horizontalBlock(Blocks.REDSTONE_INTERFACE, Items.REDSTONE_INTERFACE, REDSTONE_INTERFACE_MODEL);

        registerCableStates();
    }

    private void registerCableStates() {
        final ModelFile baseModel = models().getExistingFile(CABLE_MODEL);
        final ModelFile linkModel = models().getExistingFile(CABLE_LINK_MODEL);
        final ModelFile plugModel = models().getExistingFile(CABLE_PLUG_MODEL);
        final ModelFile straightModel = models().getExistingFile(CABLE_STRAIGHT_MODEL);

        final MultiPartBlockStateBuilder builder = getMultipartBuilder(Blocks.BUS_CABLE.get());

        // NB: We use a custom model loader + baked model to replace the base part with straight parts and
        //     insert supports where appropriate, as well as for replacing it with a facade block model.

        builder.part()
            .modelFile(baseModel)
            .addModel()
            .end();

        BusCableBlock.FACING_TO_CONNECTION_MAP.forEach((direction, connectionType) -> {
            final int rotationY = (int) direction.toYRot();
            final int rotationX;
            if (direction == Direction.UP) {
                rotationX = 90;
            } else if (direction == Direction.DOWN) {
                rotationX = -90;
            } else {
                rotationX = 0;
            }

            builder.part()
                .modelFile(linkModel)
                .rotationY(rotationY)
                .rotationX(rotationX)
                .addModel()
                .condition(connectionType, BusCableBlock.ConnectionType.CABLE)
                .condition(BusCableBlock.HAS_FACADE, false)
                .end();

            builder.part()
                .modelFile(plugModel)
                .rotationY(rotationY)
                .rotationX(rotationX)
                .addModel()
                .condition(connectionType, BusCableBlock.ConnectionType.INTERFACE)
                .condition(BusCableBlock.HAS_FACADE, false)
                .end();
        });

        itemModels().getBuilder(Items.BUS_CABLE.getId().getPath())
            .parent(straightModel)
            .transforms()
            .transform(ItemTransforms.TransformType.GUI)
            .rotation(30, 225, 0)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.GROUND)
            .translation(0, 3, 0)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIXED)
            .rotation(0, 180, 0)
            .scale(1.0f)
            .end()
            .transform(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND)
            .rotation(75, 45, 0)
            .translation(0, 2.5f, 0)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND)
            .rotation(0, 45, 0)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND)
            .rotation(0, 225, 0)
            .scale(0.75f)
            .end();

        itemModels().getBuilder(Items.BUS_INTERFACE.getId().getPath())
            .parent(plugModel)
            .transforms()
            .transform(ItemTransforms.TransformType.GUI)
            .rotation(30, 315, 0)
            .translation(2, 1, 0)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.GROUND)
            .translation(0, 3, -5)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIXED)
            .rotation(0, 180, 0)
            .translation(0, 0, 4)
            .scale(1.0f)
            .end()
            .transform(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND)
            .rotation(75, 180, 0)
            .translation(0, -1, 0)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND)
            .rotation(0, 180, 0)
            .translation(0, 0, 2)
            .scale(0.75f)
            .end()
            .transform(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND)
            .rotation(0, 180, 0)
            .translation(0, 0, 2)
            .scale(0.75f)
            .end();
    }

    private <T extends Block> ItemModelBuilder horizontalBlock(final RegistryObject<T> block, final RegistryObject<Item> item, final ResourceLocation modelFileLocation) {
        horizontalBlock(block.get(), models().getExistingFile(modelFileLocation));
        return itemModels().getBuilder(item.getId().getPath()).parent(models().getExistingFile(block.getId()));
    }

    private <T extends Block> ItemModelBuilder horizontalFaceBlock(final RegistryObject<T> block, final RegistryObject<Item> item, final ResourceLocation modelFileLocation) {
        horizontalFaceBlock(block.get(), models().getExistingFile(modelFileLocation));
        return itemModels().getBuilder(item.getId().getPath()).parent(models().getExistingFile(block.getId()));
    }

    private <T extends Block> void simpleBlock(final RegistryObject<T> block, final RegistryObject<Item> item) {
        simpleBlock(block.get());
        itemModels().getBuilder(item.getId().getPath()).parent(models().getExistingFile(block.getId()));
    }
}
