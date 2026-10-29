package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.network.StatePayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Остановка и ускорение времени. Визуальные эффекты рисует клиент. */
public class TimePowers {
    private static UUID stopper = null;
    private static boolean wasFrozen = false;
    private static int age = 0;
    private static final Map<UUID, Vec3> frozenPos = new HashMap<>();
    private static final Set<UUID> noGravity = new HashSet<>();

    private static UUID accelOwner = null;
    private static int accelMult = 0;
    private static float baseRate = 20.0F;

    public static boolean isFrozenPlayer(Player p) {
        return stopper != null && !p.getUUID().equals(stopper);
    }

    // ---------------- Остановка времени ----------------

    public static void toggleTimeStop(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (stopper != null) {
            if (stopper.equals(player.getUUID())) {
                stopTime(server);
            } else {
                PowerManager.message(player, "message.eyeofpower.time_other");
            }
            return;
        }
        if (accelMult != 0) {
            PowerManager.message(player, "message.eyeofpower.time_busy");
            return;
        }
        stopper = player.getUUID();
        age = 0;
        frozenPos.clear();
        noGravity.clear();
        ServerTickRateManager rate = server.tickRateManager();
        wasFrozen = rate.isFrozen();
        rate.setFrozen(true);
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.TIME, 1, stopper));
        PowerManager.playAll(server, ModSounds.TIME_STOP.get(), 1.0F, 1.0F);
    }

    private static void stopTime(MinecraftServer server) {
        server.tickRateManager().setFrozen(wasFrozen);
        for (UUID id : noGravity) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                sp.setNoGravity(false);
            }
        }
        noGravity.clear();
        frozenPos.clear();
        stopper = null;
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.TIME, 0, null));
        PowerManager.playAll(server, ModSounds.TIME_RESUME.get(), 1.0F, 1.0F);
    }

    private static void tickTime(MinecraftServer server) {
        if (stopper == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(stopper);
        if (owner == null || !owner.isAlive() || !PowerManager.isWearingEye(owner)) {
            stopTime(server);
            return;
        }
        age++;

        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(stopper)) {
                continue;
            }
            if (noGravity.add(other.getUUID())) {
                other.setNoGravity(true);
            }
            Vec3 frozen = frozenPos.computeIfAbsent(other.getUUID(), id -> other.position());
            other.setDeltaMovement(Vec3.ZERO);
            if (!other.isPassenger() && other.position().distanceToSqr(frozen) > 0.01) {
                other.connection.teleport(frozen.x, frozen.y, frozen.z, other.getYRot(), other.getXRot());
            }
        }

        // Звуки часов: тихое тиканье каждую секунду и "сердце часов" раз в 10 секунд
        if (age % 200 == 0) {
            PowerManager.playAll(server, ModSounds.CLOCK_HEARTBEAT.get(), 1.0F, 1.0F);
        } else if (age % 20 == 0) {
            PowerManager.playAll(server, ModSounds.CLOCK_TICK.get(), 0.35F, 1.0F);
        }
    }

    public static void moveFrozen(ServerPlayer sp, Vec3 pos) {
        if (frozenPos.containsKey(sp.getUUID())) {
            frozenPos.put(sp.getUUID(), pos);
        }
    }

    // ---------------- Ускорение времени ----------------

    public static void toggleAccel(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (stopper != null) {
            PowerManager.message(player, "message.eyeofpower.time_busy");
            return;
        }
        if (accelMult != 0 && accelOwner != null && !accelOwner.equals(player.getUUID())) {
            PowerManager.message(player, "message.eyeofpower.accel_other");
            return;
        }
        ServerTickRateManager rate = server.tickRateManager();
        int next = accelMult == 0 ? 2 : (accelMult == 2 ? 3 : 0);
        if (accelMult == 0) {
            baseRate = rate.tickrate();
            accelOwner = player.getUUID();
        }
        accelMult = next;
        rate.setTickRate(next == 0 ? baseRate : baseRate * next);
        if (next == 0) {
            accelOwner = null;
        }
        PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.ACCEL, next, player.getUUID()));
        PowerManager.playAll(server, ModSounds.ACCEL_START.get(), 1.0F, next == 0 ? 0.7F : 1.0F);
    }

    private static void tickAccel(MinecraftServer server) {
        if (accelMult == 0 || server.getTickCount() % 20 != 0) {
            return;
        }
        ServerPlayer owner = accelOwner == null ? null : server.getPlayerList().getPlayer(accelOwner);
        if (owner == null || !owner.isAlive() || !PowerManager.isWearingEye(owner)) {
            server.tickRateManager().setTickRate(baseRate);
            accelMult = 0;
            accelOwner = null;
            PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.ACCEL, 0, null));
        }
    }

    // ---------------- Служебное ----------------

    public static void tick(MinecraftServer server) {
        tickTime(server);
        tickAccel(server);
    }

    public static void syncTo(ServerPlayer sp) {
        if (stopper != null) {
            PacketDistributor.sendToPlayer(sp, StatePayload.simple(StatePayload.TIME, 1, stopper));
        }
        if (accelMult != 0) {
            PacketDistributor.sendToPlayer(sp, StatePayload.simple(StatePayload.ACCEL, accelMult, accelOwner));
        }
    }

    public static void reset(MinecraftServer server) {
        if (stopper != null) {
            server.tickRateManager().setFrozen(wasFrozen);
        }
        if (accelMult != 0) {
            server.tickRateManager().setTickRate(baseRate);
        }
        stopper = null;
        accelMult = 0;
        accelOwner = null;
        frozenPos.clear();
        noGravity.clear();
    }
}
