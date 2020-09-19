package li.cil.circuity;

import li.cil.circuity.api.CircuityAPI;
import li.cil.circuity.common.item.RISCVTesterItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(CircuityAPI.MOD_ID)
public final class Circuity {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CircuityAPI.MOD_ID);
    public static final RegistryObject<Item> RISCV_TESTER = ITEMS.register("riscv_tester", RISCVTesterItem::new);

    public Circuity() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
