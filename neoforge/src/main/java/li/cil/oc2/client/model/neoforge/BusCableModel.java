/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.neoforge;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public record BusCableModel(BlockStateModel.Unbaked proxy) implements CustomUnbakedBlockStateModel {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(API.MOD_ID, "bus_cable");
    public static final Identifier BUS_CABLE_STRAIGHT_MODEL = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/cable_straight");
    public static final Identifier BUS_CABLE_SUPPORT_MODEL = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/cable_support");

    public static final MapCodec<BusCableModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BlockStateModel.Unbaked.CODEC.fieldOf("proxy").forGetter(BusCableModel::proxy)
    ).apply(instance, BusCableModel::new));

    // --------------------------------------------------------------------- //
    // CustomUnbakedBlockStateModel

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public BlockStateModel bake(final ModelBaker baker) {
        final BlockModelPart[] straightModelByAxis = {
            bake(baker, BUS_CABLE_STRAIGHT_MODEL, Quadrant.R0, Quadrant.R90),
            bake(baker, BUS_CABLE_STRAIGHT_MODEL, Quadrant.R90, Quadrant.R0),
            bake(baker, BUS_CABLE_STRAIGHT_MODEL, Quadrant.R0, Quadrant.R0)
        };
        final BlockModelPart[] supportModelByFace = {
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R270, Quadrant.R0), // -y
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R90, Quadrant.R0), // +y
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R180), // -z
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R0), // +z
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R90), // -x
            bake(baker, BUS_CABLE_SUPPORT_MODEL, Quadrant.R0, Quadrant.R270) // +x
        };

        return new BusCableBakedModel(proxy.bake(baker), straightModelByAxis, supportModelByFace);
    }

    @Override
    public void resolveDependencies(final Resolver resolver) {
        proxy.resolveDependencies(resolver);
        resolver.markDependency(BUS_CABLE_STRAIGHT_MODEL);
        resolver.markDependency(BUS_CABLE_SUPPORT_MODEL);
    }

    // --------------------------------------------------------------------- //

    private static BlockModelPart bake(final ModelBaker baker, final Identifier model, final Quadrant x, final Quadrant y) {
        return new Variant(model).withXRot(x).withYRot(y).bake(baker);
    }
}
