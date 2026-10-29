package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.ModEntities;
import com.afofa555.eyeofpower.ModParticles;
import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.entity.CloneEntity;
import com.afofa555.eyeofpower.network.CloneListPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Клоны владельца: летают, носят плащ, бьют с его силой. Можно меняться с ними местами. */
public class ClonePower {
    private static final int MAX_CLONES = 3;
    private static final Map<UUID, List<UUID>> clones = new HashMap<>();

    private static List<UUID> list(ServerPlayer p) {
        return clones.computeIfAbsent(p.getUUID(), id -> new ArrayList<>());
    }

    private static void prune(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        list(p).removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
    }

    private static void sendList(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<Integer> ids = new ArrayList<>();
        for (UUID id : list(p)) {
            Entity e = level.getEntity(id);
            if (e != null) {
                ids.add(e.getId());
            }
        }
        PacketDistributor.sendToPlayer(p, new CloneListPayload(ids));
    }

    public static void spawn(ServerPlayer p) {
        prune(p);
        if (list(p).size() >= MAX_CLONES) {
            PowerManager.message(p, "message.eyeofpower.clone_limit");
            return;
        }
        ServerLevel level = p.serverLevel();
        CloneEntity c = new CloneEntity(ModEntities.CLONE.get(), level);
        c.setOwner(p);
        double ox = (level.random.nextDouble() - 0.5) * 4.0;
        double oz = (level.random.nextDouble() - 0.5) * 4.0;
        c.moveTo(p.getX() + ox, p.getY() + 0.5, p.getZ() + oz, p.getYRot(), 0.0F);
        c.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(6.0, p.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        c.setItemSlot(EquipmentSlot.MAINHAND, p.getMainHandItem().copy());
        c.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        level.addFreshEntity(c);
        list(p).add(c.getUUID());
        level.sendParticles(ModParticles.TIME_STAR.get(), c.getX(), c.getY() + 1.0, c.getZ(), 30, 0.5, 0.9, 0.5, 0.15);
        level.playSound(null, c.getX(), c.getY(), c.getZ(), ModSounds.CLONE_SPAWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        sendList(p);
    }

    public static void dismissAll(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (UUID id : list(p)) {
            Entity e = level.getEntity(id);
            if (e != null) {
                level.sendParticles(ModParticles.TIME_STAR.get(), e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.4, 0.8, 0.4, 0.1);
                e.discard();
            }
        }
        list(p).clear();
        sendList(p);
    }

    public static void swap(ServerPlayer p, int index) {
        prune(p);
        List<UUID> l = list(p);
        if (index < 0 || index >= l.size()) {
            return;
        }
        ServerLevel level = p.serverLevel();
        Entity c = level.getEntity(l.get(index));
        if (c == null) {
            return;
        }
        Vec3 pp = p.position();
        Vec3 cp = c.position();
        level.sendParticles(ModParticles.TIME_STAR.get(), pp.x, pp.y + 1.0, pp.z, 25, 0.4, 0.8, 0.4, 0.12);
        level.sendParticles(ModParticles.TIME_STAR.get(), cp.x, cp.y + 1.0, cp.z, 25, 0.4, 0.8, 0.4, 0.12);
        p.connection.teleport(cp.x, cp.y, cp.z, p.getYRot(), p.getXRot());
        TimePowers.moveFrozen(p, cp);
        c.moveTo(pp.x, pp.y, pp.z, c.getYRot(), c.getXRot());
        if (c instanceof CloneEntity ce) {
            ce.getNavigation().stop();
        }
        level.playSound(null, pp.x, pp.y, pp.z, ModSounds.PORTAL_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
        level.playSound(null, cp.x, cp.y, cp.z, ModSounds.PORTAL_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    public static void tick(MinecraftServer server) {
        if (clones.isEmpty() || server.getTickCount() % 40 != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (clones.containsKey(p.getUUID())) {
                prune(p);
                if (list(p).isEmpty()) {
                    clones.remove(p.getUUID());
                }
                sendList(p);
            }
        }
    }

    public static void reset() {
        clones.clear();
    }
}
