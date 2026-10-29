package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.ModItems;
import com.afofa555.eyeofpower.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Все клиентские визуальные эффекты на частицах. Количество частиц учитывает настройки игрока. */
public class ClientEffects {

    private static float budget(Minecraft mc) {
        ParticleStatus s = mc.options.particles().get();
        return s == ParticleStatus.ALL ? 1.0F : (s == ParticleStatus.DECREASED ? 0.5F : 0.2F);
    }

    private static void add(ClientLevel level, SimpleParticleType type, double x, double y, double z,
                            double vx, double vy, double vz) {
        level.addParticle(type, true, x, y, z, vx, vy, vz);
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        float b = budget(mc);
        RandomSource r = level.random;
        tickTime(level, r, b);
        tickAwaken(level, r, b);
        tickHoles(mc, level, r, b);
        tickBlood(mc, level, r, b);
        tickMirror(mc, level, r, b);
        tickAccel(mc, level, r, b);
        tickPlayers(mc, level, r, b);
    }

    // ---------- Остановка времени: взрыв зелёных частиц, кольца, вихрь, часы ----------
    private static void tickTime(ClientLevel level, RandomSource r, float b) {
        if (!ClientState.timeActive) {
            return;
        }
        int age = ClientState.timeTicks++;
        Player s = level.getPlayerByUUID(ClientState.timeStopper);
        if (s == null) {
            return;
        }
        double px = s.getX();
        double py = s.getY() + s.getBbHeight() * 0.5;
        double pz = s.getZ();

        if (age <= 16 && age % 4 == 0) {
            add(level, ModParticles.TIME_RING.get(), px, py, pz, 8.0 + age * 0.8, 0, 0);
            int n = (int) (40 * Math.max(b, 0.4F));
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2.0 / n;
                add(level, ModParticles.TIME_STAR.get(), px, s.getY() + 0.2, pz, Math.cos(a) * 0.75, 0.02, Math.sin(a) * 0.75);
            }
        }
        if (age <= 24) {
            int n = (int) (36 * b);
            for (int i = 0; i < n; i++) {
                double u = r.nextDouble() * 2.0 - 1.0;
                double th = r.nextDouble() * Math.PI * 2.0;
                double q = Math.sqrt(1.0 - u * u);
                double sp = 0.2 + r.nextDouble() * 0.6;
                add(level, ModParticles.TIME_STAR.get(), px, py, pz, q * Math.cos(th) * sp, u * sp, q * Math.sin(th) * sp);
            }
        }
        double a = age * 0.3;
        for (int k = 0; k < 2; k++) {
            double aa = a + k * Math.PI;
            add(level, ModParticles.TIME_STAR.get(), px + Math.cos(aa) * 1.4, s.getY() + (age * 0.05) % 2.0,
                    pz + Math.sin(aa) * 1.4, 0.0, 0.04, 0.0);
        }
        if (age % 10 == 0) {
            add(level, ModParticles.TIME_CLOCK.get(), px + (r.nextDouble() - 0.5) * 2.4, py + r.nextDouble(),
                    pz + (r.nextDouble() - 0.5) * 2.4, 0.0, 0.0, 0.0);
        }
    }

    // ---------- Пробуждение Глаза ----------
    private static void tickAwaken(ClientLevel level, RandomSource r, float b) {
        if (ClientState.awakenTicks < 0) {
            return;
        }
        int age = ClientState.awakenTicks++;
        if (age > 40) {
            ClientState.awakenTicks = -1;
            return;
        }
        Player p = level.getPlayerByUUID(ClientState.awakenOwner);
        if (p == null) {
            return;
        }
        double px = p.getX();
        double py = p.getY() + 1.5;
        double pz = p.getZ();
        if (age % 8 == 0) {
            add(level, ModParticles.TIME_RING.get(), px, py, pz, 6.0 + age * 0.3, 0, 0);
        }
        int n = (int) (20 * b);
        for (int i = 0; i < n; i++) {
            double u = r.nextDouble() * 2.0 - 1.0;
            double th = r.nextDouble() * Math.PI * 2.0;
            double q = Math.sqrt(1.0 - u * u);
            double sp = 0.15 + r.nextDouble() * 0.5;
            add(level, ModParticles.TIME_STAR.get(), px, py, pz, q * Math.cos(th) * sp, u * sp, q * Math.sin(th) * sp);
            if (i % 3 == 0) {
                add(level, ModParticles.SPARK.get(), px, py, pz, q * Math.cos(th) * sp, u * sp, q * Math.sin(th) * sp);
            }
        }
    }

    // ---------- Чёрная дыра: ядро, кольца, спиральный диск ----------
    private static void tickHoles(Minecraft mc, ClientLevel level, RandomSource r, float b) {
        if (ClientState.holes.isEmpty()) {
            return;
        }
        String dim = level.dimension().location().toString();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        for (ClientState.Hole h : ClientState.holes.values()) {
            if (!h.dim().equals(dim)) {
                continue;
            }
            Vec3 c = h.pos();
            double dist = cam.distanceTo(c);
            if (dist > 160.0) {
                continue;
            }
            add(level, ModParticles.HOLE_CORE.get(), c.x, c.y, c.z, 3.3, 0, 0);
            add(level, ModParticles.HOLE_HALO.get(), c.x, c.y, c.z, 4.7, 0.04, 0);
            add(level, ModParticles.HOLE_HALO.get(), c.x, c.y, c.z, 7.5, -0.025, 0);
            int n = (int) (9 * b) + 1;
            Vec3 nrm = FxParticle.DISC_NORMAL;
            for (int i = 0; i < n; i++) {
                Vec3 rnd = new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5);
                Vec3 u = nrm.cross(rnd);
                if (u.lengthSqr() < 1.0E-4) {
                    continue;
                }
                u = u.normalize();
                double rad = 10.0 + r.nextDouble() * 9.0;
                Vec3 pos = c.add(u.scale(rad)).add(nrm.scale((r.nextDouble() - 0.5) * 1.4));
                add(level, ModParticles.HOLE_ORBIT.get(), pos.x, pos.y, pos.z, c.x - pos.x, c.y - pos.y, c.z - pos.z);
            }
        }
    }

    // ---------- Кровавый дождь ----------
    private static void tickBlood(Minecraft mc, ClientLevel level, RandomSource r, float b) {
        if (!ClientState.bloodOn) {
            return;
        }
        Player me = mc.player;
        int n = (int) (16 * b);
        for (int i = 0; i < n; i++) {
            double x = me.getX() + (r.nextDouble() - 0.5) * 34.0;
            double z = me.getZ() + (r.nextDouble() - 0.5) * 34.0;
            int bx = (int) Math.floor(x);
            int bz = (int) Math.floor(z);
            if (!level.getChunkSource().hasChunk(bx >> 4, bz >> 4)) {
                continue;
            }
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
            double top = me.getY() + 14.0;
            if (ground >= top - 2.0) {
                continue;
            }
            double speed = 0.85;
            int life = (int) Math.min(40.0, (top - ground) / speed);
            add(level, ModParticles.BLOOD_DROP.get(), x, top + r.nextDouble() * 3.0, z, life, -speed, 0.0);
        }
        if (r.nextFloat() < 0.5F * b) {
            add(level, ModParticles.BLOOD_MIST.get(), me.getX() + (r.nextDouble() - 0.5) * 24.0, me.getY() + r.nextDouble() * 2.0,
                    me.getZ() + (r.nextDouble() - 0.5) * 24.0, (r.nextDouble() - 0.5) * 0.04, 0.0, (r.nextDouble() - 0.5) * 0.04);
        }
    }

    // ---------- Зеркальное измерение: осколки зеркального купола ----------
    private static void tickMirror(Minecraft mc, ClientLevel level, RandomSource r, float b) {
        if (!ClientState.mirrorOn) {
            return;
        }
        Player me = mc.player;
        int n = (int) (3 * b) + (r.nextFloat() < 0.5F ? 1 : 0);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double rad = 16.0 + r.nextDouble() * 24.0;
            double h = 4.0 + r.nextDouble() * 30.0;
            add(level, ModParticles.MIRROR_SHARD.get(), me.getX() + Math.cos(a) * rad, me.getY() + h, me.getZ() + Math.sin(a) * rad,
                    (r.nextDouble() - 0.5) * 0.05, (r.nextDouble() - 0.5) * 0.03, (r.nextDouble() - 0.5) * 0.05);
        }
    }

    // ---------- Ускорение времени: поток искр ----------
    private static void tickAccel(Minecraft mc, ClientLevel level, RandomSource r, float b) {
        if (ClientState.accel == 0) {
            return;
        }
        Player me = mc.player;
        int n = (int) (3 * b * ClientState.accel) + 1;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double rad = 2.0 + r.nextDouble() * 5.0;
            add(level, ModParticles.TIME_STAR.get(), me.getX() + Math.cos(a) * rad, me.getY() + r.nextDouble() * 3.0,
                    me.getZ() + Math.sin(a) * rad, -Math.sin(a) * 0.4, 0.02, Math.cos(a) * 0.4);
        }
    }

    // ---------- Амулет и плащ у других игроков ----------
    private static void tickPlayers(Minecraft mc, ClientLevel level, RandomSource r, float b) {
        for (Player p : level.players()) {
            if (p.isInvisible() || p.distanceToSqr(mc.player) > 48.0 * 48.0) {
                continue;
            }
            if (p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EYE_OF_POWER.get()) && r.nextFloat() < 0.12F * b) {
                double yaw = Math.toRadians(p.yBodyRot);
                double fx = -Math.sin(yaw) * 0.3;
                double fz = Math.cos(yaw) * 0.3;
                add(level, ModParticles.TIME_STAR.get(), p.getX() + fx + (r.nextDouble() - 0.5) * 0.3, p.getY() + 1.1 + r.nextDouble() * 0.3,
                        p.getZ() + fz + (r.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
            }
            if (p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BLOOD_CLOAK.get())
                    && p.getDeltaMovement().lengthSqr() > 0.02 && !p.onGround() && r.nextFloat() < 0.6F * b) {
                add(level, ModParticles.BLOOD_MIST.get(), p.getX() + (r.nextDouble() - 0.5) * 0.5, p.getY() + 1.0 + r.nextDouble() * 0.5,
                        p.getZ() + (r.nextDouble() - 0.5) * 0.5, 0.0, 0.0, 0.0);
            }
        }
    }
}
