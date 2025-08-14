package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.api.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.api.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.inventory.PigeonholeAddressMenu;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.mail.Mailboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PigeonholeBlockEntity extends BlockEntity {
    @Nullable
    protected Address.Mailbox address = null;

    public PigeonholeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PigeonholeBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.PIGEONHOLE.get(), pos, blockState);
    }

    // -- Address

    public Optional<Address.Mailbox> getAddress() {
        return Optional.ofNullable(address);
    }

    public PigeonholeBlockEntity setAddress(@NotNull Address.Mailbox address) {
        this.address = address;
        if (level instanceof ServerLevel) {
            Mail.getMailboxes().create(this.address);
        }
        setChanged();
        return this;
    }

    // -- Events

    @Override
    public void setRemoved() {
        super.setRemoved();
        dropOrReturnAllMail();
    }

    // -- Mail

    public List<ItemStack> getAllMail() {
        return Mail.getMailboxes().getAllMail(getAddress().orElseThrow());
    }

//    public boolean sendMail(ItemStack mail, @Nullable Player player) {
//        if (mail.isEmpty()) {
//            Envelope.LOGGER.error("Cannot send empty mail.");
//            return false;
//        }
//
//        if (!mail.has(Envelope.DataComponents.MAIL_RECIPIENT)) {
//            Envelope.LOGGER.error("Cannot send mail: no 'envelope:recipient' defined. {}", mail);
//            return false;
//        }
//
//        Address sender = new Address.Mailbox(address);
//        mail.set(Envelope.DataComponents.MAIL_SENDER, sender);
//
//        if (level instanceof ServerLevel) {
//            Mail.send(mail, player);
//        }
//
//        return true;
//    }

    public ItemStack takeMail(ItemStack mail, @Nullable Player player) {
        if (!mail.has(Envelope.DataComponents.MAIL_ID)) {
            return ItemStack.EMPTY;
        }

        Result<ItemStack> extractResult = Mail.getMailboxes().removeMail(address, mail.get(Envelope.DataComponents.MAIL_ID));
        return extractResult
                .mapValue(extractedMail -> {
                    MailTravelingLog.addRecords(extractedMail, TravelingRecord.receivedAt(getAddress().orElseThrow(),
                            getLevelOrThrow().getGameTime(), Optional.ofNullable(player).map(Player::getName)));
                    extractedMail.remove(Envelope.DataComponents.MAIL_ID);
                    extractedMail.remove(Envelope.DataComponents.MAIL_RECIPIENT);
                    extractedMail.remove(Envelope.DataComponents.MAIL_SENDER);
                    extractedMail.remove(Envelope.DataComponents.MAIL_SENT_AT);
                    extractedMail.remove(Envelope.DataComponents.MAIL_TRAVEL_DURATION);
                    return extractedMail;
                })
                .handleFailure(f -> Envelope.LOGGER.error(f.getMessage()), ItemStack.EMPTY);
    }

    protected void dropOrReturnAllMail() {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 p = Vec3.atCenterOf(getBlockPos());
            for (ItemStack itemStack : getAllMail()) {
                Containers.dropItemStack(serverLevel, p.x, p.y, p.z, itemStack);
            }

            Mailboxes.get(serverLevel.getServer()).remove(address);
        }
    }

    // -- Menu

    public MenuProvider createMenuProvider() {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("gui.envelope.pigeonhole");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new PigeonholeMenu(id, inventory, getBlockPos(), getAllMail());
            }
        };
    }

    public MenuProvider createAddressMenuProvider(String suggestedAddress) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("gui.envelope.pigeonhole_address.enter_address");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new PigeonholeAddressMenu(id, inventory, getBlockPos(), suggestedAddress);
            }
        };
    }

    // -- Save/Load

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Address", Tag.TAG_STRING)) {
            address = new Address.Mailbox(tag.getString("Address"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (address != null) {
            tag.putString("Address", address.id());
        }
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }
}
