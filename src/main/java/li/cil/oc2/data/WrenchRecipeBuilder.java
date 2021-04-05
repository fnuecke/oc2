package li.cil.oc2.data;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.RecipeSerializers;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.IRequirementsStrategy;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public final class WrenchRecipeBuilder {
    private final Item result;
    private final int count;
    private final List<Ingredient> ingredients = Lists.newArrayList();
    private final Advancement.Builder advancementBuilder = Advancement.Builder.builder();
    private String group;

    public WrenchRecipeBuilder(final IItemProvider result, final int count) {
        this.result = result.asItem();
        this.count = count;

        addIngredient(Items.WRENCH.get());
    }

    public static WrenchRecipeBuilder wrenchRecipe(final IItemProvider resultIn) {
        return new WrenchRecipeBuilder(resultIn, 1);
    }

    public static WrenchRecipeBuilder wrenchRecipe(final IItemProvider resultIn, final int countIn) {
        return new WrenchRecipeBuilder(resultIn, countIn);
    }

    public WrenchRecipeBuilder addIngredient(final ITag<Item> tagIn) {
        return this.addIngredient(Ingredient.fromTag(tagIn));
    }

    public WrenchRecipeBuilder addIngredient(final IItemProvider itemIn) {
        return this.addIngredient(itemIn, 1);
    }

    public WrenchRecipeBuilder addIngredient(final IItemProvider itemIn, final int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.addIngredient(Ingredient.fromItems(itemIn));
        }

        return this;
    }

    public WrenchRecipeBuilder addIngredient(final Ingredient ingredientIn) {
        return this.addIngredient(ingredientIn, 1);
    }

    public WrenchRecipeBuilder addIngredient(final Ingredient ingredientIn, final int quantity) {
        for (int i = 0; i < quantity; ++i) {
            this.ingredients.add(ingredientIn);
        }

        return this;
    }

    public WrenchRecipeBuilder addCriterion(final String name, final ICriterionInstance criterionIn) {
        this.advancementBuilder.withCriterion(name, criterionIn);
        return this;
    }

    public WrenchRecipeBuilder setGroup(final String groupIn) {
        this.group = groupIn;
        return this;
    }

    public void build(final Consumer<IFinishedRecipe> consumerIn) {
        this.build(consumerIn, ForgeRegistries.ITEMS.getKey(this.result));
    }

    public void build(final Consumer<IFinishedRecipe> consumerIn, final String save) {
        final ResourceLocation resourcelocation = ForgeRegistries.ITEMS.getKey(this.result);
        if ((new ResourceLocation(save)).equals(resourcelocation)) {
            throw new IllegalStateException("Shapeless Recipe " + save + " should remove its 'save' argument");
        } else {
            this.build(consumerIn, new ResourceLocation(save));
        }
    }

    public void build(final Consumer<IFinishedRecipe> consumerIn, final ResourceLocation id) {
        this.validate(id);
        this.advancementBuilder.withParentId(new ResourceLocation("recipes/root")).withCriterion("has_the_recipe", RecipeUnlockedTrigger.create(id)).withRewards(AdvancementRewards.Builder.recipe(id)).withRequirementsStrategy(IRequirementsStrategy.OR);
        consumerIn.accept(new WrenchRecipeBuilder.Result(id, this.result, this.count, this.group == null ? "" : this.group, this.ingredients, this.advancementBuilder, new ResourceLocation(id.getNamespace(), "recipes/" + this.result.getGroup().getPath() + "/" + id.getPath())));
    }

    private void validate(final ResourceLocation id) {
        if (this.advancementBuilder.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    public static class Result implements IFinishedRecipe {
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

        public void serialize(final JsonObject json) {
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            final JsonArray jsonarray = new JsonArray();

            for (final Ingredient ingredient : this.ingredients) {
                jsonarray.add(ingredient.serialize());
            }

            json.add("ingredients", jsonarray);
            final JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("item", ForgeRegistries.ITEMS.getKey(this.result).toString());
            if (this.count > 1) {
                jsonobject.addProperty("count", this.count);
            }

            json.add("result", jsonobject);
        }

        public IRecipeSerializer<?> getSerializer() {
            return RecipeSerializers.WRENCH.get();
        }

        public ResourceLocation getID() {
            return this.id;
        }

        @Nullable
        public JsonObject getAdvancementJson() {
            return this.advancementBuilder.serialize();
        }

        @Nullable
        public ResourceLocation getAdvancementID() {
            return this.advancementId;
        }
    }
}
