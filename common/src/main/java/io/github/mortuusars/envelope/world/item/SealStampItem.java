package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltip;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import io.github.mortuusars.mortaar.Platform;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.mortaar.world.item.ApplicatorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SealStampItem extends Item implements ApplicatorItem {
    public SealStampItem(Properties properties) {
        super(properties);
    }

    // -- Material

    public Optional<EitherHolder<SealMaterial>> getMaterial(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL_STAMP_MATERIAL));
    }

    public Holder<SealMaterial> getMaterialOrDefault(ItemStack stack, HolderLookup.Provider registries) {
        return getMaterial(stack)
              .flatMap(eitherHolder -> eitherHolder.unwrap(registries))
              .orElseGet(() -> SealMaterial.getOrThrow(registries, SealMaterial.WAX));
    }

    public boolean canDyeWith(ItemStack stack, DyeColor color) {
        if (!stack.has(Envelope.DataComponents.SEAL_STAMP_MATERIAL)) {
            return true;
        }

        return getMaterial(stack)
              .map(e -> !e.key().equals(SealMaterial.fromDyeColor(color)))
              .orElse(false);
    }

    protected boolean canApplyGold(ItemStack stack, Player player) {
        //TODO: patreon supporters
        return false;
    }

    // -- Die

    public Optional<EitherHolder<SealSymbol>> getDie(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL_STAMP_DIE));
    }

    public Holder<SealSymbol> getDieOrDefault(ItemStack stack, HolderLookup.Provider registries, @Nullable Player player) {
        return getDie(stack)
              .flatMap(eitherHolder -> eitherHolder.unwrap(registries))
              .orElseGet(() -> SealSymbol.getOrThrow(registries, SealSymbol.firstCharOrDefault(player)));
    }

    // -- Seal

    public Seal createSeal(ItemStack stack, Player player) {
        return new Seal(
              getMaterialOrDefault(stack, player.registryAccess()),
              getDieOrDefault(stack, player.registryAccess(), player),
              player.getName());
    }

    // --

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        HolderLookup.Provider registries = context.registries();
        if (flag.isAdvanced() && registries != null) {
            getDie(stack)
                  .flatMap(t -> t.unwrap(registries))
                  .flatMap(Holder::unwrapKey)
                  .ifPresent(key -> {
                      components.add(Component.literal("Die: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(key.location().toString()).withStyle(ChatFormatting.GRAY)));
                  });
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of((TooltipComponent) new SealDieTooltip(getDie(stack)))
              .filter(tooltip -> !Platform.isClient()
                    || !Config.Client.HIDE_DEFAULT_SEAL_STAMP_DIE_TOOLTIP_OUTSIDE_OF_INVENTORY.get()
                    || stack.has(Envelope.DataComponents.SEAL_STAMP_DIE)
                    || Client.isInInventory(stack));
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
            ResourceKey<SealMaterial> currentMaterial = existingSeal.material().unwrapKey().orElse(SealMaterial.WAX);
            ResourceKey<SealMaterial> newMaterial = currentMaterial == SealMaterial.WAX ? SealMaterial.GOLD : SealMaterial.WAX;

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

    public static class Client {
        public static boolean isInInventory(ItemStack stack) {
            if (Minecrft.player().containerMenu instanceof AbstractContainerMenu menu) {
                for (Slot slot : menu.slots) {
                    if (slot.getItem() == stack) {
                        return true;
                    }
                }
                return false;
            }

            for (ItemStack item : Minecrft.player().getInventory().items) {
                if (item == stack) {
                    return true;
                }
            }

            return false;
        }
    }
}