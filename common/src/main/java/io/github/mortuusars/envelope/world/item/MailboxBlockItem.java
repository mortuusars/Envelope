package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.network.packet.clientbound.ClientboundOpenMailboxPlacingScreenPacket;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
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
        if (!MailService.operatesIn(context.getLevel())
              || context.getItemInHand().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).contains("address")) {
            return super.useOn(context);
        }

        BlockPlaceContext blockPlaceContext = new BlockPlaceContext(context);

        if (blockPlaceContext.canPlace()) {
            if (blockPlaceContext.getPlayer() instanceof ServerPlayer serverPlayer) {
                var hitResult = new BlockHitResult(context.getClickLocation(),
                      context.getClickedFace(), context.getClickedPos(), context.isInside());
                new ClientboundOpenMailboxPlacingScreenPacket(context.getHand(), hitResult,
                      MailService.of(serverPlayer.serverLevel()).getKnownAddresses()).sendToClient(serverPlayer);
            }

            context.getLevel().playSound(context.getPlayer(), context.getClickedPos(),
                  getInitialSound(), SoundSource.BLOCKS, 1, 1);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    protected @NotNull SoundEvent getInitialSound() {
        return SoundEvents.WOOD_PLACE;
    }
}
