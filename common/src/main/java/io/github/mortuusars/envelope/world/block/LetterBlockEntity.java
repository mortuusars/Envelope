package io.github.mortuusars.envelope.world.block;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class LetterBlockEntity extends BlockEntity implements Nameable {
    private static final Logger LOGGER = LogUtils.getLogger();

    protected @Nullable SeededContainerLoot loot = null;
    protected @NotNull ItemStack letter = ItemStack.EMPTY;

    public LetterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public LetterBlockEntity(BlockPos pos, BlockState state) {
        super(Envelope.BlockEntityTypes.LETTER.get(), pos, state);
    }

    public ItemStack getLetter(@Nullable Player player) {
        unpackLootTableIfPresent(player);
        return !letter.isEmpty() ? letter : new ItemStack(Envelope.Items.LETTER.get());
    }

    public void setLetter(ItemStack letter) {
        this.letter = letter;
        this.loot = null;
        setChanged();
    }

    protected void unpackLootTableIfPresent(@Nullable Player player) {
        if (loot != null && level instanceof ServerLevel serverLevel) {
            if (!letter.isEmpty()) {
                Envelope.LOGGER.warn("Unpacking loot-table of the Letter block, that already has letter defined. " +
                      "Existing letter will be overriden.");
            }

            LootParams.Builder params = new LootParams.Builder(serverLevel)
                  .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()));

            if (player != null) {
                params.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, loot.lootTable());
            }

            serverLevel.getServer().reloadableRegistries().getLootTable(loot.lootTable())
                  .getRandomItems(params.create(LootContextParamSets.CHEST), loot.seed())
                  .stream()
                  .findAny()
                  .ifPresent(this::setLetter);
        }
    }

    // --

    @Override
    public @NotNull Component getName() {
        return letter.getHoverName();
    }

    @Override
    public @Nullable Component getCustomName() {
        return letter.get(DataComponents.CUSTOM_NAME);
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

    // -- Save/Load

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (loot != null) {
            SeededContainerLoot.CODEC.encodeStart(NbtOps.INSTANCE, loot)
                  .resultOrPartial(LOGGER::error)
                  .ifPresent(lootTag -> tag.put("Loot", lootTag));
        }
        if (!letter.isEmpty()) {
            tag.put("Letter", letter.save(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Loot", CompoundTag.TAG_COMPOUND)) {
            loot = SeededContainerLoot.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("Loot"))
                  .resultOrPartial(LOGGER::error)
                  .orElse(null);
        }
        if (tag.contains("Letter", CompoundTag.TAG_COMPOUND)) {
            letter = ItemStack.parse(registries, tag.getCompound("Letter"))
                  .orElse(ItemStack.EMPTY);
        }
    }
}
