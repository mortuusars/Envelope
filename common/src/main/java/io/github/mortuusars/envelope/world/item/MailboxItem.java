package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.world.inventory.MailboxAddressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class MailboxItem extends BlockItem {
    public MailboxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext blockPlaceContext = new BlockPlaceContext(context);

        BlockPos pos = blockPlaceContext.getClickedPos();

        if (blockPlaceContext.canPlace()) {
            if (blockPlaceContext.getPlayer() instanceof ServerPlayer serverPlayer) {
                PlatformHelper.openMenu(serverPlayer, new MenuProvider() {
                    @Override
                    public @NotNull Component getDisplayName() {
                        return Component.translatable("gui.envelope.mailbox.address");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                        return new MailboxAddressMenu(i, inventory, blockPlaceContext.getItemInHand(), pos, blockPlaceContext);
                    }
                }, buffer -> {
                    ItemStack.STREAM_CODEC.encode(buffer, blockPlaceContext.getItemInHand());
                    buffer.writeBlockPos(pos);
                });
            } else {
                MailboxAddressMenu.STORED_CLIENT_BLOCK_PLACE_CONTEXT = blockPlaceContext;
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
