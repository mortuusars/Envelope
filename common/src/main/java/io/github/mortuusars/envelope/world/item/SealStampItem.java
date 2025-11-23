package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.Seal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SealStampItem extends Item {
    public SealStampItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        ItemStack other = slot.getItem();
        if (action != ClickAction.SECONDARY || !other.is(Envelope.Tags.Items.MAILABLE)) {
            return false;
        }

        if (other.has(Envelope.DataComponents.SEAL)) {
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value());
            return true; // Do nothing
        }

        if (other.getItem() instanceof Sealable sealable) {
            sealable.seal(player.level(), other, new Seal(player.getName()));
        }

        other.set(Envelope.DataComponents.SEAL, new Seal(player.getName()));
        player.playSound(SoundEvents.UI_STONECUTTER_SELECT_RECIPE);
        return true;
    }
}
