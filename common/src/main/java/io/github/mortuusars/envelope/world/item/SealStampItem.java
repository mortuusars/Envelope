package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpressions;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterials;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SealStampItem extends Item {
    public SealStampItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        @Nullable Seal existingSeal = target.get(Envelope.DataComponents.SEAL);
        if (existingSeal != null && canApplyGold(stack, player)) {
            SealMaterial newMaterial = existingSeal.material().equals(SealMaterials.GOLD)
                  ? SealMaterials.RED_WAX
                  : SealMaterials.GOLD;
            target.set(Envelope.DataComponents.SEAL, new Seal(newMaterial, existingSeal.impression(), existingSeal.signature()));
            slot.set(target);
            player.playSound(SoundEvents.UI_LOOM_SELECT_PATTERN);
            return true;
        }

        if (!(target.getItem() instanceof Sealable sealable)) {
            return false;
        }

        if (!sealable.canSeal(player.level(), target)) {
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value());
            return true; // Do nothing
        }

        ItemStack sealResult = sealable.seal(player.level(), target, createSeal(stack, player));
        slot.set(sealResult);
        player.playSound(SoundEvents.UI_LOOM_SELECT_PATTERN);

        return true;
    }

    public Seal createSeal(ItemStack stack, Player player) {
        return new Seal(SealMaterials.RED_WAX, SealImpressions.firstLetterOf(player.getScoreboardName()), player.getName());
    }

    protected boolean canApplyGold(ItemStack stack, Player player) {
        //TODO: patreon supporters
        return true;
    }
}