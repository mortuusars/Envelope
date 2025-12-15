package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.item.component.PaybackTagContents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class PaybackTagItem extends Item implements ApplicatorItem {
    public PaybackTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable PaybackTagContents paybackTagContents = stack.get(Envelope.DataComponents.PAYBACK_TAG_CONTENTS);
        if (paybackTagContents != null) {
            tooltipComponents.add(Component.literal("Payback:").withStyle(ChatFormatting.RED));
            paybackTagContents.getItemsForReading().forEach(item -> {
                if (!item.isEmpty()) {
                    MutableComponent name = Component.empty().append(item.getHoverName());
                    if (item.getCount() > 1) {
                        name.append(" " + item.getCount() + "x");
                    }
                    tooltipComponents.add(name.withStyle(ChatFormatting.DARK_RED));
                }
            });
        }
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        if (!slot.allowModification(player)) return false;

        ItemStack target = slot.getItem();
        if (!target.is(Envelope.Tags.Items.MAILABLE)) {
            return true;
        }

        PaybackTagContents contents = stack.getOrDefault(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackTagContents.EMPTY);
        @Nullable Payback existingPayback = target.get(Envelope.DataComponents.PAYBACK);

        if (contents.isEmpty()) {
            if (existingPayback == null) {
                return true; // do nothing
            }
            target.remove(Envelope.DataComponents.PAYBACK);
        } else {
            Payback payback = createPayback(player.level(), stack);
            if (existingPayback != null && existingPayback.equals(payback)) {
                return true; // do nothing
            }
            target.set(Envelope.DataComponents.PAYBACK, payback);
        }

        slot.setChanged();
        stack.shrink(1);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
        return true;
    }

    public Payback createPayback(Level level, ItemStack stack) {
        PaybackTagContents tagContents = stack.getOrDefault(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackTagContents.DEFAULT);

        List<RequestedItem> requestedItems = tagContents.getItemsForReading().stream()
              .limit(Payback.SLOTS)
              .filter(item -> !item.isEmpty())
              .map(this::createRequestedItemFromStack)
              .toList();

        return Payback.createOrDefault(requestedItems);
    }

    public RequestedItem createRequestedItemFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            Envelope.LOGGER.warn("Tried to create RequestedItem from empty ItemStack.");
            return RequestedItem.DEFAULT;
        }

        DataComponentMap defaultComponents = new ItemStack(stack.getItem(), stack.getCount()).getComponents();
        DataComponentMap components = stack.copy().getComponents();
        DataComponentMap uniqueComponents = components.filter(type ->
              !Objects.equals(components.get(type), defaultComponents.get(type)));
        return new RequestedItem(stack.getItem(), stack.getCount(), DataComponentPredicate.allOf(uniqueComponents));
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