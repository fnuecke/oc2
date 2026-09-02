/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.neoforge;

import li.cil.oc2.common.item.crafting.ToolRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolRecipeBuilder implements RecipeBuilder {
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients = NonNullList.create();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    // --------------------------------------------------------------------- //

    private ToolRecipeBuilder(final ItemStack result) {
        this.result = result;
    }

    public static ToolRecipeBuilder toolRecipe(final ItemLike result) {
        return new ToolRecipeBuilder(new ItemStack(result));
    }

    public static ToolRecipeBuilder toolRecipe(final ItemLike result, final int count) {
        return new ToolRecipeBuilder(new ItemStack(result, count));
    }

    public static ToolRecipeBuilder toolRecipe(final ItemStack result) {
        return new ToolRecipeBuilder(result);
    }

    // --------------------------------------------------------------------- //

    public ToolRecipeBuilder requires(final TagKey<Item> tag) {
        return requires(Ingredient.of(tag));
    }

    public ToolRecipeBuilder requires(final ItemLike item) {
        return requires(item, 1);
    }

    public ToolRecipeBuilder requires(final ItemLike item, final int quantity) {
        for (int i = 0; i < quantity; i++) {
            requires(Ingredient.of(item));
        }

        return this;
    }

    public ToolRecipeBuilder requires(final Ingredient ingredient) {
        ingredients.add(ingredient);
        return this;
    }

    @Override
    public ToolRecipeBuilder unlockedBy(final String name, final Criterion<?> criterion) {
        criteria.put(name, criterion);
        return this;
    }

    @Override
    public ToolRecipeBuilder group(@Nullable final String group) {
        this.group = group;
        return this;
    }

    @Override
    public Item getResult() {
        return result.getItem();
    }

    @Override
    public void save(final RecipeOutput output, final ResourceLocation id) {
        if (criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }

        final Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(advancement::addCriterion);

        final ShapelessRecipe shapeless = new ShapelessRecipe(
            group == null ? "" : group,
            RecipeBuilder.determineBookCategory(RecipeCategory.MISC),
            result,
            ingredients);

        output.accept(id, new ToolRecipe(shapeless), advancement.build(id.withPrefix("recipes/misc/")));
    }
}
