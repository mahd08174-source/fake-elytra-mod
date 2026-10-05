package com.fake;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Fake sidebar scoreboard drawn on the right side of the screen. Client-side only. */
public final class FakeScoreboard {
    private static final String LINE_1 = "Kingtemanke";
    private static final String LINE_2 = "72.1b cash";

    private FakeScoreboard() {}

    public static void init() {
        HudRenderCallback.EVENT.register((DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            TextRenderer tr = mc.textRenderer;

            int w = Math.max(tr.getWidth(LINE_1), tr.getWidth(LINE_2));
            int pad = 3;
            int lineH = 9;
            int boxW = w + pad * 2;
            int boxH = lineH * 2 + pad * 2;
            int right = ctx.getScaledWindowWidth() - 1;
            int left = right - boxW;
            int top = ctx.getScaledWindowHeight() / 2 - boxH / 2;

            ctx.fill(left, top, right, top + boxH, 0x50000000);
            ctx.drawText(tr, LINE_1, left + pad, top + pad, 0xFFFFFFFF, true);
            ctx.drawText(tr, LINE_2, left + pad, top + pad + lineH, 0xFF55FF55, true);
        });
    }
}
