package com.fake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Fake sidebar scoreboard on the right side of the screen. Client-side only. Press - to edit. */
public final class FakeScoreboard {
    private static boolean minusHeld = false;

    private FakeScoreboard() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_MINUS) == GLFW.GLFW_PRESS;
            if (down && !minusHeld && client.currentScreen == null && client.player != null) {
                client.setScreen(new FakeScoreboardScreen());
            }
            minusHeld = down;
        });

        HudRenderCallback.EVENT.register((DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            TextRenderer tr = mc.textRenderer;

            String name = FakeConfig.scoreboardName();
            String money = FakeConfig.scoreboardMoney();
            String dollar = "$ ";

            int w = Math.max(tr.getWidth(name), tr.getWidth(dollar + money));
            int pad = 3;
            int lineH = 9;
            int boxW = w + pad * 2;
            int boxH = lineH * 2 + pad * 2;
            int right = ctx.getScaledWindowWidth() - 1;
            int left = right - boxW;
            int top = ctx.getScaledWindowHeight() / 2 - boxH / 2;

            ctx.fill(left, top, right, top + boxH, 0x50000000);
            ctx.drawText(tr, name, left + pad, top + pad, 0xFFFFFFFF, true);
            ctx.drawText(tr, "$", left + pad, top + pad + lineH, 0xFF55FF55, true);
            ctx.drawText(tr, money, left + pad + tr.getWidth(dollar), top + pad + lineH, 0xFFFFFFFF, true);
        });
    }
}
