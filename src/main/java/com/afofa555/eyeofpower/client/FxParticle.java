package com.afofa555.eyeofpower.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/** Единый класс всех кастомных частиц мода (поведение задаётся Kind). */
public class FxParticle extends TextureSheetParticle {
    public enum Kind { STAR, RING, CLOCK, CORE, HALO, ORBIT, SOUL, DROP, MIST, SHARD, SPARK }

    public static final Vec3 DISC_NORMAL = new Vec3(0.22, 1.0, 0.12).normalize();

    private final Kind kind;
    private final float baseSize;
    private final float targetSize;
    private final float spin;
    private final float phase;
    private final double cx;
    private final double cy;
    private final double cz;

    FxParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
               SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.pickSprite(sprites);
        this.phase = this.random.nextFloat() * 6.2832F;

        float size = 0.3F;
        float target = 0.3F;
        float sp = 0.0F;
        int life = 20;
        double mx = 0.0;
        double my = 0.0;
        double mz = 0.0;
        double ccx = x;
        double ccy = y;
        double ccz = z;
        float fr = 1.0F;

        switch (kind) {
            case STAR -> {
                life = 22 + this.random.nextInt(18);
                size = 0.25F + this.random.nextFloat() * 0.3F;
                target = size * 0.2F;
                mx = dx; my = dy; mz = dz;
                sp = (this.random.nextFloat() - 0.5F) * 0.3F;
                fr = 0.9F;
            }
            case RING -> {
                life = 24;
                size = 0.3F;
                target = (float) dx;
            }
            case CLOCK -> {
                life = 45;
                size = 0.35F;
                mx = dx; my = 0.04 + dy; mz = dz;
                sp = (this.random.nextFloat() - 0.5F) * 0.08F;
                fr = 0.98F;
            }
            case CORE -> {
                life = 3;
                size = (float) dx;
                target = size;
            }
            case HALO -> {
                life = 3;
                size = (float) dx;
                target = size;
                sp = (float) dy;
            }
            case ORBIT -> {
                life = 80;
                size = 0.5F + this.random.nextFloat() * 0.9F;
                target = size;
                ccx = x + dx;
                ccy = y + dy;
                ccz = z + dz;
            }
            case SOUL -> {
                life = 40 + this.random.nextInt(20);
                size = 0.5F + this.random.nextFloat() * 0.3F;
                target = size * 1.3F;
                my = 0.06;
                fr = 0.97F;
            }
            case DROP -> {
                life = Math.max(2, (int) dx);
                size = 0.32F;
                target = size;
                my = dy;
                mx = 0.02;
                mz = 0.02;
            }
            case MIST -> {
                life = 40 + this.random.nextInt(25);
                size = 0.5F;
                target = 1.7F;
                mx = dx; my = 0.02 + dy; mz = dz;
                sp = (this.random.nextFloat() - 0.5F) * 0.05F;
            }
            case SHARD -> {
                life = 90 + this.random.nextInt(60);
                size = 0.5F + this.random.nextFloat() * 1.1F;
                target = size;
                mx = dx; my = dy; mz = dz;
                sp = (this.random.nextFloat() - 0.5F) * 0.06F;
                fr = 0.995F;
            }
            case SPARK -> {
                life = 12 + this.random.nextInt(10);
                size = 0.1F + this.random.nextFloat() * 0.12F;
                target = size * 0.3F;
                mx = dx; my = dy; mz = dz;
                fr = 0.95F;
            }
        }
        this.lifetime = life;
        this.baseSize = size;
        this.targetSize = target;
        this.spin = sp;
        this.xd = mx;
        this.yd = my;
        this.zd = mz;
        this.friction = fr;
        this.cx = ccx;
        this.cy = ccy;
        this.cz = ccz;
        this.quadSize = size;
        this.roll = this.random.nextFloat() * 6.28F;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.roll += this.spin;
        switch (kind) {
            case ORBIT -> tickOrbit();
            case RING, CORE, HALO -> { }
            case SOUL -> {
                this.xd = Math.sin((this.age + phase) * 0.25) * 0.02;
                this.zd = Math.cos((this.age + phase) * 0.21) * 0.02;
                this.move(this.xd, this.yd, this.zd);
                this.yd *= this.friction;
            }
            case SPARK -> {
                this.yd -= 0.004;
                this.move(this.xd, this.yd, this.zd);
                this.xd *= this.friction; this.yd *= this.friction; this.zd *= this.friction;
            }
            default -> {
                this.move(this.xd, this.yd, this.zd);
                this.xd *= this.friction;
                this.yd *= this.friction;
                this.zd *= this.friction;
            }
        }
    }

    private void tickOrbit() {
        double rx = this.x - cx;
        double ry = this.y - cy;
        double rz = this.z - cz;
        double len = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (len < 3.2) {
            this.remove();
            return;
        }
        double ang = 0.05 + 2.4 / len;
        double cos = Math.cos(ang);
        double sin = Math.sin(ang);
        Vec3 v = new Vec3(rx, ry, rz);
        Vec3 n = DISC_NORMAL;
        Vec3 rot = v.scale(cos).add(n.cross(v).scale(sin)).add(n.scale(n.dot(v) * (1.0 - cos)));
        double newLen = Math.max(3.0, len * 0.975 - 0.03);
        double k = newLen / Math.max(1.0E-6, rot.length());
        this.setPos(cx + rot.x * k, cy + rot.y * k, cz + rot.z * k);
    }

    @Override
    public float getQuadSize(float partial) {
        float t = Math.min(1.0F, (this.age + partial) / (float) this.lifetime);
        float wob = (float) Math.sin((this.age + partial) * 0.3 + phase);
        return switch (kind) {
            case RING -> baseSize + (targetSize - baseSize) * (1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t));
            case CORE -> baseSize * (1.0F + 0.04F * wob);
            case HALO -> baseSize * (1.0F + 0.03F * wob);
            case ORBIT -> baseSize * (0.6F + 0.4F * (1.0F - t));
            case SOUL -> baseSize + (targetSize - baseSize) * t;
            case MIST -> baseSize + (targetSize - baseSize) * t;
            case SHARD, DROP -> baseSize;
            default -> baseSize * (1.0F - 0.8F * t);
        };
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float a = this.age + partialTicks;
        float t = a / (float) this.lifetime;
        this.alpha = switch (kind) {
            case RING -> 0.9F * (1.0F - t);
            case CORE -> 1.0F;
            case HALO -> 0.9F * Math.min(1.0F, 4.5F / Math.max(0.1F, baseSize));
            case ORBIT -> Math.min(1.0F, a / 8.0F) * Math.min(1.0F, (1.0F - t) * 4.0F);
            case SOUL -> Math.min(1.0F, a / 6.0F) * (1.0F - t) * 0.9F;
            case DROP -> 0.85F;
            case MIST -> 0.55F * Math.min(1.0F, a / 8.0F) * (1.0F - t);
            case SHARD -> Math.min(1.0F, a / 10.0F) * Math.min(1.0F, (1.0F - t) * 5.0F) * 0.85F;
            case SPARK -> 1.0F - t * t;
            default -> Math.min(1.0F, a / 3.0F) * (1.0F - t * t);
        };
        super.render(buffer, camera, partialTicks);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new FxParticle(level, x, y, z, dx, dy, dz, sprites, kind);
        }
    }
}
