package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PackageBlockEntity extends BlockEntity {
    protected ItemStack item = ItemStack.EMPTY;
    protected boolean shouldDestroy = true;

    protected PackageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PackageBlockEntity(BlockPos pos, BlockState blockState) {
        super(Envelope.BlockEntityTypes.PACKAGE.get(), pos, blockState);
    }

    // --

    public ItemStack getPackage() {
        return !item.isEmpty() ? item : new ItemStack(getBlockState().getBlock().asItem());
    }

    public void setPackage(ItemStack item) {
        this.item = item;
    }

    public boolean shouldDestroy() {
        return shouldDestroy;
    }

    public void setShouldDestroy(boolean shouldDestroy) {
        this.shouldDestroy = shouldDestroy;
    }

    // --

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (!item.isEmpty()) {
            tag.put("Package", item.save(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Package", CompoundTag.TAG_COMPOUND)) {
            item = ItemStack.parse(registries, tag.getCompound("Package")).orElse(ItemStack.EMPTY);
        }
    }
}
