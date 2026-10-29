package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.network.ActionPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Главное меню сил Глаза: список с иконками, описаниями и состоянием. */
public class PowerMenuScreen extends Screen {
    private static final int PANEL_W = 244;
    private static final int ROW_H = 26;
    private static final int GAP = 3;

    private record Entry(String name, String desc, ItemStack icon, int action) {}

    private static final int OPEN_CLONES = -1;
    private static final int OPEN_PORTALS = -2;

    private final Entry[] entries = {
            new Entry("power.eyeofpower.time_stop", "power.eyeofpower.time_stop.d", new ItemStack(Items.CLOCK), ActionPayload.TIME_STOP),
            new Entry("power.eyeofpower.time_accel", "power.eyeofpower.time_accel.d", new ItemStack(Items.FIREWORK_ROCKET), ActionPayload.TIME_ACCEL),
            new Entry("power.eyeofpower.black_hole", "power.eyeofpower.black_hole.d", new ItemStack(Items.CRYING_OBSIDIAN), ActionPayload.BLACK_HOLE),
            new Entry("power.eyeofpower.blood", "power.eyeofpower.blood.d", new ItemStack(Items.REDSTONE), ActionPayload.BLOOD_SKY),
            new Entry("power.eyeofpower.mirror", "power.eyeofpower.mirror.d", new ItemStack(Items.GLASS), ActionPayload.MIRROR),
            new Entry("power.eyeofpower.clones", "power.eyeofpower.clones.d", new ItemStack(Items.PLAYER_HEAD), OPEN_CLONES),
            new Entry("power.eyeofpower.portals", "power.eyeofpower.portals.d", new ItemStack(Items.ENDER_PEARL), OPEN_PORTALS),
    };

    private long openMs;
    private int px;
    private int py;
    private int ph;

    public PowerMenuScreen() {
        super(Component.translatable("screen.eyeofpower.title"));
    }

    @Override
    protected void init() {
        openMs = Util.getMillis();
        ph = 44 + entries.length * (ROW_H + GAP) + 18;
        px = this.width - PANEL_W - 6;
        py = Math.max(4, this.height - ph - 6);
    }

    private int rowY(int i) {
        return py + 40 + i * (ROW_H + GAP);
    }

    private boolean isOn(int action) {
        Minecraft mc = Minecraft.getInstance();
        return switch (action) {
            case ActionPayload.TIME_STOP -> ClientState.timeActive;
            case ActionPayload.TIME_ACCEL -> ClientState.accel != 0;
            case ActionPayload.BLACK_HOLE -> mc.player != null && ClientState.holes.containsKey(mc.player.getUUID());
            case ActionPayload.BLOOD_SKY -> ClientState.bloodOn;
            case ActionPayload.MIRROR -> ClientState.mirrorOn;
            default -> false;
        };
    }

    private Component tag(int action) {
        if (action == OPEN_CLONES) {
            return Component.literal(ClientState.cloneIds.size() + "/3");
        }
        if (action == OPEN_PORTALS) {
            return Component.literal(">");
        }
        if (action == ActionPayload.TIME_ACCEL && ClientState.accel != 0) {
            return Component.literal("x" + ClientState.accel);
        }
        return isOn(action) ? Component.translatable("ui.eyeofpower.on") : Component.translatable("ui.eyeofpower.off");
    }

    private void choose(int i) {
        Entry e = entries[i];
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
        if (e.action() == OPEN_CLONES) {
            mc.setScreen(new CloneScreen());
            return;
        }
        if (e.action() == OPEN_PORTALS) {
            mc.setScreen(new PortalScreen());
            return;
        }
        PacketDistributor.sendToServer(ActionPayload.of(e.action()));
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < entries.length; i++) {
                int y = rowY(i);
                if (mx >= px + 8 && mx <= px + PANEL_W - 8 && my >= y && my <= y + ROW_H) {
                    choose(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.OPEN_MENU.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        int idx = keyCode - GLFW.GLFW_KEY_1;
        if (idx >= 0 && idx < entries.length) {
            choose(idx);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float slide = UiKit.ease((now - openMs) / 280.0F);
        int off = (int) ((1.0F - slide) * (PANEL_W + 20));
        g.pose().pushPose();
        g.pose().translate(off, 0, 0);
        UiKit.panel(g, this.font, px, py, PANEL_W, ph, now, this.title, Component.translatable("screen.eyeofpower.subtitle"));
        for (int i = 0; i < entries.length; i++) {
            Entry e = entries[i];
            float anim = UiKit.ease((now - openMs - 120 - i * 45L) / 260.0F);
            int y = rowY(i);
            boolean hover = mouseX >= px + 8 + off && mouseX <= px + PANEL_W - 8 + off && mouseY >= y && mouseY <= y + ROW_H;
            boolean on = isOn(e.action());
            int tagColor = on ? 0x55FF88 : (e.action() < 0 ? 0xFFD060 : 0x667788);
            UiKit.row(g, this.font, px + 8, y, PANEL_W - 16, ROW_H, e.icon(), Component.literal((i + 1) + "  ").append(Component.translatable(e.name())),
                    Component.translatable(e.desc()), tag(e.action()), tagColor, hover, on, anim);
        }
        g.drawCenteredString(this.font, Component.translatable("screen.eyeofpower.hint"), px + PANEL_W / 2, py + ph - 13, 0x667788);
        g.pose().popPose();
    }
}
