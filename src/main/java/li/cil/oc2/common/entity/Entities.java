package li.cil.oc2.common.entity;

import li.cil.oc2.api.API;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class Entities {
    public static final EntityType<RobotEntity> ROBOT = Registry.register(Registry.ENTITY_TYPE,new ResourceLocation(API.MOD_ID, "robot"), FabricEntityTypeBuilder.create(MobCategory.MISC, RobotEntity::new).dimensions(EntityDimensions.fixed(14f / 16f, 14f / 16f)).fireImmune().disableSummon().build());
}
