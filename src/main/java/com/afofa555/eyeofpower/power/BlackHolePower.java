package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.ModParticles;
import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.entity.CloneEntity;
import com.afofa555.eyeofpower.network.StatePayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Большая чёрная дыра: живёт, пока владелец сам не закроет её в меню. Визуал рисует клиент. */
public class BlackHolePower {
    private static final double RADIUS = 40.0;
    private static final double SWALLOW_DISTANCE = 3.4;
    private static final int MAX_HOLES = 3;
    private static final int MAX_PER_SCAN = 150;

    private static final List<Hole> holes = new ArrayList<>();

    private static final class Hole {
        final ResourceKey<Level> dim;
        final Vec3 center;
        final UUID owner;
        int age = 0;

        Hole(ResourceKey<Level> dim, Vec3 center, UUID owner) {
            this.dim = dim;
            this.center = center;
            this.owner = owner;
        }
    }

    private static StatePayload payload(Hole h, int value) {
        return new StatePayload(StatePayload.HOLE, value, h.owner, h.center.x, h.center.y, h.center.z,
                h.dim.location().toString());
    }

    public static void toggle(ServerPlayer player) {
        Iterator<Hole> it = holes.iterator();
        while (it.hasNext()) {
            Hole h = it.next();
            if (h.owner.equals(player.getUUID())) {
                ServerLevel lvl = player.server.getLevel(h.dim);
                close(lvl, h);
                it.remove();
                return;
            }
        }
        if (holes.size() >= MAX_HOLES) {
            PowerManager.message(player, "message.eyeofpower.hole_limit");
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(40.0)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double dist = hit.getType() == HitResult.Type.MISS ? 22.0
                : Math.max(10.0, eye.distanceTo(hit.getLocation()) - 7.0);
        Hole h = new Hole(level.dimension(), eye.add(look.scale(dist)), player.getUUID());
        holes.add(h);
        PacketDistributor.sendToAllPlayers(payload(h, 1));
        level.playSound(null, h.center.x, h.center.y, h.center.z, ModSounds.HOLE_OPEN.get(), SoundSource.AMBIENT, 5.0F, 1.0F);
    }

    private static void close(ServerLevel lvl, Hole h) {
        PacketDistributor.sendToAllPlayers(payload(h, 0));
        if (lvl != null) {
            lvl.playSound(null, h.center.x, h.center.y, h.center.z, ModSounds.HOLE_CLOSE.get(), SoundSource.AMBIENT, 5.0F, 1.0F);
        }
    }

    public static void tick(MinecraftServer server) {
        if (holes.isEmpty()) {
            return;
        }
        Iterator<Hole> it = holes.iterator();
        while (it.hasNext()) {
            Hole h = it.next();
            h.age++;
            ServerLevel lvl = server.getLevel(h.dim);
            ServerPlayer owner = server.getPlayerList().getPlayer(h.owner);
            if (lvl == null || owner == null || !owner.isAlive()) {
                close(lvl, h);
                it.remove();
                continue;
            }
            if (h.age % 70 == 1) {
                lvl.playSound(null, h.center.x, h.center.y, h.center.z, ModSounds.HOLE_HUM.get(), SoundSource.AMBIENT, 4.0F, 1.0F);
            }
            if (h.age % 2 == 0) {
                pull(lvl, h);
            }
        }
    }

    private static void pull(ServerLevel lvl, Hole h) {
        AABB box = new AABB(h.center, h.center).inflate(RADIUS);
        List<LivingEntity> targets = lvl.getEntitiesOfClass(LivingEntity.class, box,
                e -> (e instanceof Mob || e instanceof Player) && !(e instanceof CloneEntity)
                        && !e.getUUID().equals(h.owner) && !e.isSpectator());
        int n = 0;
        for (LivingEntity e : targets) {
            if (++n > MAX_PER_SCAN) {
                break;
            }
            Vec3 body = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            Vec3 to = h.center.subtract(body);
            double dist = to.length();
            if (dist > RADIUS) {
                continue;
            }
            if (dist < SWALLOW_DISTANCE) {
                swallow(lvl, e);
                continue;
            }
            // Шаг за 2 тика: чем ближе, тем сильнее тянет
            double step = Math.min(dist, 0.5 + (1.0 - dist / RADIUS) * 2.2);
            move(e, e.position().add(to.normalize().scale(step)));
            e.setDeltaMovement(Vec3.ZERO);
            if (h.age % 12 == 0) {
                lvl.sendParticles(ModParticles.SOUL.get(), e.getX(), e.getY() + 1.0, e.getZ(), 1, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    private static void swallow(ServerLevel lvl, LivingEntity e) {
        double x = e.getX();
        double z = e.getZ();
        lvl.sendParticles(ModParticles.SOUL.get(), x, e.getY() + 1.0, z, 10, 0.5, 0.8, 0.5, 0.06);
        lvl.playSound(null, x, e.getY(), z, ModSounds.SOUL_SCREAM.get(), SoundSource.HOSTILE, 3.0F,
                0.85F + lvl.random.nextFloat() * 0.4F);
        move(e, new Vec3(x, lvl.getMinBuildHeight() - 80.0, z));
        e.hurt(lvl.damageSources().fellOutOfWorld(), 1000.0F);
    }

    private static void move(LivingEntity e, Vec3 pos) {
        if (e instanceof ServerPlayer sp) {
            sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
            TimePowers.moveFrozen(sp, pos);
        } else {
            e.setPos(pos.x, pos.y, pos.z);
        }
        e.fallDistance = 0.0F;
    }

    public static void syncTo(ServerPlayer sp) {
        for (Hole h : holes) {
            PacketDistributor.sendToPlayer(sp, payload(h, 1));
        }
    }

    public static void reset() {
        holes.clear();
    }
}
