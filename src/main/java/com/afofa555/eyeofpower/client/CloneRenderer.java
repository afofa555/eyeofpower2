package com.afofa555.eyeofpower.client;

import com.afofa555.eyeofpower.entity.CloneEntity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Клон выглядит как игрок-владелец (его скин), в плаще и с его оружием в руке. */
public class CloneRenderer extends MobRenderer<CloneEntity, PlayerModel<CloneEntity>> {

    public CloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        this.addLayer(new CloakLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CloneEntity entity) {
        UUID id = entity.getOwnerUUID().orElse(entity.getUUID());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player p = mc.level.getPlayerByUUID(id);
            if (p instanceof AbstractClientPlayer acp) {
                return acp.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.get(id).texture();
    }
}
