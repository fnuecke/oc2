package li.cil.oc2;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.RISCVTesterItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(API.MOD_ID)
public final class OpenComputers {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, API.MOD_ID);
    public static final RegistryObject<Item> RISCV_TESTER = ITEMS.register("riscv_tester", RISCVTesterItem::new);

    public OpenComputers() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
