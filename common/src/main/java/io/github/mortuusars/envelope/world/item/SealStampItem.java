package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.*;
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
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        ItemStack target = slot.getItem();

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
            player.playSound(SoundEvents.COMPARATOR_CLICK);
            return true;
        }

        if (!sealable.canSeal(player.level(), target)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK);
            return true;
        }

        ItemStack sealResult = sealable.seal(player.level(), target, createSeal(stack, player));
        slot.set(sealResult);
        player.playSound(SoundEvents.UI_LOOM_SELECT_PATTERN);

        return true;
    }

    public Seal createSeal(ItemStack stack, Player player) {
        return new Seal(SealMaterials.RED_WAX, SealImpressions.firstLetterOf(player.getScoreboardName()), player.getName());

//        List<SealImpression> impressions = SealImpressions.REGISTRY.values().stream().toList();
//        SealImpression impression = Util.getRandom(impressions, player.getRandom());
//        return new Seal(SealMaterials.RED_WAX, impression, player.getName());
    }

    protected boolean canApplyGold(ItemStack stack, Player player) {
        //TODO: patreon supporters
        return false;
    }
}