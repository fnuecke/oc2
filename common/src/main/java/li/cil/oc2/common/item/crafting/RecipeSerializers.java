/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public final class RecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = RegistryUtils.getInitializerFor(Registries.RECIPE_SERIALIZER);

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<WrenchRecipe.Serializer> WRENCH = RECIPE_SERIALIZERS.register("wrench", WrenchRecipe.Serializer::new);
    public static final RegistrySupplier<SimpleCraftingRecipeSerializer<ResetRecipe>> RESET = RECIPE_SERIALIZERS.register("reset", () -> new SimpleCraftingRecipeSerializer<>(ResetRecipe::new));

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }
}
