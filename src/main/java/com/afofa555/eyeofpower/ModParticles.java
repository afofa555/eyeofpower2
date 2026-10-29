package com.afofa555.eyeofpower;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, EyeOfPowerMod.MODID);

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> reg(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false));
    }

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TIME_STAR = reg("time_star");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TIME_RING = reg("time_ring");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TIME_CLOCK = reg("time_clock");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HOLE_CORE = reg("hole_core");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HOLE_HALO = reg("hole_halo");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HOLE_ORBIT = reg("hole_orbit");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SOUL = reg("soul");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_DROP = reg("blood_drop");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_MIST = reg("blood_mist");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MIRROR_SHARD = reg("mirror_shard");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = reg("spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PORTAL_RING = reg("portal_ring");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PORTAL_GLOW = reg("portal_glow");
}
