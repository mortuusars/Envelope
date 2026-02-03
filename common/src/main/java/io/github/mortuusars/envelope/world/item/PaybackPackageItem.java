package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.inventory.PaybackPackageMenu;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
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
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.PACKAGE_CONTENTS));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable PaybackSubject subject = stack.get(Envelope.DataComponents.PAYBACK_SUBJECT);
        if (subject != null && !subject.mail().isEmpty()) {
            tooltipComponents.add(Component.literal("⌛ ")
                  .append(GameTime.formatLargest(subject.timeoutTick() - Minecrft.level().getGameTime(), false))
                  .withStyle(DeliveryRecord.MessageType.NEGATIVE.getStyle()));
        }
    }

    // --

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer serverPlayer) {
            SimpleMenuProvider menuProvider = new SimpleMenuProvider((id, inventory, pl) ->
                  new PaybackPackageMenu(id, inventory, hand), stack.getHoverName());
            PlatformHelper.openMenu(serverPlayer, menuProvider, buffer ->
                  buffer.writeEnum(hand));
        }

        player.level().playSound(player, player, Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS, 0.6f, 0.95f);
        return InteractionResultHolder.success(stack);
    }
}