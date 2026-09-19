/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

import static li.cil.oc2.common.item.Items.HARD_DRIVE_SMALL;
import static li.cil.oc2.gametest.util.TestSupport.*;

public final class RPCTypeAdapterTests {
    public static void directionAdapterIsApplied(final GameTestHelper helper) {
        final Gson gson = RPCTypeAdapters.beginBuildGson().create();

        assertEquals(helper, "lowercase name", Direction.UP.ordinal(),
            gson.fromJson("\"up\"", Direction.class).ordinal());
        assertEquals(helper, "numeric value", Direction.NORTH.ordinal(),
            gson.fromJson("2", Direction.class).ordinal());
        assertEquals(helper, "round trip", Direction.WEST.ordinal(),
            gson.fromJson(gson.toJson(Direction.WEST), Direction.class).ordinal());

        helper.succeed();
    }

    public static void itemStackAdapterIsApplied(final GameTestHelper helper) {
        final Gson gson = RPCTypeAdapters.beginBuildGson().create();

        final JsonElement json = gson.toJsonTree(new ItemStack(Items.STONE, 3));
        assertTrue(helper, "serializes to an object", json.isJsonObject());
        assertEquals(helper, "count survives", 3, json.getAsJsonObject().get("Count").getAsLong());
        assertTrue(helper, "item id is missing", json.getAsJsonObject().toString().contains("minecraft:stone"));

        assertTrue(helper, "empty stack serializes to null", gson.toJsonTree(ItemStack.EMPTY).isJsonNull());

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private RPCTypeAdapterTests() {
    }

    public static void itemStackCarriesFilteredComponents(final GameTestHelper helper) {
        final Gson gson = RPCTypeAdapters.beginBuildGson().create();

        final ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.set(DataComponents.CUSTOM_NAME, Component.literal("Digger"));
        tool.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Found in a cave"))));
        tool.set(DataComponents.REPAIR_COST, 5);
        tool.setDamageValue(10);
        tool.set(DataComponents.RARITY, Rarity.EPIC);
        tool.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.EFFICIENCY), 3);

        final JsonObject components = componentsOf(helper, gson.toJsonTree(tool));
        assertTrue(helper, "custom name survives", components.has("minecraft:custom_name"));
        assertTrue(helper, "lore survives", components.has("minecraft:lore"));
        assertTrue(helper, "enchantments survive", components.has("minecraft:enchantments"));
        assertTrue(helper, "repair cost survives", components.has("minecraft:repair_cost"));
        assertTrue(helper, "damage survives", components.has("minecraft:damage"));
        assertTrue(helper, "unlisted components stay filtered out", !components.has("minecraft:rarity"));

        final ItemStack armor = new ItemStack(Items.LEATHER_HELMET);
        armor.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000, true));
        assertTrue(helper, "dye color survives",
            componentsOf(helper, gson.toJsonTree(armor)).has("minecraft:dyed_color"));

        final JsonElement drive = gson.toJsonTree(HARD_DRIVE_SMALL.get().withCapacity(new ItemStack(HARD_DRIVE_SMALL.get()), 12345));
        final JsonObject customData = componentsOf(helper, drive).getAsJsonObject("minecraft:custom_data");
        final JsonObject modData = customData != null ? customData.getAsJsonObject("oc2") : null;
        assertTrue(helper, "storage capacity survives: " + drive,
            modData != null && modData.has("capacity") && modData.get("capacity").getAsInt() == 12345);

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static JsonObject componentsOf(final GameTestHelper helper, final JsonElement json) {
        final JsonElement components = json.getAsJsonObject().get("components");
        if (components == null || !components.isJsonObject()) {
            throw failure(helper, "no components in " + json);
        }
        return components.getAsJsonObject();
    }
}
