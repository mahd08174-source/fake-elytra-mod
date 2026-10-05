package com.fake;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class FakeModClient implements ClientModInitializer {
    // Edit these to change the fake prices.
    private static final long ELYTRA_PRICE = 240_000_000L;  // 240M
    private static final long NETHERITE_INGOT_PRICE = 5_000_000L;  // 5M each

    @Override
    public void onInitializeClient() {
        FakeGlide.init();
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> applyFakePrice(stack, lines));
    }

    private static void applyFakePrice(ItemStack stack, List<Text> lines) {
        long unit;
        // Real items and their fakes (leather chestplate = elytra, sea pickle = netherite ingot)
        if (stack.isOf(Items.ELYTRA) || stack.isOf(Items.LEATHER_CHESTPLATE)) {
            unit = ELYTRA_PRICE;
        } else if (stack.isOf(Items.NETHERITE_INGOT) || stack.isOf(Items.SEA_PICKLE)) {
            unit = NETHERITE_INGOT_PRICE;
        } else {
            return;
        }

        MutableText line = Text.literal("~").formatted(Formatting.GRAY)
                .append(Text.literal("$").formatted(Formatting.GREEN))
                .append(Text.literal(" " + fmt(unit)).formatted(Formatting.WHITE));

        int count = stack.getCount();
        if (count > 1) {
            line.append(Text.literal(" each").formatted(Formatting.GRAY));
            line.append(Text.literal(" (total " + fmt(unit * count) + ")").formatted(Formatting.DARK_GRAY));
        }

        // Replace the server's price line if there is one, otherwise add ours.
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains("$")) {
                lines.set(i, line);
                return;
            }
        }
        lines.add(line);
    }

    private static String fmt(long v) {
        if (v >= 1_000_000_000L) return trim(v / 1_000_000_000.0) + "B";
        if (v >= 1_000_000L) return trim(v / 1_000_000.0) + "M";
        if (v >= 1_000L) return trim(v / 1_000.0) + "K";
        return Long.toString(v);
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? Long.toString((long) d) : String.format("%.1f", d);
    }
}
