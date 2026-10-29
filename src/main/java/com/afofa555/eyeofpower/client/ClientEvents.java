package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.ModItems;
import com.afofa555.eyeofpower.item.EyeOfPowerItem;
import com.afofa555.eyeofpower.network.ActionPayload;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = EyeOfPowerMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    private static final int[] SEQ = {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_5};
    private static int seqIndex = 0;
    private static long seqLast = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeys.OPEN_MENU.consumeClick()) {
            if (mc.player == null || mc.screen != null) {
                continue;
            }
            ItemStack head = mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (!head.is(ModItems.EYE_OF_POWER.get())) {
                mc.player.displayClientMessage(Component.translatable("message.eyeofpower.not_wearing"), true);
            } else if (!EyeOfPowerItem.isActive(head)) {
                mc.player.displayClientMessage(Component.translatable("message.eyeofpower.dormant"), true);
            } else {
                mc.setScreen(new PowerMenuScreen());
            }
        }
        ClientEffects.tick(mc);
    }

    // Тайное пробуждение Глаза
    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        ItemStack head = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.EYE_OF_POWER.get()) || EyeOfPowerItem.isActive(head)) {
            seqIndex = 0;
            return;
        }
        long now = Util.getMillis();
        if (now - seqLast > 3000L) {
            seqIndex = 0;
        }
        int key = event.getKey();
        if (key == SEQ[seqIndex]) {
            seqIndex++;
            seqLast = now;
            if (seqIndex == SEQ.length) {
                seqIndex = 0;
                PacketDistributor.sendToServer(ActionPayload.of(ActionPayload.AWAKEN));
            }
        } else if (key == SEQ[0]) {
            seqIndex = 1;
            seqLast = now;
        }
    }

    // Пока время остановлено, остальные игроки не могут двигаться
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (ClientState.isFrozenFor(player.getUUID())) {
            Input in = event.getInput();
            in.up = false;
            in.down = false;
            in.left = false;
            in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
            in.forwardImpulse = 0.0F;
            in.leftImpulse = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        ClientHud.render(event.getGuiGraphics(), Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }

    // ---------- Камера: зум, наклон, тряска ----------

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        long now = Util.getMillis();
        double mul = 1.0;
        if (ClientState.mirrorOn) {
            float in = UiKit.ease((now - ClientState.mirrorStartMs) / 2500.0F);
            mul *= 1.0 + (0.5 + 0.04 * Math.sin(now / 900.0)) * in;
        } else if (now - ClientState.mirrorStartMs < 1500 && ClientState.mirrorStartMs > 0) {
            float out = 1.0F - UiKit.ease((now - ClientState.mirrorStartMs) / 1500.0F);
            mul *= 1.0 + 0.5 * out;
        }
        if (ClientState.timeActive) {
            float t = (now - ClientState.timeStartMs) / 900.0F;
            if (t >= 0.0F && t <= 1.0F) {
                mul *= 1.0 + 0.3 * Math.sin(t * Math.PI);
            }
        }
        if (ClientState.accel != 0) {
            mul *= 1.0 + 0.04 * ClientState.accel;
        }
        long cine = now - ClientState.cineStartMs;
        if (cine >= 0 && cine < 900 && ClientState.cineKey.equals("cine.awaken")) {
            mul *= 1.0 - 0.18 * Math.sin(cine / 900.0 * Math.PI);
        }
        event.setFOV(event.getFOV() * mul);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = Util.getMillis();
        float roll = 0.0F;
        if (ClientState.mirrorOn) {
            float in = UiKit.ease((now - ClientState.mirrorStartMs) / 2500.0F);
            roll += (float) Math.sin(now / 2200.0) * 3.5F * in;
        }
        long el = now - ClientState.shakeStartMs;
        if (el >= 0 && el < ClientState.shakeDurMs) {
            float k = 1.0F - el / (float) ClientState.shakeDurMs;
            float amp = ClientState.shakeAmp * k * k;
            event.setPitch(event.getPitch() + (float) Math.sin(now * 0.09) * amp * 1.2F);
            event.setYaw(event.getYaw() + (float) Math.sin(now * 0.07 + 1.3) * amp * 1.0F);
            roll += (float) Math.sin(now * 0.11 + 0.5) * amp * 1.5F;
        }
        if (roll != 0.0F) {
            event.setRoll(event.getRoll() + roll);
        }
    }

    // ---------- Небо: кровавое и зеркальное ----------

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        long now = Util.getMillis();
        if (ClientState.bloodOn) {
            float k = UiKit.ease((now - ClientState.bloodStartMs) / 2000.0F);
            event.setRed(Mth.lerp(k, event.getRed(), 0.46F));
            event.setGreen(Mth.lerp(k, event.getGreen(), 0.02F));
            event.setBlue(Mth.lerp(k, event.getBlue(), 0.03F));
        } else if (ClientState.mirrorOn) {
            float k = UiKit.ease((now - ClientState.mirrorStartMs) / 2000.0F);
            event.setRed(Mth.lerp(k, event.getRed(), 0.62F));
            event.setGreen(Mth.lerp(k, event.getGreen(), 0.58F));
            event.setBlue(Mth.lerp(k, event.getBlue(), 0.82F));
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE || (!ClientState.bloodOn && !ClientState.mirrorOn)) {
            return;
        }
        long now = Util.getMillis();
        long start = ClientState.bloodOn ? ClientState.bloodStartMs : ClientState.mirrorStartMs;
        float k = UiKit.ease((now - start) / 2000.0F);
        float far = event.getFarPlaneDistance();
        if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
            // Небо "закрывается": тонется в цвет тумана
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(Mth.lerp(k, far, 8.0F));
        } else {
            float target = ClientState.mirrorOn ? 110.0F : 150.0F;
            event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), 4.0F));
            event.setFarPlaneDistance(Mth.lerp(k, far, Math.min(far, target)));
        }
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
