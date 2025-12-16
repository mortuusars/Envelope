package io.github.mortuusars.envelope.mixin.tooltip;

import io.github.mortuusars.envelope.client.gui.tooltip.*;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltipComponent;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.PaybackTagContents;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceKey;
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
            cir.setReturnValue(new PackageTooltip(packageContents));
        } else if (component instanceof Payback payback) {
            cir.setReturnValue(new PaybackTooltip(payback));
        } else if (component instanceof PaybackTagContents paybackTagContents) {
            cir.setReturnValue(new PaybackTagContentsTooltip(paybackTagContents));
        } else if (component instanceof Seal seal) {
            cir.setReturnValue(new SealTooltip(seal));
        } else if (component instanceof SealDieTooltipComponent die) {
            SealImpression impression = die.impression().orElseGet(() -> {
                ResourceKey<SealImpression> key = SealImpression.firstCharOrDefault(Minecrft.player());
                return SealImpression.getHolder(Minecrft.registryAccess(), key);
            }).value();
            cir.setReturnValue(new SealDieTooltip(impression));
        } else if (component instanceof CompositeTooltipComponent composite) {
            cir.setReturnValue(new CompositeClientTooltipComponent(
                  composite.components().stream().map(ClientTooltipComponent::create).toList()
            ));
        }
    }
}