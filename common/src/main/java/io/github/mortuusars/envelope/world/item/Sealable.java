package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.PackageBlockEntity;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface Sealable {
    ItemStack seal(Level level, ItemStack stack, Seal seal);

    default boolean canSeal(Level level, ItemStack stack) {
        return !stack.has(Envelope.DataComponents.SEAL);
    }

    // --

    static int getSealOverlayColor(ItemStack stack, int layer) {
        if (layer != 1) {
            return -1;
        }

        @Nullable Seal seal = stack.get(Envelope.DataComponents.SEAL);
        if (seal != null) {
            return seal.material().value().modelTintColor();
        }

        return 0xFFCC4E47; // Default red color
    }

    static int getSealOverlayColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int index) {
        if (index != 0) {
            return -1;
        }

        if (level != null && pos != null && level.getBlockEntity(pos) instanceof PackageBlockEntity blockEntity) {
            @Nullable Seal seal = blockEntity.getPackage().get(Envelope.DataComponents.SEAL);
            if (seal != null) {
                return seal.material().value().modelTintColor();
            }
        }

        return 0xFFCC4E47; // Default red color
    }
}
