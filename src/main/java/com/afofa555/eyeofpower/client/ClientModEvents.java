package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.ModEntities;
import com.afofa555.eyeofpower.ModParticles;
import java.util.function.Supplier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = EyeOfPowerMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.OPEN_MENU);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CLONE.get(), CloneRenderer::new);
        event.registerEntityRenderer(ModEntities.PORTAL.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer pr) {
                pr.addLayer(new AmuletLayer(pr));
                pr.addLayer(new CloakLayer<AbstractClientPlayer>(pr));
            }
        }
    }

    private static void reg(RegisterParticleProvidersEvent e, Supplier<SimpleParticleType> type, FxParticle.Kind kind) {
        e.registerSpriteSet(type.get(), sprites -> new FxParticle.Provider(sprites, kind));
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        reg(event, ModParticles.TIME_STAR, FxParticle.Kind.STAR);
        reg(event, ModParticles.TIME_RING, FxParticle.Kind.RING);
        reg(event, ModParticles.TIME_CLOCK, FxParticle.Kind.CLOCK);
        reg(event, ModParticles.HOLE_CORE, FxParticle.Kind.CORE);
        reg(event, ModParticles.HOLE_HALO, FxParticle.Kind.HALO);
        reg(event, ModParticles.HOLE_ORBIT, FxParticle.Kind.ORBIT);
        reg(event, ModParticles.SOUL, FxParticle.Kind.SOUL);
        reg(event, ModParticles.BLOOD_DROP, FxParticle.Kind.DROP);
        reg(event, ModParticles.BLOOD_MIST, FxParticle.Kind.MIST);
        reg(event, ModParticles.MIRROR_SHARD, FxParticle.Kind.SHARD);
        reg(event, ModParticles.SPARK, FxParticle.Kind.SPARK);
        reg(event, ModParticles.PORTAL_RING, FxParticle.Kind.HALO);
        reg(event, ModParticles.PORTAL_GLOW, FxParticle.Kind.CORE);
    }
}
