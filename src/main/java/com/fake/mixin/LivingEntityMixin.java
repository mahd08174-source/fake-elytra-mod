package com.fake.mixin;

import com.fake.FakeGlide;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "isGliding", at = @At("HEAD"), cancellable = true)
    private void fakemod$isGliding(CallbackInfoReturnable<Boolean> cir) {
        if (FakeGlide.visualOnly((LivingEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getGlidingTicks", at = @At("HEAD"), cancellable = true)
    private void fakemod$getGlidingTicks(CallbackInfoReturnable<Integer> cir) {
        if (FakeGlide.visualOnly((LivingEntity) (Object) this)) {
            cir.setReturnValue(FakeGlide.ticks());
        }
    }
}
