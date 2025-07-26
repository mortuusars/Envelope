package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import io.github.mortuusars.envelope.api.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.api.mail.log.TravelingRecord;
import io.github.mortuusars.envelope.util.result.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MailboxBlockEntity extends BlockEntity {
    protected String address = "";
    protected List<ItemStack> mailQueue = new ArrayList<>();

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

    public List<ItemStack> getAllMail() {
        return Mail.getMailboxes().getAllMail(address);
    }

    public boolean sendMail(ItemStack mail, @Nullable Player player) {
        if (mail.isEmpty()) {
            Envelope.LOGGER.error("Cannot send empty mail.");
            return false;
        }

        if (!mail.has(Envelope.DataComponents.MAIL_RECIPIENT)) {
            Envelope.LOGGER.error("Cannot send mail: no 'envelope:recipient' defined. {}", mail);
            return false;
        }

        Address sender = new Address.Mailbox(address);
        mail.set(Envelope.DataComponents.MAIL_SENDER, sender);

        if (level instanceof ServerLevel) {
            Mail.send(mail, player);
        }

        return true;
    }

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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Address", address);

        ListTag mailQueueTag = new ListTag();
        mailQueue.stream().map(s -> s.save(registries)).forEach(mailQueueTag::add);
        tag.put("MailQueue", mailQueueTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        address = tag.getString("Address");

        mailQueue.clear();
        ListTag mailQueueTag = tag.getList("MailQueue", Tag.TAG_COMPOUND);
        for (Tag mailTag : mailQueueTag) {
            ItemStack.parse(registries, mailTag)
                    .ifPresentOrElse(mailQueue::add,
                            () -> Envelope.LOGGER.error("Cannot load queued mail from tag '{}'", mailTag));
        }
    }

    // --

    public @NotNull Level getLevelOrThrow() {
        return Objects.requireNonNull(level);
    }
}
