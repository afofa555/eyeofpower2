package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.network.ActionPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Меню порталов: ввод координат назначения. */
public class PortalScreen extends Screen {
    private static final int PANEL_W = 244;
    private static final int PANEL_H = 168;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private long openMs;
    private int px;
    private int py;

    public PortalScreen() {
        super(Component.translatable("power.eyeofpower.portals"));
    }

    @Override
    protected void init() {
        openMs = Util.getMillis();
        px = this.width - PANEL_W - 6;
        py = Math.max(4, this.height - PANEL_H - 6);
        Minecraft mc = Minecraft.getInstance();
        int bx = px + 14;
        int by = py + 58;
        int bw = 62;
        xBox = box(bx, by, bw, mc.player == null ? 0 : mc.player.getX());
        yBox = box(bx + bw + 14, by, bw, mc.player == null ? 64 : mc.player.getY());
        zBox = box(bx + (bw + 14) * 2, by, bw, mc.player == null ? 0 : mc.player.getZ());
        addRenderableWidget(xBox);
        addRenderableWidget(yBox);
        addRenderableWidget(zBox);

        addRenderableWidget(Button.builder(Component.translatable("ui.eyeofpower.portal_open"), b -> open())
                .bounds(px + 14, py + 86, PANEL_W - 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ui.eyeofpower.portal_close"),
                        b -> PacketDistributor.sendToServer(ActionPayload.of(ActionPayload.PORTAL_CLOSE)))
                .bounds(px + 14, py + 110, (PANEL_W - 34) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ui.eyeofpower.back"),
                        b -> Minecraft.getInstance().setScreen(new PowerMenuScreen()))
                .bounds(px + 20 + (PANEL_W - 34) / 2, py + 110, (PANEL_W - 34) / 2, 20).build());
    }

    private EditBox box(int x, int y, int w, double value) {
        EditBox b = new EditBox(this.font, x, y, w, 18, Component.empty());
        b.setMaxLength(12);
        b.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
        b.setValue(String.valueOf((int) Math.floor(value)));
        return b;
    }

    private static double parse(EditBox b) {
        try {
            return Double.parseDouble(b.getValue());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private void open() {
        PacketDistributor.sendToServer(new ActionPayload(ActionPayload.PORTAL_OPEN, 0,
                parse(xBox), parse(yBox), parse(zBox)));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter
            open();
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
        UiKit.panel(g, this.font, px, py, PANEL_W, PANEL_H, now, this.title, Component.translatable("ui.eyeofpower.portals_sub"));
        int bx = px + 14;
        int bw = 62;
        g.drawString(this.font, "X", bx + bw / 2 - 2, py + 46, 0xFF9060, false);
        g.drawString(this.font, "Y", bx + bw + 14 + bw / 2 - 2, py + 46, 0xFF9060, false);
        g.drawString(this.font, "Z", bx + (bw + 14) * 2 + bw / 2 - 2, py + 46, 0xFF9060, false);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, Component.translatable("ui.eyeofpower.portal_hint"), px + PANEL_W / 2, py + 140, 0x778899);
        g.drawCenteredString(this.font, Component.translatable("ui.eyeofpower.portal_hint2"), px + PANEL_W / 2, py + 152, 0x667788);
    }
}
