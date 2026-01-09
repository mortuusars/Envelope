package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackSubject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PaybackPackageItem extends Item {
    public PaybackPackageItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable PaybackSubject subject = stack.get(Envelope.DataComponents.PAYBACK_PACKAGE_SUBJECT);
        if (subject != null && !subject.mail().isEmpty()) {
            tooltipComponents.add(Component.translatable("gui.envelope.time.remaining")
                  .append(GameTime.formatLargest(subject.timeoutTick() - Minecrft.level().getGameTime(), false))
                  .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.PACKAGE_CONTENTS));
    }

    // --

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack = stack.transmuteCopy(Envelope.Items.PAYBACK_PACKING_BOX.get());
        player.setItemInHand(hand, stack);

        ((PaybackPackingBoxItem)stack.getItem()).openPackingGui(player, hand, stack);

        return InteractionResultHolder.success(stack);
    }
}