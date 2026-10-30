package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.network.StatePayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

/** Клиентское состояние эффектов (приходит с сервера). Не содержит клиентских классов. */
public class ClientState {
    public static final UUID NONE = new UUID(0L, 0L);

    public static boolean timeActive = false;
    public static UUID timeStopper = NONE;
    public static long timeStartMs = 0;
    public static int timeTicks = 0;

    public static boolean bloodOn = false;
    public static long bloodStartMs = 0;
    public static boolean mirrorOn = false;
    public static long mirrorStartMs = 0;
    public static int accel = 0;
    public static long accelStartMs = 0;

    public record Hole(Vec3 pos, String dim) {}

    public static final Map<UUID, Hole> holes = new HashMap<>();
    public static List<Integer> cloneIds = List.of();

    // Кинематограф
    public static String cineKey = "";
    public static long cineStartMs = -100000;
    public static int cineDurMs = 0;
    public static int cineColor = 0xFFFFFF;

    public static long shakeStartMs = -100000;
    public static int shakeDurMs = 0;
    public static float shakeAmp = 0;

    public static UUID awakenOwner = NONE;
    public static int awakenTicks = -1;

    public static boolean isFrozenFor(UUID self) {
        return timeActive && !timeStopper.equals(self);
    }

    private static void cine(String key, int color, int dur) {
        cineKey = key;
        cineColor = color;
        cineDurMs = dur;
        cineStartMs = Util.getMillis();
    }

    private static void shake(int dur, float amp) {
        shakeStartMs = Util.getMillis();
        shakeDurMs = dur;
        shakeAmp = amp;
    }

    public static void apply(StatePayload p, UUID self) {
        long now = Util.getMillis();
        boolean on = p.value() != 0;
        switch (p.kind()) {
            case StatePayload.TIME -> {
                if (on && !timeActive) {
                    timeStartMs = now;
                    timeTicks = 0;
                    cine("cine.time_stop", 0x55FF88, 2800);
                    shake(700, 1.0F);
                }
                timeActive = on;
                timeStopper = on ? p.owner() : NONE;
            }
            case StatePayload.BLOOD -> {
                if (on && !bloodOn) {
                    bloodStartMs = now;
                    cine("cine.blood", 0xFF3030, 2800);
                    shake(900, 0.8F);
                }
                bloodOn = on;
            }
            case StatePayload.MIRROR -> {
                if (on && !mirrorOn) {
                    mirrorStartMs = now;
                    cine("cine.mirror", 0xC8B8FF, 2800);
                    shake(600, 0.5F);
                }
                if (on != mirrorOn) {
                    mirrorStartMs = now;
                }
                mirrorOn = on;
            }
            case StatePayload.ACCEL -> {
                if (p.value() > accel && p.owner().equals(self)) {
                    cine("cine.accel", 0x55E0FF, 2000);
                }
                accel = p.value();
                accelStartMs = now;
            }
            case StatePayload.HOLE -> {
                if (on) {
                    if (!holes.containsKey(p.owner()) && p.owner().equals(self)) {
                        cine("cine.hole", 0xB070FF, 2400);
                        shake(900, 1.0F);
                    }
                    holes.put(p.owner(), new Hole(new Vec3(p.x(), p.y(), p.z()), p.dim()));
                } else {
                    holes.remove(p.owner());
                }
            }
            case StatePayload.AWAKEN -> {
                awakenOwner = p.owner();
                awakenTicks = 0;
                if (p.owner().equals(self)) {
                    cine("cine.awaken", 0x55FF88, 3200);
                    shake(900, 1.2F);
                }
            }
            default -> { }
        }
    }

    public static void reset() {
        timeActive = false;
        timeStopper = NONE;
        bloodOn = false;
        mirrorOn = false;
        accel = 0;
        holes.clear();
        cloneIds = List.of();
        cineKey = "";
        awakenTicks = -1;
    }
}
