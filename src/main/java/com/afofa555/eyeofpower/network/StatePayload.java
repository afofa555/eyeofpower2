package com.afofa555.eyeofpower.network;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.client.ClientState;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер -> клиент: состояние мировых эффектов. */
public record StatePayload(int kind, int value, UUID owner, double x, double y, double z, String dim)
        implements CustomPacketPayload {
    public static final int TIME = 0;
    public static final int BLOOD = 1;
    public static final int MIRROR = 2;
    public static final int ACCEL = 3;
    public static final int HOLE = 4;
    public static final int AWAKEN = 5;

    public static final UUID NONE = new UUID(0L, 0L);

    public static final CustomPacketPayload.Type<StatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EyeOfPowerMod.MODID, "state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC =
            StreamCodec.ofMember(StatePayload::write, StatePayload::read);

    public static StatePayload simple(int kind, int value, UUID owner) {
        return new StatePayload(kind, value, owner == null ? NONE : owner, 0, 0, 0, "");
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeVarInt(value);
        buf.writeUUID(owner);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeUtf(dim);
    }

    private static StatePayload read(RegistryFriendlyByteBuf buf) {
        return new StatePayload(buf.readVarInt(), buf.readVarInt(), buf.readUUID(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientState.apply(payload, context.player().getUUID()));
    }
}
