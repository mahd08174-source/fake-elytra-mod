package com.fake.mixin;

import com.fake.FakeGlide;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ElytraFlightController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wing angles are computed in update(), which runs inside the client tick where
 * the fake glide is hidden from the game, so the wings never opened. While the
 * fake glide is on, drive the wings open ourselves (fully spread, ignoring the
 * fall-direction folding vanilla does).
 */
@Mixin(ElytraFlightController.class)
public abstract class ElytraFlightControllerMixin {
    @Shadow private float leftWingPitch;
    @Shadow private float leftWingYaw;
    @Shadow private float leftWingRoll;
    @Shadow private float lastLeftWingPitch;
    @Shadow private float lastLeftWingYaw;
    @Shadow private float lastLeftWingRoll;
    @Shadow @Final private LivingEntity entity;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void fakemod$spreadWings(CallbackInfo ci) {
        if (!FakeGlide.isActiveFor(this.entity)) return;

        this.lastLeftWingPitch = this.leftWingPitch;
        this.lastLeftWingYaw = this.leftWingYaw;
        this.lastLeftWingRoll = this.leftWingRoll;

        this.leftWingPitch += (0.34906584F - this.leftWingPitch) * 0.2F;
        this.leftWingYaw += (0.0F - this.leftWingYaw) * 0.2F;
        this.leftWingRoll += (-1.5707964F - this.leftWingRoll) * 0.2F;
        ci.cancel();
    }
}
