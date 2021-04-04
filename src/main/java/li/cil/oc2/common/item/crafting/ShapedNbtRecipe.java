package li.cil.oc2.common.item.crafting;

import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ShapedNbtRecipe extends ShapedRecipe {
    private static final Logger LOGGER = LogManager.getLogger();

    public ShapedNbtRecipe(final ShapedRecipe recipe) {
        this(recipe, recipe.getRecipeOutput());
    }

    public ShapedNbtRecipe(final ShapedRecipe recipe, final ItemStack output) {
        super(recipe.getId(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), output);
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ShapedNbtRecipe> {
        @Override
        public ShapedNbtRecipe read(final ResourceLocation location, final JsonObject json) {
            final ShapedRecipe recipe = CRAFTING_SHAPED.read(location, json);
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
            return new ShapedNbtRecipe(recipe, stack);
        }

        @Nullable
        @Override
        public ShapedNbtRecipe read(final ResourceLocation location, final PacketBuffer buffer) {
            return new ShapedNbtRecipe(CRAFTING_SHAPED.read(location, buffer));
        }

        @Override
        public void write(final PacketBuffer buffer, final ShapedNbtRecipe recipe) {
            CRAFTING_SHAPED.write(buffer, recipe);
        }
    }
}
