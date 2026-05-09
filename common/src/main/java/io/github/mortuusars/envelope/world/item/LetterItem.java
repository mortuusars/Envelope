package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.clientbound.OpenLetterViewScreenS2CP;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class LetterItem extends BlockItem implements Sealable {
    public LetterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        stack.set(Envelope.DataComponents.LETTER_CONTENT,
              stack.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY).withUnfolded(true));
        player.getCooldowns().addCooldown(this, 5);

        if (player instanceof ServerPlayer serverPlayer) {
            Packets.sendToClient(new OpenLetterViewScreenS2CP(usedHand), serverPlayer);
        }

        level.playSound(player, player, Envelope.SoundEvents.PAPER_CRACKLE.get(), SoundSource.PLAYERS, 1, 1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ItemStack seal(Level level, ItemStack stack, Seal seal) {
        ItemStack sealedLetter = stack.transmuteCopy(Envelope.Items.SEALED_LETTER.get());
        sealedLetter.set(Envelope.DataComponents.SEAL, seal);
        return sealedLetter;
    }
}