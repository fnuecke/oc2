package li.cil.oc2.client.manual;

import li.cil.manual.api.ManualModel;
import li.cil.manual.api.Tab;
import li.cil.manual.api.prefab.Manual;
import li.cil.manual.api.prefab.provider.NamespaceContentProvider;
import li.cil.manual.api.prefab.provider.NamespacePathProvider;
import li.cil.manual.api.prefab.tab.ItemStackTab;
import li.cil.manual.api.prefab.tab.TextureTab;
import li.cil.manual.api.provider.ContentProvider;
import li.cil.manual.api.provider.PathProvider;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

@OnlyIn(Dist.CLIENT)
public final class Manuals {
    private static final DeferredRegister<ManualModel> MANUALS = RegistryUtils.create(ManualModel.class);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ManualModel> MANUAL = MANUALS.register("manual", Manual::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        final DeferredRegister<PathProvider> pathProviders = RegistryUtils.create(PathProvider.class);
        final DeferredRegister<ContentProvider> contentProviders = RegistryUtils.create(ContentProvider.class);
        final DeferredRegister<Tab> tabs = RegistryUtils.create(Tab.class);

        pathProviders.register("path_provider", () -> new NamespacePathProvider(API.MOD_ID));
        contentProviders.register("content_provider", () -> new NamespaceContentProvider(API.MOD_ID, "doc"));

        tabs.register("home", () -> new TextureTab(
                ManualModel.LANGUAGE_KEY + "/index.md",
                new TranslationTextComponent("manual." + API.MOD_ID + ".home"),
                new ResourceLocation(API.MOD_ID, "textures/gui/manual/home.png")));
        tabs.register("blocks", () -> new ItemStackTab(
                ManualModel.LANGUAGE_KEY + "/block/index.md",
                new TranslationTextComponent("manual." + API.MOD_ID + ".blocks"),
                new ItemStack(Blocks.COMPUTER.get())));
        tabs.register("modules", () -> new ItemStackTab(
                ManualModel.LANGUAGE_KEY + "/item/index.md",
                new TranslationTextComponent("manual." + API.MOD_ID + ".items"),
                new ItemStack(Items.TRANSISTOR.get())));
    }
}
