/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = RegistryUtils.getInitializerFor(Registries.RECIPE_SERIALIZER);

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<ToolRecipe.Serializer> TOOL = RECIPE_SERIALIZERS.register("tool", ToolRecipe.Serializer::new);
    public static final RegistrySupplier<CustomRecipe.Serializer<ResetRecipe>> RESET = RECIPE_SERIALIZERS.register("reset", () -> new CustomRecipe.Serializer<>(ResetRecipe::new));

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }
}
