package com.fake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Purely visual glide pose for the local player.
 *
 * The mixin only reports "gliding" OUTSIDE of the client tick (i.e. while
 * rendering frames). During the tick, where movement/physics/packets run,
 * the game sees the real value, so nothing about your movement changes.
 */
public final class FakeGlide {
    private static volatile boolean inTick = false;
    private static volatile boolean active = false;
    private static volatile int ticks = 0;
    private static int airTicks = 0;

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
}
