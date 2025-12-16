package io.github.mortuusars.envelope.mixin.tooltip;

import io.github.mortuusars.envelope.EnvelopeClient;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin {
    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
          at = @At("HEAD"),
          cancellable = true)
    private static void onCreate(TooltipComponent component, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        @Nullable ClientTooltipComponent clientComponent = EnvelopeClient.TooltipComponents.create(component);
        if (clientComponent != null) {
            cir.setReturnValue(clientComponent);
        }
    }
}