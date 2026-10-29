package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.ModEntities;
import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.entity.PortalEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Парные порталы: один рядом с игроком, второй по введённым координатам. */
public class PortalPower {
    private static final Map<UUID, List<UUID>> portals = new HashMap<>();
    private static final Map<UUID, Long> cooldown = new HashMap<>();

    public static boolean onCooldown(ServerLevel level, LivingEntity e) {
        Long until = cooldown.get(e.getUUID());
        return until != null && level.getGameTime() < until;
    }

    public static void setCooldown(ServerLevel level, LivingEntity e) {
        cooldown.put(e.getUUID(), level.getGameTime() + 50L);
    }

    public static void open(ServerPlayer p, double x, double y, double z) {
        ServerLevel level = p.serverLevel();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || y < level.getMinBuildHeight() + 1 || y > level.getMaxBuildHeight() - 3
                || !level.getWorldBorder().isWithinBounds(x, z)) {
            PowerManager.message(p, "message.eyeofpower.portal_bad");
            return;
        }
        close(p);

        float yaw = p.getYRot();
        Vec3 fwd = forward(yaw);
        Vec3 a = p.position().add(fwd.scale(2.5));
        Vec3 b = new Vec3(x, y, z);

        // Грузим чанк назначения, чтобы второй портал точно появился
        level.getChunkAt(BlockPos.containing(b));

        PortalEntity pa = make(level, a, yaw, b.add(fwd.scale(2.2)));
        PortalEntity pb = make(level, b, yaw, a.add(fwd.scale(2.2)));
        List<UUID> l = new ArrayList<>();
        l.add(pa.getUUID());
        l.add(pb.getUUID());
        portals.put(p.getUUID(), l);

        level.playSound(null, a.x, a.y, a.z, ModSounds.PORTAL_OPEN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        level.playSound(null, b.x, b.y, b.z, ModSounds.PORTAL_OPEN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    private static PortalEntity make(ServerLevel level, Vec3 pos, float yaw, Vec3 dest) {
        PortalEntity e = new PortalEntity(ModEntities.PORTAL.get(), level);
        e.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        e.setDest(dest);
        level.addFreshEntity(e);
        return e;
    }

    private static Vec3 forward(float yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new Vec3(-Math.sin(r), 0.0, Math.cos(r));
    }

    public static void close(ServerPlayer p) {
        List<UUID> l = portals.remove(p.getUUID());
        if (l == null) {
            return;
        }
        for (ServerLevel level : p.server.getAllLevels()) {
            for (UUID id : l) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 400 != 0 || cooldown.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = cooldown.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() < now) {
                it.remove();
            }
        }
    }

    public static void reset() {
        portals.clear();
        cooldown.clear();
    }
}
