package com.fake;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Items;

public class FakeMod implements ModInitializer {
    @Override
    public void onInitialize() {
        DefaultItemComponentEvents.MODIFY.register(ctx ->
            ctx.modify(Items.LEATHER_CHESTPLATE, b ->
                b.set(DataComponents.GLIDER, Unit.INSTANCE)));
    }
}
