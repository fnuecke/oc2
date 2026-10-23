/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.fabric;

import com.mojang.math.Quadrant;
import li.cil.oc2.api.API;
import li.cil.oc2.client.model.fabric.BusCableModel;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.item.Items;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Most stuff comes from NeoForge datagen, this is just the diff; see fabric/build.gradle.kts.
 */
public final class ModModelProvider extends FabricModelProvider {
    private static final Identifier CABLE_MODEL = modelLocation("block/cable_base");
    private static final Identifier CABLE_LINK_MODEL = modelLocation("block/cable_link");
    private static final Identifier CABLE_PLUG_MODEL = modelLocation("block/cable_plug");

    public ModModelProvider(final FabricDataOutput output) {
        super(output);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void generateBlockStateModels(final BlockModelGenerators blocks) {
        blocks.blockStateOutput.accept(busCableDefinition());

        blocks.itemModelOutput.accept(Items.BUS_CABLE.get(), ItemModelUtils.plainModel(modelLocation("item/bus_cable")));
        blocks.itemModelOutput.accept(Items.BUS_INTERFACE.get(), ItemModelUtils.plainModel(modelLocation("item/bus_interface")));
    }

    @Override
    public void generateItemModels(final ItemModelGenerators items) {
    }

    // --------------------------------------------------------------------- //

    private static BlockModelDefinitionGenerator busCableDefinition() {
        final List<Selector> selectors = new ArrayList<>();

        selectors.add(new Selector(Optional.empty(),
            new BusCableModel(new SingleVariant.Unbaked(new Variant(CABLE_MODEL)))));

        BusCableBlock.FACING_TO_CONNECTION_MAP.forEach((direction, connectionType) -> {
            final Quadrant x = switch (direction) {
                case UP -> Quadrant.R90;
                case DOWN -> Quadrant.R270;
                default -> Quadrant.R0;
            };
            final Quadrant y = Quadrant.values()[Math.floorMod((int) direction.toYRot() / 90, 4)];

            selectors.add(new Selector(Optional.of(condition(connectionType.getName(), BusCableBlock.ConnectionType.CABLE.getSerializedName())),
                new SingleVariant.Unbaked(new Variant(CABLE_LINK_MODEL).withXRot(x).withYRot(y))));
            selectors.add(new Selector(Optional.of(condition(connectionType.getName(), BusCableBlock.ConnectionType.INTERFACE.getSerializedName())),
                new SingleVariant.Unbaked(new Variant(CABLE_PLUG_MODEL).withXRot(x).withYRot(y))));
        });

        return new BlockModelDefinitionGenerator() {
            @Override
            public Block block() {
                return Blocks.BUS_CABLE.get();
            }

            @Override
            public BlockModelDefinition create() {
                return new BlockModelDefinition(Optional.empty(),
                    Optional.of(new BlockModelDefinition.MultiPartDefinition(selectors)));
            }
        };
    }

    private static Condition condition(final String property, final String value) {
        return new KeyValueCondition(Map.of(
            property, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(value, false))),
            BusCableBlock.HAS_FACADE.getName(), new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term("false", false)))));
    }

    private static Identifier modelLocation(final String path) {
        return Identifier.fromNamespaceAndPath(API.MOD_ID, path);
    }
}
