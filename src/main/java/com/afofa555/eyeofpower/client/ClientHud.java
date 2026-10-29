package com.afofa555.eyeofpower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Кинематографичный интерфейс: чёрные полосы, титры, виньетки, часы, статусы. */
public class ClientHud {

    public static void render(GuiGraphics g, Minecraft mc) {
        Player me = mc.player;
        if (me == null || mc.options.hideGui) {
            return;
        }
        long now = Util.getMillis();
        int w = g.guiWidth();
        int h = g.guiHeight();
        vignettes(g, me, now, w, h);
        cinematic(g, mc, now, w, h);
        status(g, mc, me, now, w, h);
    }

    private static void edge(GuiGraphics g, int w, int h, int rgb, int alpha) {
        if (alpha < 3) {
            return;
        }
        g.fillGradient(0, 0, w, h / 4, UiKit.argb(alpha, rgb), UiKit.argb(0, rgb));
        g.fillGradient(0, h - h / 4, w, h, UiKit.argb(0, rgb), UiKit.argb(alpha, rgb));
    }

    private static void flash(GuiGraphics g, int w, int h, long start, long now, int rgb, int dur, float max) {
        float t = (now - start) / (float) dur;
        if (t < 0.0F || t > 1.0F) {
            return;
        }
        int a = (int) (255 * max * (1.0F - t) * (1.0F - t));
        if (a > 2) {
            g.fill(0, 0, w, h, UiKit.argb(a, rgb));
        }
    }

    private static void vignettes(GuiGraphics g, Player me, long now, int w, int h) {
        if (ClientState.timeActive) {
            boolean frozen = ClientState.isFrozenFor(me.getUUID());
            float in = UiKit.ease((now - ClientState.timeStartMs) / 900.0F);
            edge(g, w, h, 0x20FF70, (int) ((frozen ? 120 : 60) * in));
            if (frozen) {
                g.fill(0, 0, w, h, UiKit.argb((int) (20 * in), 0x00FF55));
            }
            flash(g, w, h, ClientState.timeStartMs, now, 0xB8FFD0, 700, 0.6F);
        }
        if (ClientState.bloodOn) {
            float in = UiKit.ease((now - ClientState.bloodStartMs) / 1500.0F);
            float pulse = 0.75F + 0.25F * (float) Math.sin(now / 600.0);
            edge(g, w, h, 0xA00000, (int) (130 * in * pulse));
            g.fill(0, 0, w, h, UiKit.argb((int) (28 * in), 0x700000));
            flash(g, w, h, ClientState.bloodStartMs, now, 0xFF2020, 900, 0.5F);
        }
        if (ClientState.mirrorOn) {
            float in = UiKit.ease((now - ClientState.mirrorStartMs) / 1800.0F);
            edge(g, w, h, 0xB8A8FF, (int) (90 * in));
            flash(g, w, h, ClientState.mirrorStartMs, now, 0xFFFFFF, 800, 0.6F);
        }
        if (ClientState.accel != 0) {
            edge(g, w, h, 0x40D8FF, 50);
        }
        if (ClientState.awakenTicks >= 0 || ClientState.cineKey.equals("cine.awaken")) {
            flash(g, w, h, ClientState.cineStartMs, now, 0x90FFB0, 1000, 0.55F);
        }
    }

    private static void cinematic(GuiGraphics g, Minecraft mc, long now, int w, int h) {
        long el = now - ClientState.cineStartMs;
        int dur = ClientState.cineDurMs;
        if (ClientState.cineKey.isEmpty() || el < 0 || el > dur) {
            return;
        }
        float in = UiKit.ease(el / 350.0F);
        float out = UiKit.ease((dur - el) / 450.0F);
        int bar = (int) (Math.min(in, out) * h * 0.11F);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);

        float ta = Math.min(UiKit.ease((el - 250) / 500.0F), UiKit.ease((dur - el) / 500.0F));
        int a = (int) (ta * 255);
        if (a < 8) {
            return;
        }
        float sc = 2.4F + 0.4F * (el / (float) dur);
        Component title = Component.translatable(ClientState.cineKey);
        int tw = mc.font.width(title);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(w / 2.0F, h * 0.40F, 0.0F);
        ps.scale(sc, sc, 1.0F);
        g.drawString(mc.font, title, -tw / 2, -4, UiKit.argb(a, ClientState.cineColor), true);
        ps.popPose();
        int lw = (int) (tw * sc * UiKit.ease((el - 200) / 700.0F));
        int ly = (int) (h * 0.40F + 12 * sc * 0.5F + 6);
        g.fill(w / 2 - lw / 2, ly, w / 2 + lw / 2, ly + 1, UiKit.argb(a, ClientState.cineColor));
    }

    private static void status(GuiGraphics g, Minecraft mc, Player me, long now, int w, int h) {
        // Часы остановки времени (появляются после вступительных титров, поэтому текст не дублируется)
        if (ClientState.timeActive) {
            long since = now - ClientState.timeStartMs - 2600;
            if (since > 0) {
                float fade = UiKit.ease(since / 600.0F);
                int a = (int) (255 * fade);
                if (a > 8) {
                    drawClock(g, mc, now, w, a, since / 1000.0);
                }
            }
        }
        // Остальные статусы - маленькие метки слева сверху
        int y = 8;
        if (ClientState.bloodOn) {
            y = chip(g, mc, y, "hud.eyeofpower.blood", 0xFF5050);
        }
        if (ClientState.mirrorOn) {
            y = chip(g, mc, y, "hud.eyeofpower.mirror", 0xC8B8FF);
        }
        if (ClientState.accel != 0) {
            Component c = Component.translatable("hud.eyeofpower.accel", ClientState.accel);
            g.fill(6, y - 2, 12 + mc.font.width(c) + 6, y + 10, 0xA0000000);
            g.drawString(mc.font, c, 10, y, 0x55E0FF, false);
            y += 14;
        }
        if (ClientState.holes.containsKey(me.getUUID())) {
            chip(g, mc, y, "hud.eyeofpower.hole", 0xB070FF);
        }
    }

    private static int chip(GuiGraphics g, Minecraft mc, int y, String key, int color) {
        Component c = Component.translatable(key);
        g.fill(6, y - 2, 12 + mc.font.width(c) + 6, y + 10, 0xA0000000);
        g.fill(6, y - 2, 8, y + 10, UiKit.argb(255, color));
        g.drawString(mc.font, c, 12, y, color, false);
        return y + 14;
    }

    private static void drawClock(GuiGraphics g, Minecraft mc, long now, int w, int a, double sec) {
        PoseStack ps = g.pose();
        int green = 0x55FF88;
        ps.pushPose();
        ps.translate(w / 2.0F, 40.0F, 0.0F);
        for (int i = 0; i < 12; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(i * 30.0F));
            g.fill(-1, -17, 1, i % 3 == 0 ? -11 : -14, UiKit.argb(a, green));
            ps.popPose();
        }
        double whole = Math.floor(sec);
        double f = Math.min(1.0, (sec - whole) * 5.0);
        double e = 1.0 - Math.pow(1.0 - f, 3.0) + 0.12 * Math.sin(f * Math.PI);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees((float) ((whole + e) * 30.0)));
        g.fill(-1, -14, 1, 2, UiKit.argb(a, 0xE8FFF0));
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees((float) (sec * 3.0)));
        g.fill(-1, -8, 1, 2, UiKit.argb(a, green));
        ps.popPose();
        g.fill(-2, -2, 2, 2, UiKit.argb(a, 0xFFFFFF));
        ps.popPose();

        Component t = Component.translatable("hud.eyeofpower.time_stopped");
        int pulse = (int) (a * (0.7 + 0.3 * Math.sin(now / 500.0)));
        g.drawCenteredString(mc.font, t, w / 2, 62, UiKit.argb(Math.max(pulse, 8), green));
    }
}
