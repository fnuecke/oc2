package li.cil.oc2.common.item.crafting;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ShapelessNbtRecipe extends ShapelessRecipe {
    private static final Logger LOGGER = LogManager.getLogger();

    public ShapelessNbtRecipe(final ShapelessRecipe recipe) {
        this(recipe, recipe.getRecipeOutput());
    }

    public ShapelessNbtRecipe(final ShapelessRecipe recipe, final ItemStack output) {
        super(recipe.getId(), recipe.getGroup(), output, recipe.getIngredients());
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ShapelessNbtRecipe> {
        @Override
        public ShapelessNbtRecipe read(final ResourceLocation location, final JsonObject json) {
            final ShapelessRecipe recipe = CRAFTING_SHAPELESS.read(location, json);
            final ItemStack stack = recipe.getRecipeOutput();
            if (json.has("nbt")) {
                try {
                    final CompoundNBT recipeTag = JsonToNBT.getTagFromJson(
                            JSONUtils.getJsonObject(json, "nbt").toString());
                    stack.getOrCreateTag().merge(recipeTag);
                } catch (final CommandSyntaxException e) {
                    LOGGER.error(e);
                }
            }
            return new ShapelessNbtRecipe(recipe, stack);
        }

        @Nullable
        @Override
        public ShapelessNbtRecipe read(final ResourceLocation location, final PacketBuffer buffer) {
            return new ShapelessNbtRecipe(CRAFTING_SHAPELESS.read(location, buffer));
        }

        @Override
        public void write(final PacketBuffer buffer, final ShapelessNbtRecipe recipe) {
            CRAFTING_SHAPELESS.write(buffer, recipe);
        }
    }
}
