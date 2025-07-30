package io.github.mortuusars.envelope.world.block;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.api.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.api.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.util.result.Result;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.mail.Mailboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MailboxBlockEntity extends BaseContainerBlockEntity {
    public static final int SENDING_SLOTS = 9;

    protected String address = "";
    protected NonNullList<ItemStack> items = NonNullList.withSize(SENDING_SLOTS, ItemStack.EMPTY);

    public MailboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public MailboxBlockEntity(BlockPos pos, BlockState blockState) {
        this(Envelope.BlockEntityTypes.MAILBOX.get(), pos, blockState);
    }

    // --

    public String getAddress() {
        return address;
    }

    public MailboxBlockEntity setAddress(String address) {
        this.address = address;
        if (level instanceof ServerLevel) {
            Mail.getMailboxes().create(this.address);
        }
        return this;
    }

    // --

    @Override
    public int getContainerSize() {
        return SENDING_SLOTS;
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal(getAddress());
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.envelope.mailbox");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MailboxMenu(id, inventory, getBlockPos(), getAllMail());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Preconditions.checkElementIndex(slot, SENDING_SLOTS);
        return canPlaceItemIntoSendingSlot(stack);
    }

    public boolean canPlaceItemIntoSendingSlot(ItemStack stack) {
        return stack.has(Envelope.DataComponents.MAIL_RECIPIENT);
    }

    // --

    public List<ItemStack> getAllMail() {
        return Mail.getMailboxes().getAllMail(address);
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
                    MailTravelingLog.addRecords(extractedMail, TravelingRecord.receivedAt(new Address.Mailbox(address),
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

    // --

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        address = tag.getString("Address");
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Address", address);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            Containers.dropContents(serverLevel, getBlockPos(), items);

            Vec3 p = Vec3.atCenterOf(getBlockPos());
            for (ItemStack itemStack : getAllMail()) {
                Containers.dropItemStack(serverLevel, p.x, p.y, p.z, itemStack);
            }

            Mailboxes.get(serverLevel.getServer()).remove(address);
        }
    }
}
