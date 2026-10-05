package com.fake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * Fake elytra glide for the local player.
 *
 * - Pose/camera are visual only (pose mixin reports gliding only while rendering).
 * - Glide STARTS only when you press jump again while in the air (like a real elytra),
 *   not on a normal jump.
 * - While gliding, falling speed is capped so you descend slowly. Hitbox stays normal.
 */
public final class FakeGlide {
    /** Real glide eye height is 0.4 vs 1.62 standing. */
    private static final double CAMERA_DROP = 1.2;

    /** Fall speed is eased toward this (blocks/tick) so the descent is smooth, ~0.1 b/tick in practice. */
    private static final double TARGET_FALL = -0.025;
    /** How fast velocity is eased toward the target each tick (0..1). */
    private static final double EASE = 0.5;

    private static volatile boolean inTick = false;
    private static volatile boolean active = false;
    private static volatile int ticks = 0;
    private static int airTicks = 0;
    private static boolean jumpHeld = false;

    private static float blend = 0f;
    private static long lastNanos = 0L;

    private FakeGlide() {}

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> inTick = true);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            inTick = false;
            update(client);
        });
    }

    private static void reset() {
        active = false;
        ticks = 0;
        airTicks = 0;
        jumpHeld = false;
    }

    private static void update(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || p.isSpectator() || p.isSleeping()) {
            reset();
            return;
        }

        ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
        boolean gear = chest.isOf(Items.LEATHER_CHESTPLATE) || chest.isOf(Items.ELYTRA);

        boolean air = !p.isOnGround()
                && !p.isTouchingWater()
                && !p.isInLava()
                && !p.hasVehicle()
                && !p.isClimbing();

        boolean jump = client.options.jumpKey.isPressed();
        airTicks = air ? airTicks + 1 : 0;

        if (!gear || !air) {
            active = false;
        } else if (!active && jump && !jumpHeld && airTicks >= 2) {
            // fresh jump press while already in the air -> start gliding
            active = true;
        }
        jumpHeld = jump;

        ticks = active ? ticks + 1 : 0;

        // Slow the fall like a real glide (hitbox/pose untouched).
        if (active) {
            Vec3d v = p.getVelocity();
            if (v.y < TARGET_FALL) {
                p.setVelocity(v.x, v.y + (TARGET_FALL - v.y) * EASE, v.z);
            }
        }
    }

    /** True only for the local player, only while rendering, only when the fake glide is on. */
    public static boolean visualOnly(LivingEntity entity) {
        return active && !inTick && entity == MinecraftClient.getInstance().player;
    }

    /** True for the local player whenever the fake glide is on (tick or render). */
    public static boolean isActiveFor(LivingEntity entity) {
        return active && entity == MinecraftClient.getInstance().player;
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
