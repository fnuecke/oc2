package li.cil.oc2.common.item.crafting;

import li.cil.oc2.api.API;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class RecipeSerializers {
    private static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, API.MOD_ID);

    public static final RegistryObject<ShapedNbtRecipe.Serializer> SHAPED = RECIPE_SERIALIZERS.register("shaped", ShapedNbtRecipe.Serializer::new);
    public static final RegistryObject<ShapelessNbtRecipe.Serializer> SHAPELESS = RECIPE_SERIALIZERS.register("shapeless", ShapelessNbtRecipe.Serializer::new);

    public static void initialize() {
        RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
