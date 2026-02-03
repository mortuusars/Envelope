package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.inventory.PaybackPackingMenu;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PaybackBoxItem extends Item {
    public PaybackBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
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
        if (openPackingGui(player, hand, stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.fail(stack);
    }

    public boolean openPackingGui(Player player, InteractionHand hand, ItemStack stack) {
        @Nullable PaybackSubject subject = stack.get(Envelope.DataComponents.PAYBACK_SUBJECT);
        if (subject == null || subject.mail().isEmpty() || !subject.mail().has(Envelope.DataComponents.MAIL_PAYBACK_REQUEST)) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PlatformHelper.openMenu(serverPlayer, new SimpleMenuProvider((id, inventory, pl) ->
                        new PaybackPackingMenu(id, inventory, hand), stack.getHoverName()),
                  buffer -> buffer.writeEnum(hand));
        }

        player.level().playSound(player, player, Envelope.SoundEvents.PAPER_USE.get(), SoundSource.PLAYERS, 0.6f, 0.95f);
        return true;
    }
}