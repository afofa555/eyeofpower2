package com.afofa555.eyeofpower;

import com.afofa555.eyeofpower.entity.CloneEntity;
import com.afofa555.eyeofpower.entity.PortalEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EyeOfPowerMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CloneEntity>> CLONE = ENTITIES.register("clone",
            () -> EntityType.Builder.of(CloneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(2).build("clone"));

    public static final DeferredHolder<EntityType<?>, EntityType<PortalEntity>> PORTAL = ENTITIES.register("portal",
            () -> EntityType.Builder.of(PortalEntity::new, MobCategory.MISC)
                    .sized(2.2F, 2.8F).clientTrackingRange(8).updateInterval(20).fireImmune().build("portal"));
}
