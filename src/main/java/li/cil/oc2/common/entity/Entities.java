package li.cil.oc2.common.entity;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public final class Entities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = RegistryUtils.create(ForgeRegistries.ENTITIES);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<EntityType<RobotEntity>> ROBOT = register("robot", RobotEntity::new, EntityClassification.MISC, b -> b.sized(14f / 16f, 14f / 16f).fireImmune().noSummon());

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static <T extends Entity> RegistryObject<EntityType<T>> register(final String name, final EntityType.IFactory<T> factory, final EntityClassification classification, final Function<EntityType.Builder<T>, EntityType.Builder<T>> customizer) {
        return ENTITIES.register(name, () -> customizer.apply(EntityType.Builder.of(factory, classification)).build(name));
    }
}
