package com.afofa555.eyeofpower.network;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.client.ClientState;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер -> клиент: id клонов игрока (для меню выбора клона). */
public record CloneListPayload(List<Integer> ids) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CloneListPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, "clone_list"));

    public static final StreamCodec<ByteBuf, CloneListPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CloneListPayload::ids,
            CloneListPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CloneListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientState.cloneIds = List.copyOf(payload.ids()));
    }
}
