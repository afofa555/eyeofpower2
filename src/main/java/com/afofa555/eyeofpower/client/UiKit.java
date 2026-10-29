package com.afofa555.eyeofpower.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Общие элементы оформления меню. */
public class UiKit {
    public static float ease(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    public static int argb(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Фон панели с заголовком и бегущей полоской света. */
    public static void panel(GuiGraphics g, Font font, int x, int y, int w, int h, long now, Component title, Component subtitle) {
        float pulse = 0.5F + 0.5F * (float) Math.sin(now / 420.0);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, argb(120 + (int) (80 * pulse), 0x2ECC40));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF06140B);
        g.fill(x, y, x + w, y + h, 0xF00A0A14);
        g.fillGradient(x, y, x + w, y + 34, 0xFF18492D, 0xFF0A0A14);
        float p = (now % 2600L) / 2600.0F;
        int lx = x + (int) ((w - 70) * p);
        g.fill(lx, y, lx + 70, y + 2, argb(230, 0x88FFAA));
        g.drawCenteredString(font, title, x + w / 2, y + 8, 0x7CFFA0);
        g.drawCenteredString(font, subtitle, x + w / 2, y + 20, 0x8899AA);
    }

    /** Строка меню: иконка, название, описание, метка справа. */
    public static void row(GuiGraphics g, Font font, int x, int y, int w, int h, ItemStack icon, Component name,
                           Component desc, Component tag, int tagColor, boolean hover, boolean on, float anim) {
        int a = (int) (255 * anim);
        if (a < 6) {
            return;
        }
        int ox = (int) ((1.0F - anim) * 26);
        x += ox;
        int bg = on ? 0x103A22 : (hover ? 0x203040 : 0x12121C);
        g.fill(x, y, x + w, y + h, argb(a * 235 / 255, bg));
        g.fill(x, y, x + 2, y + h, argb(a, on ? 0x55FF88 : (hover ? 0x88AACC : 0x2A3A4A)));
        if (hover) {
            g.fillGradient(x + 2, y, x + w, y + h, argb(40, 0x88FFAA), argb(0, 0x88FFAA));
        }
        if (anim > 0.4F) {
            g.renderItem(icon, x + 7, y + (h - 16) / 2);
        }
        g.drawString(font, name, x + 30, y + 4, argb(a, 0xFFFFFF), false);
        g.drawString(font, desc, x + 30, y + 15, argb(a, 0x8FA0B0), false);
        if (tag != null) {
            int tw = font.width(tag);
            g.drawString(font, tag, x + w - tw - 8, y + (h - 8) / 2, argb(a, tagColor), false);
        }
    }
}
