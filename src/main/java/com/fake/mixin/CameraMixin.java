package com.fake.mixin;

import com.fake.FakeGlide;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void fakemod$glideCamera(CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        if (self.getFocusedEntity() != MinecraftClient.getInstance().player) return;

        double dy = FakeGlide.cameraYOffset();
        if (dy == 0.0) return;

        Vec3d p = self.getPos();
        this.setPos(p.x, p.y + dy, p.z);
    }
}
