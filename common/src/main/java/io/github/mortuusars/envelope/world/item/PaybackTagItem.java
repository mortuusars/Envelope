package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import io.github.mortuusars.envelope.world.item.component.PaybackTagContents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
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

import java.util.List;
import java.util.Objects;
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

        PaybackTagContents contents = stack.getOrDefault(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackTagContents.EMPTY);
        @Nullable PaybackRequest existingPaybackRequest = slot.getItem().get(Envelope.DataComponents.MAIL_PAYBACK_REQUEST);

        if (contents.isEmpty()) {
            if (existingPaybackRequest == null) {
                return true; // do nothing
            }
            slot.getItem().remove(Envelope.DataComponents.MAIL_PAYBACK_REQUEST);
        } else {
            PaybackRequest paybackRequest = createPayback(player.level(), stack);
            if (existingPaybackRequest != null && existingPaybackRequest.equals(paybackRequest)) {
                return true; // do nothing
            }
            slot.getItem().set(Envelope.DataComponents.MAIL_PAYBACK_REQUEST, paybackRequest);
        }

        slot.setChanged();
        stack.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        return true;
    }

    public PaybackRequest createPayback(Level level, ItemStack stack) {
        PaybackTagContents tagContents = stack.getOrDefault(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackTagContents.DEFAULT);

        List<StackIngredient> stackIngredients = tagContents.getItemsForReading().stream()
              .limit(PaybackRequest.SLOTS)
              .filter(item -> !item.isEmpty())
              .map(this::createRequestedItemFromStack)
              .toList();

        return PaybackRequest.createOrDefault(stackIngredients);
    }

    public StackIngredient createRequestedItemFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            Envelope.LOGGER.warn("Tried to create RequestedItem from empty ItemStack.");
            return StackIngredient.createDefault();
        }

        DataComponentMap defaultComponents = new ItemStack(stack.getItem(), stack.getCount()).getComponents();
        DataComponentMap components = stack.copy().getComponents();
        DataComponentMap uniqueComponents = components.filter(type ->
              !Objects.equals(components.get(type), defaultComponents.get(type)));
        return new StackIngredient(HolderSet.direct(stack.getItemHolder()), stack.getCount(),
              DataComponentPredicate.allOf(uniqueComponents), false);
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
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlatformHelper.openMenu(
                  serverPlayer,
                  new SimpleMenuProvider((id, inventory, pl) ->
                        new PaybackTagMenu(id, inventory, usedHand), Component.translatable("container.envelope.payback_tag")),
                  buffer -> buffer.writeEnum(usedHand));
            player.getCooldowns().addCooldown(this, 3);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }

    @Override
    public boolean shouldRenderTooltipWhileCarrying(Level level, ItemStack carried, ItemStack hovered) {
        return true;
    }
}