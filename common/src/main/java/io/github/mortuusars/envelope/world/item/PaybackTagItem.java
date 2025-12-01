package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.item.component.Payback;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PaybackTagItem extends Item implements ApplicatorItem {
    public PaybackTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        @Nullable Payback payback = stack.get(Envelope.DataComponents.PAYBACK);
        if (payback != null) {
            tooltipComponents.add(Component.literal("Payback:").withStyle(ChatFormatting.RED));
            payback.ingredients().forEach(i -> {
                ItemStack[] items = i.getItems();
                if (items.length > 0) {
                    int index = (int) (Util.getMillis() / 1000) % items.length;
                    ItemStack item = items[index];
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
        ItemStack target = slot.getItem();
        if (!target.is(Envelope.Tags.Items.MAILABLE)) return false;

        Payback payback = stack.getOrDefault(Envelope.DataComponents.PAYBACK, new Payback(Collections.emptyList()));

        @Nullable Payback existingPayback = target.get(Envelope.DataComponents.PAYBACK);
        if (Objects.equals(payback, existingPayback)) {
            return true;
        }

        if (payback.ingredients().isEmpty()) {
            target.remove(Envelope.DataComponents.PAYBACK);
        } else {
            target.set(Envelope.DataComponents.PAYBACK, payback);
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
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlatformHelper.openMenu(
                  serverPlayer,
                  new MenuProvider() {
                      @Override
                      public @NotNull Component getDisplayName() {
                          return Component.translatable("container.envelope.payback_tag");
                      }

                      @Override
                      public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                          return new PaybackTagMenu(i, inventory, usedHand);
                      }
                  },
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
