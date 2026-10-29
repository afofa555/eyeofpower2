package com.afofa555.eyeofpower.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, ActionPayload::handle);
        registrar.playToClient(StatePayload.TYPE, StatePayload.CODEC, StatePayload::handle);
        registrar.playToClient(CloneListPayload.TYPE, CloneListPayload.CODEC, CloneListPayload::handle);
    }
}
