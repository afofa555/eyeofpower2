package com.afofa555.eyeofpower.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class EyeOfPowerItem extends Item {
    private static final String TAG = "afo_awake";

    public EyeOfPowerItem(Properties properties) {
        super(properties);
    }

    public static boolean isActive(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }

    public static void setActive(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(TAG, true));
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return armorType == EquipmentSlot.HEAD;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            stack.setCount(0);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        boolean on = isActive(stack);
        ChatFormatting main = on ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        return Component.literal("✦ ").withStyle(on ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)
                .append(Component.translatable(this.getDescriptionId()).withStyle(main, ChatFormatting.BOLD))
                .append(Component.literal(" afofa555").withStyle(on ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY,
                        ChatFormatting.BOLD, ChatFormatting.ITALIC))
                .append(Component.literal(" ✦").withStyle(on ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isActive(stack)) {
            tooltip.add(Component.translatable("tooltip.eyeofpower.awake").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.eyeofpower.hint").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.eyeofpower.asleep").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("tooltip.eyeofpower.asleep2").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
