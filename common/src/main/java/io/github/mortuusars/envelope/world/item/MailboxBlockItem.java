package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenMailboxPlacingScreenS2CP;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class MailboxBlockItem extends BlockItem {
    public MailboxBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getPlayer().getItemInHand(context.getHand());

        if (!MailService.operatesIn(context.getLevel())
              || stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).contains("address")) {
            return super.useOn(context);
        }

        BlockPlaceContext blockPlaceContext = new BlockPlaceContext(context);

        if (blockPlaceContext.canPlace()) {
            if (blockPlaceContext.getPlayer() instanceof ServerPlayer serverPlayer) {
                BlockHitResult hitResult = new BlockHitResult(
                      context.getClickLocation(),
                      context.getClickedFace(),
                      context.getClickedPos(),
                      context.isInside());
                OpenMailboxPlacingScreenS2CP packet = new OpenMailboxPlacingScreenS2CP(
                      context.getHand(),
                      hitResult,
                      MailService.of(serverPlayer.serverLevel()).getKnownAddresses()
                );
                Packets.sendToClient(packet, serverPlayer);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
