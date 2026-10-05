package com.fake;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

public class FakeMod implements ModInitializer {
    @Override
    public void onInitialize() {
        DefaultItemComponentEvents.MODIFY.register(ctx -> {
            // Leather chestplate = fake elytra: name, wings model, no armor.
            // NOTE: no GLIDER component on purpose, so the game never tries to
            // really glide (no rubber-banding). The glide look is visual only,
            // see FakeGlide + LivingEntityMixin.
            ctx.modify(Items.LEATHER_CHESTPLATE, b -> {
                b.add(DataComponentTypes.ITEM_NAME, Text.literal("Elytra"));
                b.add(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.CHEST)
                    .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA)
                    .model(EquipmentAssetKeys.ELYTRA)
                    .damageOnHurt(false)
                    .build());
                b.add(DataComponentTypes.RARITY, Rarity.EPIC);
                b.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            });
            // Sea pickle = fake netherite ingot.
            ctx.modify(Items.SEA_PICKLE, b ->
                b.add(DataComponentTypes.ITEM_NAME, Text.literal("Netherite Ingot")));
        });
    }
}
