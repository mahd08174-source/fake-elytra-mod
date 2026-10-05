package com.fake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Purely visual glide for the local player (pose + camera).
 *
 * The pose mixin only reports "gliding" OUTSIDE of the client tick (i.e. while
 * rendering frames). During the tick, where movement/physics/packets run,
 * the game sees the real value, so nothing about your movement changes.
 * The camera shift is applied after the camera is positioned, so your real
 * position, hitbox and reach are untouched.
 */
public final class FakeGlide {
    /** Real glide eye height is 0.4 vs 1.62 standing. */
    private static final double CAMERA_DROP = 1.2;

    private static volatile boolean inTick = false;
    private static volatile boolean active = false;
    private static volatile int ticks = 0;
    private static int airTicks = 0;

    private static float blend = 0f;
    private static long lastNanos = 0L;

    private FakeGlide() {}

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> inTick = true);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            inTick = false;
            update(client.player);
        });
    }

    private static void update(ClientPlayerEntity p) {
        if (p == null || p.isSpectator() || p.isSleeping()) {
            active = false;
            ticks = 0;
            airTicks = 0;
            return;
        }

        ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
        boolean gear = chest.isOf(Items.LEATHER_CHESTPLATE) || chest.isOf(Items.ELYTRA);

        boolean air = !p.isOnGround()
                && !p.isTouchingWater()
                && !p.isInLava()
                && !p.hasVehicle()
                && !p.isClimbing();

        airTicks = air ? airTicks + 1 : 0;
        active = gear && airTicks >= 3;
        ticks = active ? ticks + 1 : 0;
    }

    /** True only for the local player, only while rendering, only when the fake glide is on. */
    public static boolean visualOnly(LivingEntity entity) {
        return active && !inTick && entity == MinecraftClient.getInstance().player;
    }

    public static int ticks() {
        return ticks;
    }

    /** Vertical camera offset (negative = lower), eased per frame so it glides in and out smoothly. */
    public static double cameraYOffset() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        float target = active ? 1f : 0f;
        blend += (target - blend) * (1f - (float) Math.exp(-10.0 * dt));
        if (Math.abs(blend) < 0.001f) blend = 0f;
        return -CAMERA_DROP * blend;
    }
}
