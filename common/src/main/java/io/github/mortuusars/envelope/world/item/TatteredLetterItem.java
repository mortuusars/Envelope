package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class TatteredLetterItem extends LetterItem {
    public TatteredLetterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public ItemLike getSealedItem() {
        return Envelope.Items.SEALED_TATTERED_LETTER.get();
    }
}
