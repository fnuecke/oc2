package li.cil.oc2.common.entity;

import li.cil.oc2.api.API;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public final class Entities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<EntityType<RobotEntity>> ROBOT = register("robot", RobotEntity::new, EntityClassification.MISC, b -> b.size(14f / 16f, 14f / 16f).immuneToFire().disableSummoning());

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    private static <T extends Entity> RegistryObject<EntityType<T>> register(final String name, final EntityType.IFactory<T> factory, final EntityClassification classification, final Function<EntityType.Builder<T>, EntityType.Builder<T>> customizer) {
        return ENTITIES.register(name, () -> customizer.apply(EntityType.Builder.create(factory, classification)).build(name));
    }
}
