/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static li.cil.oc2.gametest.util.TestSupport.assertEquals;
import static li.cil.oc2.gametest.util.TestSupport.assertTrue;

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
        assertTrue(helper, "carries the item id",
            json.getAsJsonObject().toString().contains("minecraft:stone"));

        assertTrue(helper, "empty stack serializes to null", gson.toJsonTree(ItemStack.EMPTY).isJsonNull());

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private RPCTypeAdapterTests() {
    }
}
