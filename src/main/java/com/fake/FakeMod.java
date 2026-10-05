package com.fake;

import net.fabricmc.api.ModInitializer;

public class FakeMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Nothing here on purpose: no real glider component is added,
        // so the client never tries to actually glide (no rubber-banding).
        // The glide look is purely visual, see FakeGlide + LivingEntityMixin.
    }
}
