/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class ItemGroup {
    private static final DeferredRegister<CreativeModeTab> TABS = RegistryUtils.getInitializerFor(Registries.CREATIVE_MODE_TAB);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<CreativeModeTab> COMMON = TABS.register("common", () ->
        CreativeTabRegistry.create(builder -> {
            builder.icon(() -> new ItemStack(Items.COMPUTER.get()));
            builder.title(Component.translatable("itemGroup." + API.MOD_ID + ".common"));
            builder.displayItems((parameters, output) -> BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(API.MOD_ID))
                .map(Map.Entry::getValue)
                .forEach(item -> addItem(item, parameters, output)));
        }));

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static void addItem(final Item item, final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output) {
        if (item instanceof final CreativeTabItemProvider provider) {
            provider.addCreativeTabItems(parameters, output);
        } else {
            output.accept(new ItemStack(item));
        }
    }

    private ItemGroup() {
    }
}
