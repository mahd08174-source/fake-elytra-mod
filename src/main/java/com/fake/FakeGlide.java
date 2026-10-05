package com.fake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Fake elytra glide for the local player. 100% visual: real movement, hitbox,
 * and physics are never changed.
 *
 * - Pose is visual only (pose mixin reports gliding only while rendering).
 * - Glide STARTS only when you press jump again while in the air (like a real elytra).
 * - The slow fall is faked with the camera: while gliding, the camera descends at a
 *   slow speed while your real body falls normally. The gap is eased back to zero
 *   when the glide ends (landing, water, etc).
 */
public final class FakeGlide {
    /** Real glide eye height is 0.4 vs 1.62 standing. */
    private static final double CAMERA_DROP = 1.2;

    /** How fast the camera appears to fall while gliding (blocks/sec). */
    private static final double VISUAL_FALL_SPEED = 2.0;
    /** Max distance the camera may hover above your real position (blocks). */
    private static final double MAX_LAG = 12.0;
    /** How fast the camera catches up to the real position when the glide ends (1/sec). */
    private static final double CATCH_UP_RATE = 8.0;

    private static volatile boolean inTick = false;
    private static volatile boolean active = false;
    private static volatile int ticks = 0;
    private static int airTicks = 0;
    private static boolean jumpHeld = false;

    private static float blend = 0f;
    private static double lag = 0.0;
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

    /**
     * Vertical camera offset, called once per frame.
     * Negative part = low glide eye height. Positive part = camera hovering above the
     * real body so the fall LOOKS slow.
     */
    public static double cameraYOffset() {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        float target = active ? 1f : 0f;
        blend += (target - blend) * (1f - (float) Math.exp(-10.0 * dt));
        if (Math.abs(blend) < 0.001f) blend = 0f;

        if (active) {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            double realDown = p == null ? 0.0 : Math.max(0.0, -p.getVelocity().y * 20.0);
            lag += (realDown - VISUAL_FALL_SPEED) * dt;
            if (lag < 0.0) lag = 0.0;
            if (lag > MAX_LAG) lag = MAX_LAG;
        } else {
            lag *= Math.exp(-CATCH_UP_RATE * dt);
            if (lag < 0.001) lag = 0.0;
        }

        return -CAMERA_DROP * blend + lag;
    }
}
