package com.fake;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.util.Unit;

public class FakeMod implements ModInitializer {
    @Override
    public void onInitialize() {
        DefaultItemComponentEvents.MODIFY.register(ctx ->
            ctx.modify(Items.LEATHER_CHESTPLATE, b ->
                b.add(DataComponentTypes.GLIDER, Unit.INSTANCE)));
    }
}
