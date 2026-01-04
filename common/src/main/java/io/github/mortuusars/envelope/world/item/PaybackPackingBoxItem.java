package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.inventory.PaybackPackingMenu;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PaybackPackingBoxItem extends Item implements PackingBox {
    public PaybackPackingBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
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
        @Nullable StoredItemStack subject = stack.get(Envelope.DataComponents.PAYBACK_SUBJECT);
        if (subject == null || subject.isEmpty() || !subject.getForReading().has(Envelope.DataComponents.PAYBACK_TAG)) {
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