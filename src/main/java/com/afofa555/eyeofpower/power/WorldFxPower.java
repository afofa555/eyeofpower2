package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.entity.CloneEntity;
import com.afofa555.eyeofpower.network.StatePayload;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

/** Кровавое небо (с проклятиями) и Зеркальное измерение (визуальный эффект мира). */
public class WorldFxPower {
    private static UUID bloodOwner = null;
    private static UUID mirrorOwner = null;

    public static void toggleBlood(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (bloodOwner != null) {
            if (bloodOwner.equals(player.getUUID())) {
                endBlood();
            } else {
                PowerManager.message(player, "message.eyeofpower.fx_other");
            }
            return;
        }
        bloodOwner = player.getUUID();
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.BLOOD, 1, bloodOwner));
        PowerManager.playAll(server, ModSounds.BLOOD_START.get(), 1.0F, 1.0F);
    }

    private static void endBlood() {
        bloodOwner = null;
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.BLOOD, 0, null));
    }

    public static void toggleMirror(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (mirrorOwner != null) {
            if (mirrorOwner.equals(player.getUUID())) {
                endMirror();
            } else {
                PowerManager.message(player, "message.eyeofpower.fx_other");
            }
            return;
        }
        mirrorOwner = player.getUUID();
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.MIRROR, 1, mirrorOwner));
        PowerManager.playAll(server, ModSounds.MIRROR_START.get(), 1.0F, 1.0F);
    }

    private static void endMirror() {
        mirrorOwner = null;
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.MIRROR, 0, null));
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0 || (bloodOwner == null && mirrorOwner == null)) {
            return;
        }
        if (bloodOwner != null) {
            ServerPlayer o = server.getPlayerList().getPlayer(bloodOwner);
            if (o == null || !o.isAlive() || !PowerManager.isWearingEye(o)) {
                endBlood();
            }
        }
        if (mirrorOwner != null) {
            ServerPlayer o = server.getPlayerList().getPlayer(mirrorOwner);
            if (o == null || !o.isAlive() || !PowerManager.isWearingEye(o)) {
                endMirror();
            }
        }
        if (bloodOwner == null) {
            return;
        }
        // Проклятие крови: слабость, замедление, голод и усталость - игрокам (кроме владельца) и мобам рядом
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (!sp.getUUID().equals(bloodOwner) && !sp.isSpectator()) {
                sp.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, true));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, true));
                sp.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 0, false, true));
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0, false, true));
            }
            AABB box = sp.getBoundingBox().inflate(40.0);
            for (Mob m : sp.serverLevel().getEntitiesOfClass(Mob.class, box, mob -> !(mob instanceof CloneEntity))) {
                m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false));
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false));
            }
        }
    }

    public static void syncTo(ServerPlayer sp) {
        if (bloodOwner != null) {
            PacketDistributor.sendToPlayer(sp, StatePayload.simple(StatePayload.BLOOD, 1, bloodOwner));
        }
        if (mirrorOwner != null) {
            PacketDistributor.sendToPlayer(sp, StatePayload.simple(StatePayload.MIRROR, 1, mirrorOwner));
        }
    }

    public static void reset() {
        bloodOwner = null;
        mirrorOwner = null;
    }
}
