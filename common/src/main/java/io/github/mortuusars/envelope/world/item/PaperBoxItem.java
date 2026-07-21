package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.Platform;
import io.github.mortuusars.envelope.world.block.PaperBoxBlock;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PaperBoxItem extends BlockItem {
    public PaperBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (context.getPlayer() != null && !context.getPlayer().isSecondaryUseActive()
              && (!(state.getBlock() instanceof PaperBoxBlock) || state.getValue(PaperBoxBlock.BOXES) >= PaperBoxBlock.MAX_BOXES)) {
            openPackingMenu(context.getPlayer(), context.getHand(), context.getItemInHand());
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        openPackingMenu(player, usedHand, stack);
        return InteractionResultHolder.success(stack);
    }

    public boolean openPackingMenu(Player player, InteractionHand hand, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            SimpleMenuProvider menuProvider = new SimpleMenuProvider((id, inventory, pl) ->
                  new PackingMenu(id, inventory, hand), stack.getHoverName());
            Platform.openMenu(serverPlayer, menuProvider, buffer ->
                  buffer.writeEnum(hand));
        }

        player.level().playSound(player, player, Envelope.SoundEvents.PAPER_USE.get(), SoundSource.PLAYERS, 0.6f, 0.95f);
        return true;
    }
}