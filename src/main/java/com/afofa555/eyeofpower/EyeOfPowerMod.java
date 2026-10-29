package com.afofa555.eyeofpower;

import com.afofa555.eyeofpower.entity.CloneEntity;
import com.afofa555.eyeofpower.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(EyeOfPowerMod.MODID)
public class EyeOfPowerMod {
    public static final String MODID = "eyeofpower";

    public EyeOfPowerMod(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(EyeOfPowerMod::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CLONE.get(), CloneEntity.createAttributes().build());
    }
}
