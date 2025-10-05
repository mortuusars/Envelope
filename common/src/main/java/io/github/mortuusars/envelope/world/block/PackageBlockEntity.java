package io.github.mortuusars.envelope.world.block;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.PackageItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PackageBlockEntity extends BlockEntity {
    protected ItemStack item = new ItemStack(Envelope.Items.PACKAGE.get());
    protected boolean shouldDestroy = true;

    protected PackageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PackageBlockEntity(BlockPos pos, BlockState blockState) {
        super(Envelope.BlockEntityTypes.PACKAGE.get(), pos, blockState);
    }

    // --

    public ItemAndStack<PackageItem> getPackage() {
        return new ItemAndStack<>(item);
    }

    public void setPackage(ItemStack item) {
        Preconditions.checkState(item.getItem() instanceof PackageItem);
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
        CompoundTag itemTag = new CompoundTag();
        item.save(registries, itemTag);
        tag.put("Package", itemTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        item = ItemStack.parse(registries, tag.getCompound("Package"))
                .orElseGet(() -> new ItemStack(Envelope.Items.PACKAGE.get()));
    }
}
