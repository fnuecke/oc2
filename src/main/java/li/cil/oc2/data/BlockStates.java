package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.item.Items;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.RegistryObject;

public class BlockStates extends BlockStateProvider {
    public BlockStates(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(Blocks.COMPUTER_BLOCK, Items.COMPUTER_ITEM);
        horizontalBlock(Blocks.REDSTONE_INTERFACE_BLOCK, Items.REDSTONE_INTERFACE_ITEM);
        horizontalFaceBlock(Blocks.NETWORK_CONNECTOR_BLOCK, Items.NETWORK_CONNECTOR_ITEM)
                .transforms()
                .transform(ModelBuilder.Perspective.GUI)
                .rotation(30, 315, 0)
                .translation(0, 2, 0)
                .scale(0.75f, 0.75f, 0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIXED)
                .rotation(270, 0, 0)
                .translation(0, 0, -5)
                .end()
                .end();
        horizontalBlock(Blocks.NETWORK_HUB_BLOCK, Items.NETWORK_HUB_ITEM);

        registerCableStates();
    }

    private void registerCableStates() {
        final ModelFile baseModel = models().getExistingFile(new ResourceLocation(API.MOD_ID, "block/cable_base"));
        final ModelFile linkModel = models().getExistingFile(new ResourceLocation(API.MOD_ID, "block/cable_link"));
        final ModelFile plugModel = models().getExistingFile(new ResourceLocation(API.MOD_ID, "block/cable_plug"));

        final MultiPartBlockStateBuilder builder = getMultipartBuilder(Blocks.BUS_CABLE_BLOCK.get());

        // NB: We use a custom model loader + baked model to replace the base part with straight parts and
        //     insert supports where appropriate.
        builder.part().modelFile(baseModel).addModel().end();

        BusCableBlock.FACING_TO_CONNECTION_MAP.forEach((direction, connectionType) -> {
            final int rotationY = (int) direction.getHorizontalAngle();
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
                    .condition(connectionType, BusCableBlock.ConnectionType.LINK)
                    .end();

            builder.part()
                    .modelFile(plugModel)
                    .rotationY(rotationY)
                    .rotationX(rotationX)
                    .addModel()
                    .condition(connectionType, BusCableBlock.ConnectionType.PLUG)
                    .end();
        });

        itemModels().getBuilder(Items.BUS_CABLE_ITEM.getId().getPath())
                .parent(baseModel)
                .transforms()
                .transform(ModelBuilder.Perspective.GUI)
                .rotation(30, 225, 0)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.GROUND)
                .translation(0, 3, 0)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIXED)
                .rotation(0, 180, 0)
                .scale(1.0f)
                .end()
                .transform(ModelBuilder.Perspective.THIRDPERSON_RIGHT)
                .rotation(75, 45, 0)
                .translation(0, 2.5f, 0)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
                .rotation(0, 45, 0)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
                .rotation(0, 225, 0)
                .scale(0.75f)
                .end();

        itemModels().getBuilder(Items.BUS_INTERFACE_ITEM.getId().getPath())
                .parent(plugModel)
                .transforms()
                .transform(ModelBuilder.Perspective.GUI)
                .rotation(30, 315, 0)
                .translation(2, 1, 0)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.GROUND)
                .translation(0, 3, -5)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIXED)
                .rotation(0, 180, 0)
                .translation(0, 0, 4)
                .scale(1.0f)
                .end()
                .transform(ModelBuilder.Perspective.THIRDPERSON_RIGHT)
                .rotation(75, 180, 0)
                .translation(0, -1, 0)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
                .rotation(0, 180, 0)
                .translation(0, 0, 2)
                .scale(0.75f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
                .rotation(0, 180, 0)
                .translation(0, 0, 2)
                .scale(0.75f)
                .end();
    }

    private <T extends Block> ItemModelBuilder horizontalBlock(final RegistryObject<T> block, final RegistryObject<Item> item) {
        horizontalBlock(block.get(), models().getBuilder(block.getId().getPath()));
        return itemModels().getBuilder(item.getId().getPath()).parent(models().getExistingFile(block.getId()));
    }

    private <T extends Block> ItemModelBuilder horizontalFaceBlock(final RegistryObject<T> block, final RegistryObject<Item> item) {
        horizontalFaceBlock(block.get(), models().getBuilder(block.getId().getPath()));
        return itemModels().getBuilder(item.getId().getPath()).parent(models().getExistingFile(block.getId()));
    }
}
