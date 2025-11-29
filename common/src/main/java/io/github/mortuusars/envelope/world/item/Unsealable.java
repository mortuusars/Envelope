package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface Unsealable {
    ItemLike getUnsealedItem();

    default SoundEvent getUnsealingSound() {
        return Envelope.SoundEvents.PAPER_CRACKLE.get();
    }

    default SoundEvent getUnsealFinishedSound() {
        return Envelope.SoundEvents.PAPER_TEAR.get();
    }

    default int getUnsealingDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    default ItemStack unseal(ItemStack stack, Level level, @Nullable LivingEntity entity) {
        ItemStack unsealedStack = stack.transmuteCopy(getUnsealedItem());
        unsealedStack.remove(Envelope.DataComponents.SEAL);

        onUnsealed(stack, unsealedStack, level, entity);

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

        //TODO: SEALS_BROKEN Stat
        //player.awardStat(Envelope.Stats.SEALS_BROKEN);
    }
}
