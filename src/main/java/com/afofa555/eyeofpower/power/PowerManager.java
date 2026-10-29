package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.ModItems;
import com.afofa555.eyeofpower.ModSounds;
import com.afofa555.eyeofpower.item.EyeOfPowerItem;
import com.afofa555.eyeofpower.network.ActionPayload;
import com.afofa555.eyeofpower.network.StatePayload;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Точка входа серверной логики: действия игроков, тики, события. */
@EventBusSubscriber(modid = EyeOfPowerMod.MODID)
public class PowerManager {

    private static final Set<UUID> flightGranted = new HashSet<>();

    // ---------------- Помощники ----------------

    public static boolean isWearingEye(Player p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EYE_OF_POWER.get());
    }

    public static boolean isEyeActive(Player p) {
        ItemStack s = p.getItemBySlot(EquipmentSlot.HEAD);
        return s.is(ModItems.EYE_OF_POWER.get()) && EyeOfPowerItem.isActive(s);
    }

    public static boolean isWearingCloak(Player p) {
        return p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BLOOD_CLOAK.get());
    }

    public static void message(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
    }

    public static void playAll(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    // ---------------- Действия от клиента ----------------

    public static void handle(ServerPlayer player, ActionPayload a) {
        if (!isWearingEye(player)) {
            message(player, "message.eyeofpower.not_wearing");
            return;
        }
        if (a.action() == ActionPayload.AWAKEN) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!EyeOfPowerItem.isActive(head)) {
                EyeOfPowerItem.setActive(head);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.EYE_AWAKEN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
                PacketDistributor.sendToAllPlayers(StatePayload.simple(StatePayload.AWAKEN, 1, player.getUUID()));
            }
            return;
        }
        if (!isEyeActive(player)) {
            message(player, "message.eyeofpower.dormant");
            return;
        }
        switch (a.action()) {
            case ActionPayload.TIME_STOP -> TimePowers.toggleTimeStop(player);
            case ActionPayload.TIME_ACCEL -> TimePowers.toggleAccel(player);
            case ActionPayload.BLACK_HOLE -> BlackHolePower.toggle(player);
            case ActionPayload.BLOOD_SKY -> WorldFxPower.toggleBlood(player);
            case ActionPayload.MIRROR -> WorldFxPower.toggleMirror(player);
            case ActionPayload.CLONE_SPAWN -> ClonePower.spawn(player);
            case ActionPayload.CLONE_DISMISS -> ClonePower.dismissAll(player);
            case ActionPayload.CLONE_SWAP -> ClonePower.swap(player, a.arg());
            case ActionPayload.PORTAL_OPEN -> PortalPower.open(player, a.x(), a.y(), a.z());
            case ActionPayload.PORTAL_CLOSE -> PortalPower.close(player);
            default -> { }
        }
    }

    // ---------------- События ----------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        TimePowers.tick(server);
        BlackHolePower.tick(server);
        WorldFxPower.tick(server);
        ClonePower.tick(server);
        PortalPower.tick(server);
    }

    // Полёт в плаще (проверка раз в 5 тиков - почти бесплатно)
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0) {
            return;
        }
        if (isWearingCloak(p)) {
            if (!p.getAbilities().mayfly) {
                p.getAbilities().mayfly = true;
                p.onUpdateAbilities();
                flightGranted.add(p.getUUID());
            }
        } else if (flightGranted.remove(p.getUUID())) {
            if (!p.isCreative() && !p.isSpectator()) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TimePowers.syncTo(sp);
            BlackHolePower.syncTo(sp);
            WorldFxPower.syncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        flightGranted.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        TimePowers.reset(server);
        BlackHolePower.reset();
        WorldFxPower.reset();
        ClonePower.reset();
        PortalPower.reset();
        flightGranted.clear();
    }

    // Замороженные игроки не могут действовать
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (TimePowers.isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (TimePowers.isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (TimePowers.isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (TimePowers.isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (TimePowers.isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (TimePowers.isFrozenPlayer(e.getPlayer())) e.setCanceled(true);
    }
}
