package io.github.mortuusars.envelope.mixin.jei;

import io.github.mortuusars.envelope.integration.jei.JeiCompatibleScreen;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.handlers.GlobalInputHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(GlobalInputHandler.class)
public class JeiKeyConflictContextMixin {
    /**
     * Fixes keys such as Ctrl+O used in formatting.
     */
    @Inject(method = "handleUserInput", at = @At("HEAD"), cancellable = true)
    private void onHandleUserInput(Screen screen, UserInput input, IInternalKeyMappings keyBindings, CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        if (screen instanceof JeiCompatibleScreen resolverScreen && resolverScreen.shouldBlockJeiInput()) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
