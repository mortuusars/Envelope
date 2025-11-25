package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SealedLetterItem extends Item {
    public SealedLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL));
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public @NotNull SoundEvent getEatingSound() {
        return Envelope.SoundEvents.PAPER_TEAR.get();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && player.getInventory().contains(Envelope.Tags.Items.CUTTERS)) {
            return 5;
        }
        return 25;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        stack = stack.transmuteCopy(Envelope.Items.LETTER.get());
        stack.remove(Envelope.DataComponents.SEAL);

        if (entity instanceof Player player) {
            level.playSound(player, entity, Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS,
                  1f, level.getRandom().nextFloat() * 0.1f + 0.8f);
            //TODO: SEALED_LETTERS_OPENED Stat
            //player.awardStat(Envelope.Stats.SEALED_LETTERS_OPENED);
        } else {
            level.playSound(null, entity, Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS,
                  1f, level.getRandom().nextFloat() * 0.1f + 0.8f);
        }

        if (level.isClientSide()) {
            // Release use key after opening.
            // Otherwise, right click will be still held and will activate use again.
            Minecrft.releaseUseButton();
        }

        return stack;
    }
}
