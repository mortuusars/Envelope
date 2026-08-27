package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltipComponent;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import io.github.mortuusars.mortaar.world.item.ApplicatorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SealStampItem extends Item implements ApplicatorItem {
    public SealStampItem(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("removal")
    public Optional<Holder<SealSymbol>> getDie(ItemStack stack) {
        if (stack.has(Envelope.DataComponents.SEAL_STAMP_IMPRESSION)) {
            stack.set(Envelope.DataComponents.SEAL_STAMP_DIE, stack.remove(Envelope.DataComponents.SEAL_STAMP_IMPRESSION));
        }
        return Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL_STAMP_DIE));
    }

    public Holder<SealSymbol> getDieOrDefault(ItemStack stack, RegistryAccess registryAccess, @Nullable Player player) {
        return getDie(stack).orElseGet(() ->
              SealSymbol.getOrThrow(registryAccess, SealSymbol.firstCharOrDefault(player)));
    }

    // --

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        if (flag.isAdvanced()) {
            getDie(stack)
                  .flatMap(Holder::unwrapKey)
                  .ifPresent(key -> {
                      components.add(Component.literal("Die: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(key.location().toString()).withStyle(ChatFormatting.GRAY)));
                  });
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new SealDieTooltipComponent(getDie(stack)));
    }

    @Override
    public boolean shouldRenderSlotTooltipWhileCarrying(Player player, AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        if (!slot.allowModification(player)) {
            return false;
        }

        return slot.getItem().has(Envelope.DataComponents.SEAL)
                || (slot.getItem().getItem() instanceof Sealable sealable && sealable.canSeal(player.level(), slot.getItem()));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        if (!slot.allowModification(player)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK);
            return true;
        }

        ItemStack target = slot.getItem();

        @Nullable Seal existingSeal = target.get(Envelope.DataComponents.SEAL);
        if (existingSeal != null && canApplyGold(stack, player)) {
            ResourceKey<SealMaterial> currentMaterial = existingSeal.material().unwrapKey().orElse(SealMaterial.RED_WAX);
            ResourceKey<SealMaterial> newMaterial = currentMaterial == SealMaterial.RED_WAX ? SealMaterial.GOLD : SealMaterial.RED_WAX;

            Holder<SealMaterial> material = SealMaterial.getOrThrow(player.registryAccess(), newMaterial);

            target.set(Envelope.DataComponents.SEAL, new Seal(material, existingSeal.impression(), existingSeal.signature()));
            slot.set(target);
            player.playSound(SoundEvents.UI_LOOM_SELECT_PATTERN);
            return true;
        }

        if (!(target.getItem() instanceof Sealable sealable) || !sealable.canSeal(player.level(), target)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK);
            return true;
        }

        ItemStack sealResult = sealable.seal(player.level(), target, createSeal(stack, player));
        slot.set(sealResult);
        player.playSound(SoundEvents.UI_LOOM_SELECT_PATTERN);

        player.awardStat(Envelope.Stats.SEALS_APPLIED.get());

        return true;
    }

    public Seal createSeal(ItemStack stack, Player player) {
        return new Seal(
              SealMaterial.getOrThrow(player.registryAccess(), SealMaterial.RED_WAX),
              getDieOrDefault(stack, player.registryAccess(), player),
              player.getName());
    }

    protected boolean canApplyGold(ItemStack stack, Player player) {
        //TODO: patreon supporters
        return false;
    }
}