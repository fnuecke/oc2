/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public final class WrenchRecipeBuilder {
    private final Item result;
    private final int count;
    private final List<Ingredient> ingredients = Lists.newArrayList();
    private final Advancement.Builder advancementBuilder = Advancement.Builder.advancement();
    private String group;

    public WrenchRecipeBuilder(final ItemLike result, final int count) {
        this.result = result.asItem();
        this.count = count;

        requires(Items.WRENCH.get());
    }

    public static WrenchRecipeBuilder wrenchRecipe(final ItemLike resultIn) {
        return new WrenchRecipeBuilder(resultIn, 1);
    }

    public static WrenchRecipeBuilder wrenchRecipe(final ItemLike resultIn, final int countIn) {
        return new WrenchRecipeBuilder(resultIn, countIn);
    }

    public WrenchRecipeBuilder requires(final Tag<Item> tagIn) {
        return this.requires(Ingredient.of(tagIn));
    }

    public WrenchRecipeBuilder requires(final ItemLike itemIn) {
        return this.requires(itemIn, 1);
    }

    public WrenchRecipeBuilder requires(final ItemLike itemIn, final int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.requires(Ingredient.of(itemIn));
        }

        return this;
    }

    public WrenchRecipeBuilder requires(final Ingredient ingredientIn) {
        return this.addIngredient(ingredientIn, 1);
    }

    public WrenchRecipeBuilder addIngredient(final Ingredient ingredientIn, final int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.ingredients.add(ingredientIn);
        }

        return this;
    }

    public WrenchRecipeBuilder unlockedBy(final String name, final CriterionTriggerInstance criterionIn) {
        this.advancementBuilder.addCriterion(name, criterionIn);
        return this;
    }

    public WrenchRecipeBuilder setGroup(final String groupIn) {
        this.group = groupIn;
        return this;
    }

    public void save(final Consumer<FinishedRecipe> consumerIn) {
        final ResourceLocation key = ForgeRegistries.ITEMS.getKey(this.result);
        if (key != null) {
            this.save(consumerIn, key);
        }
    }

    public void save(final Consumer<FinishedRecipe> consumerIn, final String save) {
        final ResourceLocation resourcelocation = ForgeRegistries.ITEMS.getKey(this.result);
        if ((new ResourceLocation(save)).equals(resourcelocation)) {
            throw new IllegalStateException("Shapeless Recipe " + save + " should remove its 'save' argument");
        } else {
            this.save(consumerIn, new ResourceLocation(save));
        }
    }

    public void save(final Consumer<FinishedRecipe> consumerIn, final ResourceLocation id) {
        this.validate(id);
        this.advancementBuilder.parent(new ResourceLocation("recipes/root")).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id)).rewards(AdvancementRewards.Builder.recipe(id)).requirements(RequirementsStrategy.OR);
        final CreativeModeTab itemCategory = this.result.getItemCategory();
        if (itemCategory != null) {
            consumerIn.accept(new WrenchRecipeBuilder.Result(id, this.result, this.count, this.group == null ? "" : this.group, this.ingredients, this.advancementBuilder, new ResourceLocation(id.getNamespace(), "recipes/" + itemCategory.getRecipeFolderName() + "/" + id.getPath())));
        }
    }

    private void validate(final ResourceLocation id) {
        if (this.advancementBuilder.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    public static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final Item result;
        private final int count;
        private final String group;
        private final List<Ingredient> ingredients;
        private final Advancement.Builder advancementBuilder;
        private final ResourceLocation advancementId;

        public Result(final ResourceLocation idIn, final Item resultIn, final int countIn, final String groupIn, final List<Ingredient> ingredientsIn, final Advancement.Builder advancementBuilderIn, final ResourceLocation advancementIdIn) {
            this.id = idIn;
            this.result = resultIn;
            this.count = countIn;
            this.group = groupIn;
            this.ingredients = ingredientsIn;
            this.advancementBuilder = advancementBuilderIn;
            this.advancementId = advancementIdIn;
        }

        public void serializeRecipeData(final JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            final JsonArray jsonarray = new JsonArray();

            for (final Ingredient ingredient : this.ingredients) {
                jsonarray.add(ingredient.toJson());
            }

            json.add("ingredients", jsonarray);
            final JsonObject jsonobject = new JsonObject();
            final ResourceLocation key = ForgeRegistries.ITEMS.getKey(this.result);
            if (key != null) {
                jsonobject.addProperty("item", key.toString());
            }
            if (this.count > 1) {
                jsonobject.addProperty("count", this.count);
            }

            json.add("result", jsonobject);
        }

        public RecipeSerializer<?> getType() {
            return RecipeSerializers.WRENCH.get();
        }

        public ResourceLocation getId() {
            return this.id;
        }

        @Nullable
        public JsonObject serializeAdvancement() {
            return this.advancementBuilder.serializeToJson();
        }

        @Nullable
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
