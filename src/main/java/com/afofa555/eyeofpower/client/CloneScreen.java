package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.network.ActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Меню клонов: призвать, поменяться местами, убрать. */
public class CloneScreen extends Screen {
    private static final int PANEL_W = 244;
    private static final int ROW_H = 26;
    private static final int GAP = 3;

    private record Row(Component name, Component desc, ItemStack icon, Component tag, Runnable action) {}

    private long openMs;
    private int px;
    private int py;

    public CloneScreen() {
        super(Component.translatable("power.eyeofpower.clones"));
    }

    @Override
    protected void init() {
        openMs = Util.getMillis();
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        int n = ClientState.cloneIds.size();
        rows.add(new Row(Component.translatable("ui.eyeofpower.clone_spawn"), Component.translatable("ui.eyeofpower.clone_spawn.d"),
                new ItemStack(Items.TOTEM_OF_UNDYING), Component.literal(n + "/3"),
                () -> PacketDistributor.sendToServer(ActionPayload.of(ActionPayload.CLONE_SPAWN))));
        for (int i = 0; i < n; i++) {
            final int idx = i;
            String dist = "";
            if (mc.level != null && mc.player != null) {
                Entity e = mc.level.getEntity(ClientState.cloneIds.get(i));
                if (e != null) {
                    dist = " (" + (int) e.distanceTo(mc.player) + " m)";
                }
            }
            rows.add(new Row(Component.translatable("ui.eyeofpower.clone_n", i + 1),
                    Component.translatable("ui.eyeofpower.clone_swap").append(dist), new ItemStack(Items.PLAYER_HEAD),
                    Component.translatable("ui.eyeofpower.swap"),
                    () -> PacketDistributor.sendToServer(new ActionPayload(ActionPayload.CLONE_SWAP, idx, 0, 0, 0))));
        }
        rows.add(new Row(Component.translatable("ui.eyeofpower.clone_dismiss"), Component.translatable("ui.eyeofpower.clone_dismiss.d"),
                new ItemStack(Items.BARRIER), null,
                () -> PacketDistributor.sendToServer(ActionPayload.of(ActionPayload.CLONE_DISMISS))));
        rows.add(new Row(Component.translatable("ui.eyeofpower.back"), Component.translatable("ui.eyeofpower.back.d"),
                new ItemStack(Items.ARROW), null, () -> Minecraft.getInstance().setScreen(new PowerMenuScreen())));
        return rows;
    }

    private int panelH(int rows) {
        return 44 + rows * (ROW_H + GAP) + 8;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            List<Row> rows = rows();
            int ph = panelH(rows.size());
            px = this.width - PANEL_W - 6;
            py = Math.max(4, this.height - ph - 6);
            for (int i = 0; i < rows.size(); i++) {
                int y = py + 40 + i * (ROW_H + GAP);
                if (mx >= px + 8 && mx <= px + PANEL_W - 8 && my >= y && my <= y + ROW_H) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    rows.get(i).action().run();
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
        List<Row> rows = rows();
        int ph = panelH(rows.size());
        px = this.width - PANEL_W - 6;
        py = Math.max(4, this.height - ph - 6);
        float slide = UiKit.ease((now - openMs) / 220.0F);
        g.pose().pushPose();
        g.pose().translate((int) ((1.0F - slide) * (PANEL_W + 20)), 0, 0);
        UiKit.panel(g, this.font, px, py, PANEL_W, ph, now, this.title, Component.translatable("ui.eyeofpower.clones_sub"));
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = py + 40 + i * (ROW_H + GAP);
            float anim = UiKit.ease((now - openMs - 80 - i * 40L) / 220.0F);
            boolean hover = mouseX >= px + 8 && mouseX <= px + PANEL_W - 8 && mouseY >= y && mouseY <= y + ROW_H;
            UiKit.row(g, this.font, px + 8, y, PANEL_W - 16, ROW_H, r.icon(), r.name(), r.desc(), r.tag(), 0xFFD060, hover, false, anim);
        }
        g.pose().popPose();
    }
}
