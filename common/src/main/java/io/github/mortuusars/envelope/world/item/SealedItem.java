package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PackageBlockEntity;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.mortaar.client.Minecrft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SealedItem {
    ItemLike getUnsealedItem();

    default SoundEvent getUnsealingSound() {
        return Envelope.SoundEvents.PAPER_CRACKLE.get();
    }

    default SoundEvent getUnsealFinishedSound() {
        return Envelope.SoundEvents.PAPER_TEAR.get();
    }

    default int getUnsealingDuration(ItemStack stack, LivingEntity entity) {
        return Config.Server.SEAL_REMOVE_DURATION.get();
    }

    default boolean canRemoveSeal(ItemStack stack, Player player) {
        if (stack.get(Envelope.DataComponents.SEAL) instanceof Seal seal) {
            return seal.lock().map(lock -> !lock.isLockedFor(player)).orElse(true);
        }

        return true;
    }

    default @NotNull InteractionResultHolder<ItemStack> useSealedItem(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canRemoveSeal(stack, player)) {
            player.displayClientMessage(Component.translatable("gui.envelope.sealed_item.locked").withStyle(ChatFormatting.RED), true);
            player.playSound(SoundEvents.SCULK_BLOCK_FALL);
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    default ItemStack unsealByUsing(ItemStack stack, Level level, @Nullable LivingEntity entity) {
        if (entity instanceof Player player && !canRemoveSeal(stack, player)) {
            return stack;
        }

        ItemStack unsealedStack = unseal(stack);
        onUnsealed(stack, unsealedStack, level, entity);
        return unsealedStack;
    }

    default ItemStack unseal(ItemStack stack) {
        ItemStack unsealedStack = stack.transmuteCopy(getUnsealedItem());
        unsealedStack.remove(Envelope.DataComponents.SEAL);
        return unsealedStack;
    }

    default void onUnsealed(ItemStack stack, ItemStack unsealedStack, Level level, @Nullable LivingEntity entity) {
        if (entity != null) {
            @Nullable Player player = entity instanceof Player pl ? pl : null;
            level.playSound(player, entity, getUnsealFinishedSound(), SoundSource.PLAYERS, 1f,
                  level.getRandom().nextFloat() * 0.1f + 0.8f);
        }

        if (level.isClientSide()) {
            // Release use key after opening.
            // Otherwise, right click will be still held and will activate use again.
            Minecrft.releaseUseButton();
        }

        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(Envelope.Stats.SEALS_BROKEN.get());
        }
    }

    // --

    class Client {
        public static int LOCKED_HIGHLIGHT_OVERLAY_COLOR = 0xFF18A2B6;

        public static int getSealOverlayColor(ItemStack stack, int layer) {
            if (layer != 1) {
                return -1;
            }

            @Nullable Seal seal = stack.get(Envelope.DataComponents.SEAL);
            if (seal != null) {
                int materialColor = seal.material().value().modelTintColor();
                return seal.lock()
                      .map(lock -> {
                          if (lock.isLocked(Minecrft.level())) {
                              double time = (Minecrft.level().getGameTime() + Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true)) / 20.0;
                              double beat = Math.pow((Math.sin(time * Math.PI * 2 * 1) + 1.0) * 0.5, 10);
                              float delta = (float) Mth.lerp(beat, 0, 1);
                              return FastColor.ARGB32.lerp(delta, materialColor, LOCKED_HIGHLIGHT_OVERLAY_COLOR);
                          } else {
                              return materialColor;
                          }
                      })
                      .orElse(materialColor);
            }

            return 0xFFCC4E47; // Default red color
        }

        public static int getSealOverlayColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int index) {
            if (index != 0) {
                return -1;
            }

            if (level != null && pos != null && level.getBlockEntity(pos) instanceof PackageBlockEntity blockEntity) {
                @Nullable Seal seal = blockEntity.getPackage().get(Envelope.DataComponents.SEAL);
                if (seal != null) {
                    return seal.material().value().modelTintColor();
                }
            }

            return 0xFFCC4E47; // Default red color
        }
    }
}
