package com.afofa555.eyeofpower.entity;

import com.afofa555.eyeofpower.ModParticles;
import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.power.PortalPower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Портал: телепортирует вошедшего к парному порталу. */
public class PortalEntity extends Entity {
    public static final int MAX_LIFE = 6000;

    private Vec3 dest = Vec3.ZERO;
    private int life = 0;

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setDest(Vec3 dest) {
        this.dest = dest;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.dest = new Vec3(tag.getDouble("dx"), tag.getDouble("dy"), tag.getDouble("dz"));
        this.life = tag.getInt("life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("dx", dest.x);
        tag.putDouble("dy", dest.y);
        tag.putDouble("dz", dest.z);
        tag.putInt("life", life);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        if (++life > MAX_LIFE) {
            this.discard();
            return;
        }
        if (this.tickCount % 2 != 0 || dest == Vec3.ZERO) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), le -> !le.isSpectator())) {
            if (PortalPower.onCooldown(level, e)) {
                continue;
            }
            PortalPower.setCooldown(level, e);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.PORTAL_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ModParticles.SPARK.get(), e.getX(), e.getY() + 1.0, e.getZ(), 20, 0.4, 0.8, 0.4, 0.2);
            if (e instanceof ServerPlayer sp) {
                sp.connection.teleport(dest.x, dest.y, dest.z, sp.getYRot(), sp.getXRot());
            } else {
                e.moveTo(dest.x, dest.y, dest.z, e.getYRot(), e.getXRot());
            }
            level.playSound(null, dest.x, dest.y, dest.z, ModSounds.PORTAL_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
            level.sendParticles(ModParticles.SPARK.get(), dest.x, dest.y + 1.0, dest.z, 20, 0.4, 0.8, 0.4, 0.2);
        }
    }

    private void clientTick() {
        double yaw = Math.toRadians(this.getYRot());
        double rx = Math.cos(yaw);
        double rz = Math.sin(yaw);
        double cx = this.getX();
        double cy = this.getY() + 1.3;
        double cz = this.getZ();
        double radius = 1.25;

        // Мерцающее свечение и кольцо
        this.level().addParticle(ModParticles.PORTAL_GLOW.get(), cx, cy, cz, 1.2, 0.0, 0.0);
        this.level().addParticle(ModParticles.PORTAL_RING.get(), cx, cy, cz, 1.45, 0.05, 0.0);

        // Кружащиеся искры по кольцу
        double base = this.tickCount * 0.28;
        for (int i = 0; i < 6; i++) {
            double a = base + i * (Math.PI * 2.0 / 6.0);
            double ca = Math.cos(a);
            double sa = Math.sin(a);
            double px = cx + rx * ca * radius;
            double py = cy + sa * radius;
            double pz = cz + rz * ca * radius;
            double tx = -rx * sa * 0.06;
            double ty = ca * 0.06;
            double tz = -rz * sa * 0.06;
            this.level().addParticle(ModParticles.SPARK.get(), px, py, pz, tx, ty, tz);
        }
        if (this.tickCount % 4 == 0) {
            this.level().addParticle(ModParticles.SPARK.get(),
                    cx + (this.random.nextDouble() - 0.5) * 1.6, cy + (this.random.nextDouble() - 0.5) * 1.6,
                    cz + (this.random.nextDouble() - 0.5) * 1.6, 0.0, 0.02, 0.0);
        }
    }
}
