package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.block.LetterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class LetterBlockViewScreen extends LetterViewScreen {
    private final BlockPos pos;

    public LetterBlockViewScreen(ItemStack letter, BlockPos pos) {
        super(letter, null);
        this.pos = pos;
    }

    @Override
    public void tick() {
        if (!(Minecrft.level().getBlockState(pos).getBlock() instanceof LetterBlock)) {
            onClose();
        }
    }
}
