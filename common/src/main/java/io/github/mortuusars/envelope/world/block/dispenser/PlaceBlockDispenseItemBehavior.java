package io.github.mortuusars.envelope.world.block.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.NotNull;

public class PlaceBlockDispenseItemBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected @NotNull ItemStack execute(BlockSource blockSource, ItemStack stack) {
        this.setSuccess(false);
        if (stack.getItem() instanceof BlockItem blockItem) {
            Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
            BlockPos placePos = blockSource.pos().relative(direction);

            try {
                setSuccess(blockItem.place(new DirectionalPlaceContext(blockSource.level(), placePos, direction, stack, direction))
                      .consumesAction());
            } catch (Exception e) {
                LOGGER.error("Error trying to place block at {}", placePos, e);
            }
        }

        return stack;
    }
}
