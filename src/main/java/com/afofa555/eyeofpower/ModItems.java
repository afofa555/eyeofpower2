package com.afofa555.eyeofpower;

import com.afofa555.eyeofpower.item.BloodCloakItem;
import com.afofa555.eyeofpower.item.EyeOfPowerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EyeOfPowerMod.MODID);

    public static final DeferredItem<EyeOfPowerItem> EYE_OF_POWER = ITEMS.register("eye_of_power",
            () -> new EyeOfPowerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<BloodCloakItem> BLOOD_CLOAK = ITEMS.register("blood_cloak",
            () -> new BloodCloakItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // Служебные предметы: модели амулета на груди (в творческом меню не показываются)
    public static final DeferredItem<Item> EYE_WORN = ITEMS.registerSimpleItem("eye_of_power_worn");
    public static final DeferredItem<Item> EYE_WORN_DORMANT = ITEMS.registerSimpleItem("eye_of_power_worn_dormant");
}
