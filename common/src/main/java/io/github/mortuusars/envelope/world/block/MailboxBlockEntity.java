package io.github.mortuusars.envelope.world.block;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.Mail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MailboxBlockEntity extends BlockEntity {
    protected String address = "";

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

    public boolean sendMail(ItemStack mail, @Nullable Player player) {
        if (mail.isEmpty()) {
            Envelope.LOGGER.error("Cannot send empty mail.");
            return false;
        }

        if (!mail.has(Envelope.DataComponents.RECIPIENT)) {
            Envelope.LOGGER.error("Cannot send mail: no 'envelope:recipient' defined. {}", mail);
            return false;
        }

        Address sender = new Address.Mailbox(address);
        mail.set(Envelope.DataComponents.SENDER, sender);

        if (level instanceof ServerLevel) {
            Mail.send(mail, player);
        }

        return true;
    }

    // --

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Address", address);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        address = tag.getString("Address");
    }
}
