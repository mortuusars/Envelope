package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.Platform;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PaybackTagItem extends Item implements ApplicatorItem {
    public PaybackTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        if (!slot.allowModification(player)) return false;
        if (!slot.getItem().is(Envelope.Tags.Items.MAILABLE)) return false;

        @Nullable PaybackRequest request = stack.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
        @Nullable PaybackRequest currentRequest = slot.getItem().get(Envelope.DataComponents.MAIL_PAYBACK_REQUEST);

        if (request != null) {
            if (request.equals(currentRequest)) {
                return true; // do nothing
            }
            slot.getItem().set(Envelope.DataComponents.MAIL_PAYBACK_REQUEST, request);
        } else {
            if (currentRequest == null) {
                return true; // do nothing
            }
            slot.getItem().remove(Envelope.DataComponents.MAIL_PAYBACK_REQUEST);
        }

        slot.setChanged();
        stack.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        return true;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof PigeonholeBlock) {
            return InteractionResult.FAIL;
        }

        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            @Nullable PaybackRequest removedRequest = player.getItemInHand(hand).remove(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
            if (removedRequest != null) {
                player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            Platform.openMenu(
                  serverPlayer,
                  new SimpleMenuProvider((id, inventory, pl) ->
                        new PaybackTagMenu(id, inventory, hand), Component.translatable("container.envelope.payback_tag")),
                  buffer -> buffer.writeEnum(hand));
            player.getCooldowns().addCooldown(this, 3);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean shouldRenderTooltipWhileCarrying(Level level, ItemStack carried, ItemStack hovered) {
        return true;
    }
}