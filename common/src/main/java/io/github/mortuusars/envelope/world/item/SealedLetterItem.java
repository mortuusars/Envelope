package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SealedLetterItem extends Item implements SealedItem {
    protected final Supplier<? extends Item> baseItem;

    public SealedLetterItem(Supplier<? extends Item> baseItem, Properties properties) {
        super(properties);
        this.baseItem = baseItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        baseItem.get().appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public ItemLike getUnsealedItem() {
        return baseItem.get();
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
        return getUnsealingSound();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getUnsealingDuration(stack, entity);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return useSealedItem(level, player, hand);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return unsealByUsing(stack, level, entity);
    }
}
