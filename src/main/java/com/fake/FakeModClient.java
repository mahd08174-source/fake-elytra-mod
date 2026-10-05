package com.fake;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class FakeModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FakeConfig.load();
        FakeGlide.init();
        FakeScoreboard.init();

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> applyPrice(stack, lines));

        // /fakemod key <apikey>   -> save your DonutSMP API key (from /api)
        // /fakemod prices         -> show what the mod currently thinks the prices are
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("fakemod")
                        .then(ClientCommandManager.literal("key")
                                .then(ClientCommandManager.argument("key", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            FakeConfig.setApiKey(StringArgumentType.getString(ctx, "key"));
                                            AhPrices.reset();
                                            ctx.getSource().sendFeedback(Text.literal("[fakemod] API key saved"));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("prices")
                                .executes(ctx -> {
                                    AhPrices.get("elytra");
                                    AhPrices.get("netherite_ingot");
                                    String key = FakeConfig.apiKey().isEmpty() ? "no API key set (using fallback prices)" : "API key set";
                                    ctx.getSource().sendFeedback(Text.literal("[fakemod] " + key));
                                    ctx.getSource().sendFeedback(Text.literal(AhPrices.describe("elytra")));
                                    ctx.getSource().sendFeedback(Text.literal(AhPrices.describe("netherite_ingot")));
                                    return 1;
                                }))));
    }

    private static void applyPrice(ItemStack stack, List<Text> lines) {
        long unit;
        // Real items and their fakes (leather chestplate = elytra, sea pickle = netherite ingot)
        if (stack.isOf(Items.ELYTRA) || stack.isOf(Items.LEATHER_CHESTPLATE)) {
            Long ah = AhPrices.get("elytra");
            unit = ah != null ? ah : FakeConfig.elytraFallback();
        } else if (stack.isOf(Items.NETHERITE_INGOT) || stack.isOf(Items.SEA_PICKLE)) {
            Long ah = AhPrices.get("netherite_ingot");
            unit = ah != null ? ah : FakeConfig.netheriteIngotFallback();
        } else {
            return;
        }

        MutableText line = Text.literal("~").formatted(Formatting.GRAY)
                .append(Text.literal("$").formatted(Formatting.GREEN))
                .append(Text.literal(" " + fmt(unit)).formatted(Formatting.WHITE));

        // Replace the server's price line if there is one, otherwise add ours.
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains("$")) {
                lines.set(i, line);
                return;
            }
        }
        lines.add(line);
    }

    public static String fmt(long v) {
        if (v >= 1_000_000_000L) return trim(v / 1_000_000_000.0) + "B";
        if (v >= 1_000_000L) return trim(v / 1_000_000.0) + "M";
        if (v >= 1_000L) return trim(v / 1_000.0) + "K";
        return Long.toString(v);
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? Long.toString((long) d) : String.format("%.1f", d);
    }
}
