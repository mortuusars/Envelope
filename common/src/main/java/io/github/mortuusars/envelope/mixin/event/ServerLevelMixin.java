package io.github.mortuusars.envelope.mixin.event;

import io.github.mortuusars.envelope.event.CommonEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "saveLevelData", at = @At("HEAD"))
    private void onSaveLevelData(CallbackInfo ci) {
        CommonEvents.saveLevelData((ServerLevel)(Object)this);
    }
}
