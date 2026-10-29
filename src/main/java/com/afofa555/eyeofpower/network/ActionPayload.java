package com.afofa555.eyeofpower.network;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.power.PowerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент -> сервер: действие игрока (выбор силы, портал, клон и т.д.). */
public record ActionPayload(int action, int arg, double x, double y, double z) implements CustomPacketPayload {
    public static final int AWAKEN = 0;
    public static final int TIME_STOP = 1;
    public static final int TIME_ACCEL = 2;
    public static final int BLACK_HOLE = 3;
    public static final int BLOOD_SKY = 4;
    public static final int MIRROR = 5;
    public static final int CLONE_SPAWN = 6;
    public static final int CLONE_DISMISS = 7;
    public static final int CLONE_SWAP = 8;
    public static final int PORTAL_OPEN = 9;
    public static final int PORTAL_CLOSE = 10;

    public static final CustomPacketPayload.Type<ActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, "action"));

    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActionPayload::action,
            ByteBufCodecs.VAR_INT, ActionPayload::arg,
            ByteBufCodecs.DOUBLE, ActionPayload::x,
            ByteBufCodecs.DOUBLE, ActionPayload::y,
            ByteBufCodecs.DOUBLE, ActionPayload::z,
            ActionPayload::new);

    public static ActionPayload of(int action) {
        return new ActionPayload(action, 0, 0, 0, 0);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PowerManager.handle(player, payload);
            }
        });
    }
}
