package io.github.mortuusars.envelope.mixin.package_tooltip;

import io.github.mortuusars.envelope.client.gui.tooltip.ClientPackageTooltip;
import io.github.mortuusars.envelope.client.gui.tooltip.ClientSealTooltip;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
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
        if (component instanceof PackageContents packageContents) {
            cir.setReturnValue(new ClientPackageTooltip(packageContents));
        }

        if (component instanceof Seal seal) {
            cir.setReturnValue(new ClientSealTooltip(seal));
        }
    }
}