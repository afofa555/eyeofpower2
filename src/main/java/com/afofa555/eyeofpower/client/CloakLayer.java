package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.ModItems;
import com.afofa555.eyeofpower.entity.CloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** Кроваво-красный плащ за спиной (игроки в плаще и клоны). */
public class CloakLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, "textures/entity/blood_cloak.png");

    public CloakLayer(RenderLayerParent<T, PlayerModel<T>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        if (!(entity instanceof CloneEntity) && !entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BLOOD_CLOAK.get())) {
            return;
        }
        double speed = entity.getDeltaMovement().horizontalDistance();
        float lean = Mth.clamp((float) speed * 70.0F, 0.0F, 70.0F) + limbSwingAmount * 18.0F
                + (float) Math.sin(ageInTicks * 0.12) * 2.5F;
        if (entity.isCrouching()) {
            lean += 25.0F;
        }
        float flap = (float) Math.sin(ageInTicks * 0.22) * Mth.clamp((float) speed * 30.0F, 0.0F, 8.0F);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + lean));
        poseStack.mulPose(Axis.ZP.rotationDegrees(flap));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(TEXTURE));
        this.getParentModel().renderCloak(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
