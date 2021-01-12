package li.cil.oc2.common.customrecipes;

import li.cil.oc2.api.API;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class CustomRecipes {
    private static final DeferredRegister<IRecipeSerializer<?>> INITIALIZER = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, API.MOD_ID);

    public static final RegistryObject<HDDRecipe.Serializer> HDD_RECIPE = INITIALIZER.register("hard_drive", HDDRecipe.Serializer::new);
    public static final RegistryObject<MemoryRecipe.Serializer> MEMORY_RECIPE = INITIALIZER.register("memory", MemoryRecipe.Serializer::new);
    public static final RegistryObject<LinuxFlashRecipe.Serializer> FLASH_MEMORY_LINUX_RECIPE = INITIALIZER.register("flash_memory_linux", LinuxFlashRecipe.Serializer::new);

    public static void initialize() {
        INITIALIZER.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
