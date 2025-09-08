package io.github.mortuusars.envelope.mixin.mail_tooltip;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.mail.Address;
import io.github.mortuusars.envelope.mail.log.MailDeliveryLog;
import io.github.mortuusars.envelope.mail.log.TravelingRecord;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {
    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void appendHoverText(Item.TooltipContext tooltipContext, Player player,
                                 TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir, @Local Consumer<Component> consumer) {

        @Nullable Address sender = get(Envelope.DataComponents.MAIL_SENDER);
        if (sender != null) {
            consumer.accept(Component.translatable("gui.envelope.mail.sender", sender.getDisplayName()));
        }

        @Nullable Address recipient = get(Envelope.DataComponents.MAIL_RECIPIENT);
        if (recipient != null) {
            consumer.accept(Component.translatable("gui.envelope.mail.recipient", recipient.getDisplayName()));
        }

        @Nullable MailDeliveryLog log = get(Envelope.DataComponents.MAIL_DELIVERY_LOG);
        if (log != null && !log.isEmpty()) {
            if (Screen.hasShiftDown()) {
                consumer.accept(Component.translatable("gui.envelope.mail.log"));
                for (TravelingRecord record : log.records()) {
                    consumer.accept(record.translate());
                }
            } else {
                consumer.accept(Component.translatable("gui.envelope.mail.log.show_tooltip"));
            }
        }
    }
}