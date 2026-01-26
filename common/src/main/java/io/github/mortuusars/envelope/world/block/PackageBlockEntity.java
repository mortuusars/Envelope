package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.PackageItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PackageBlockEntity extends BlockEntity {
    protected ItemStack item = ItemStack.EMPTY;
    protected boolean unpackWhenBroken = true;

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
        setChanged();
    }

    public boolean unpackWhenBroken() {
        return unpackWhenBroken;
    }

    public void setUnpackWhenBroken(boolean unpackWhenBroken) {
        this.unpackWhenBroken = unpackWhenBroken;
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        ItemStack packageStack = getPackage();
        if (unpackWhenBroken() && packageStack.getItem() instanceof PackageItem packageItem) {
            packageItem.unpack(packageStack, level, pos, null).forEach(stored ->
                  Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stored));
        } else {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), packageStack);
        }
        setChanged();
    }

    // -- Sync

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
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
