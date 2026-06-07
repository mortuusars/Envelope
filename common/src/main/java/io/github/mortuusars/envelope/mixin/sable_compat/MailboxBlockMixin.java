package io.github.mortuusars.envelope.mixin.sable_compat;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MailboxBlock.class)
public class MailboxBlockMixin implements BlockSubLevelAssemblyListener {
    @Override
    public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
        // Prevents uniquifying of the address when the block is moved due to the old address still existing
        MailService.of(originLevel).getMailboxes().remove(oldPos);

        // Save inbox before moving to keep the mail inside
        if (originLevel.getBlockEntity(oldPos) instanceof MailboxBlockEntity blockEntity) {
            blockEntity.unloadInbox();
        }

        // Sable calls Clearable#clearContent between 'beforeMove' and 'afterMove', which prevents the inbox from dropping on the ground.
    }

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
        // Load stored inbox after moving. 'loadInbox' is called in BlockEntity#clearRemoved (when block is placed),
        // but it happens before the block entity is loaded from the tag, so it has a random inboxId at that point.
        // Here we load it after inboxId is restored from the tag.
        if (resultingLevel.getBlockEntity(newPos) instanceof MailboxBlockEntity blockEntity) {
            blockEntity.loadInbox();
        }
    }
}
