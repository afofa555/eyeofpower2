package com.afofa555.eyeofpower.entity;

import com.afofa555.eyeofpower.ModParticles;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Летающий клон игрока: носит плащ, атакует с силой владельца. */
public class CloneEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.FLYING_SPEED, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER);
    }

    public void setOwner(Player player) {
        this.entityData.set(OWNER, Optional.of(player.getUUID()));
    }

    @Nullable
    public Player getOwnerPlayer() {
        return getOwnerUUID().map(id -> this.level().getPlayerByUUID(id)).orElse(null);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 10 != 0) {
            return;
        }
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }
        if (this.distanceToSqr(owner) > 80.0 * 80.0) {
            this.moveTo(owner.getX(), owner.getY() + 1.0, owner.getZ(), this.getYRot(), this.getXRot());
            this.getNavigation().stop();
        }
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || target == owner || target instanceof CloneEntity
                || target.distanceToSqr(this) > 40.0 * 40.0)) {
            this.setTarget(null);
            target = null;
        }
        if (target == null) {
            LivingEntity cand = owner.getLastHurtMob();
            if (cand == null || !cand.isAlive() || owner.tickCount - owner.getLastHurtMobTimestamp() > 200) {
                cand = null;
                double best = Double.MAX_VALUE;
                for (Monster m : this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(24.0), Monster::isAlive)) {
                    double d = m.distanceToSqr(this);
                    if (d < best) {
                        best = d;
                        cand = m;
                    }
                }
            }
            if (cand != null && cand != owner && !(cand instanceof CloneEntity)) {
                this.setTarget(cand);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.tickCount % 3 == 0) {
            this.level().addParticle(ModParticles.TIME_STAR.get(),
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.6,
                    this.getY() + this.random.nextDouble() * 1.8,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof CloneEntity) {
            return false;
        }
        if (source.getEntity() != null && getOwnerUUID().isPresent()
                && source.getEntity().getUUID().equals(getOwnerUUID().get())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private static class FollowOwnerGoal extends Goal {
        private final CloneEntity clone;

        FollowOwnerGoal(CloneEntity clone) {
            this.clone = clone;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = clone.getOwnerPlayer();
            return owner != null && clone.getTarget() == null && clone.distanceToSqr(owner) > 5.0 * 5.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = clone.getOwnerPlayer();
            return owner != null && clone.getTarget() == null && clone.distanceToSqr(owner) > 3.0 * 3.0;
        }

        @Override
        public void tick() {
            Player owner = clone.getOwnerPlayer();
            if (owner != null) {
                Vec3 p = owner.position().add(0.0, 1.0, 0.0);
                clone.getNavigation().moveTo(p.x, p.y, p.z, 1.3);
            }
        }
    }
}
