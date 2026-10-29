package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.ModItems;
import com.afofa555.eyeofpower.item.EyeOfPowerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Рисует Глаз Силы как амулет на цепочке на груди (а не над головой). */
public class AmuletLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private ItemStack active;
    private ItemStack dormant;

    public AmuletLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (player.isInvisible() || !head.is(ModItems.EYE_OF_POWER.get())) {
            return;
        }
        if (active == null) {
            active = new ItemStack(ModItems.EYE_WORN.get());
            dormant = new ItemStack(ModItems.EYE_WORN_DORMANT.get());
        }
        boolean on = EyeOfPowerItem.isActive(head);

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.0F, -0.17F);
        float sway = (float) (Math.sin(ageInTicks * 0.12) * 2.0 + limbSwingAmount * Math.sin(limbSwing * 0.6662F) * 8.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(sway));
        poseStack.translate(0.0F, 0.3F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        float pulse = on ? 1.0F + 0.05F * (float) Math.sin(ageInTicks * 0.15F) : 1.0F;
        float s = 1.2F * pulse;
        poseStack.scale(s, -s, -s);
        Minecraft.getInstance().getItemRenderer().renderStatic(on ? active : dormant, ItemDisplayContext.FIXED,
                on ? LightTexture.FULL_BRIGHT : packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                player.level(), player.getId());
        poseStack.popPose();
    }
}
