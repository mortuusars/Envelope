package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltipComponent;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SealStampItem extends Item implements ApplicatorItem {
    public SealStampItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (tooltipFlag.isAdvanced()) {
            tooltipComponents.add(Component.literal("Impression:").withStyle(ChatFormatting.DARK_GRAY)
                  .append(CommonComponents.SPACE)
                  .append(Component.literal(getImpression(stack, Minecrft.player()).id().toString()).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new SealDieTooltipComponent(Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL_IMPRESSION))));
    }

    @Override
    public boolean shouldRenderTooltipWhileCarrying(Level level, ItemStack carried, ItemStack hovered) {
        return hovered.has(Envelope.DataComponents.SEAL)
              || (hovered.getItem() instanceof Sealable sealable && sealable.canSeal(level, hovered));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack target = slot.getItem();

        @Nullable Seal existingSeal = target.get(Envelope.DataComponents.SEAL);
        if (existingSeal != null && canApplyGold(stack, player)) {
            ResourceKey<SealMaterial> currentMaterial = existingSeal.material().unwrapKey().orElse(SealMaterial.RED_WAX);
            ResourceKey<SealMaterial> newMaterial = currentMaterial == SealMaterial.RED_WAX ? SealMaterial.GOLD : SealMaterial.RED_WAX;

            Holder<SealMaterial> material = SealMaterial.getHolder(player.registryAccess(), newMaterial);

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
        //TODO: sealed stat

        return true;
    }

    public Seal createSeal(ItemStack stack, Player player) {
        return new Seal(
              SealMaterial.getHolder(player.registryAccess(), SealMaterial.RED_WAX),
              getImpression(stack, player),
              player.getName());

//        List<SealImpression> impressions = SealImpressions.REGISTRY.values().stream().toList();
//        SealImpression impression = Util.getRandom(impressions, player.getRandom());
//        return new Seal(SealMaterials.RED_WAX, impression, player.getName());
    }

    public SealImpression getImpression(ItemStack stack, @Nullable Player player) {
        return stack.getOrDefault(Envelope.DataComponents.SEAL_IMPRESSION,
              SealImpression.firstCharOrDefault(player != null ? player.getScoreboardName() : ""));
    }

    protected boolean canApplyGold(ItemStack stack, Player player) {
        //TODO: patreon supporters
        return false;
    }
}