package li.cil.oc2.common.item.crafting;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class RecipeSerializers {
    private static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = RegistryUtils.create(ForgeRegistries.RECIPE_SERIALIZERS);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<WrenchRecipe.Serializer> WRENCH = RECIPE_SERIALIZERS.register("wrench", () -> WrenchRecipe.Serializer.INSTANCE);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
