package com.afofa555.eyeofpower;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, EyeOfPowerMod.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> TIME_STOP = reg("time_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIME_RESUME = reg("time_resume");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOCK_TICK = reg("clock_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOCK_HEARTBEAT = reg("clock_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLE_OPEN = reg("hole_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLE_HUM = reg("hole_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLE_CLOSE = reg("hole_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_SCREAM = reg("soul_scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOD_START = reg("blood_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRROR_START = reg("mirror_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCEL_START = reg("accel_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_OPEN = reg("portal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_ENTER = reg("portal_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLONE_SPAWN = reg("clone_spawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> EYE_AWAKEN = reg("eye_awaken");
}
