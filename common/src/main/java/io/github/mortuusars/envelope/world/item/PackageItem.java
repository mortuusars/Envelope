package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PackageItem extends BlockItem implements PackingBox, Sealable {
    public PackageItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.PACKAGE_CONTENTS));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        @Nullable SeededContainerLoot loot = stack.get(DataComponents.CONTAINER_LOOT);
        if (tooltipFlag.isAdvanced() && loot != null) {
            tooltipComponents.add(Component.literal("Loot Table: ").withStyle(ChatFormatting.DARK_GRAY)
                  .append(Component.literal(loot.lootTable().location().toString()).withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    // --

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Config.Server.PACKAGE_LAST_OPEN_ANIMATION.get() && shouldBeDestroyedWhenEmpty(stack) ? 15 : 0;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public @NotNull SoundEvent getEatingSound() {
        return Envelope.SoundEvents.PAPER_TEAR.get();
    }

    // --

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (Config.Server.PACKAGE_LAST_OPEN_ANIMATION.get() && shouldBeDestroyedWhenEmpty(stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.success(stack);
        } else {
            return InteractionResultHolder.success(open(level, player, hand, stack));
        }
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            return open(level, player, player.getUsedItemHand(), stack);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    // --

    protected ItemStack open(Level level, Player player, InteractionHand hand, ItemStack stack) {
        stack = stack.transmuteCopy(Envelope.Items.PACKING_BOX.get());
        Mail.removePreviousDeliveryData(stack);

        unpackLootTableIfPresent(stack, level, player.blockPosition(), player);

        level.playSound(player, player, Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS, 1, 1);

        if (stack.getItem() instanceof PackingBoxItem packingBox) {
            packingBox.openPackingGui(player, hand, stack);
        } else {
            Envelope.LOGGER.error("Cannot open packing box gui. Stack {} is not a PackingBoxItem.", stack.getHoverName().getString());
        }
        return stack;
    }

    protected void unpackLootTableIfPresent(ItemStack stack, Level level, BlockPos pos, @Nullable Player player) {
        @Nullable SeededContainerLoot loot = stack.get(DataComponents.CONTAINER_LOOT);
        if (level instanceof ServerLevel serverLevel && loot != null) {
            ResourceKey<LootTable> table = loot.lootTable();
            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(table);

            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                  .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos));

            if (player != null) {
                builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            }

            SimpleContainer container = new SimpleContainer(PackageContents.SLOTS);
            lootTable.fill(container, builder.create(LootContextParamSets.CHEST), loot.seed());

            if (stack.has(Envelope.DataComponents.PACKAGE_CONTENTS)) {
                Envelope.LOGGER.warn("Unpacking container loot into the Package, that already has contents inside. " +
                      "Loot will override the contents.");
            }

            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, table);
            }

            stack.remove(DataComponents.CONTAINER_LOOT);
            stack.set(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.createFrom(container));
        }
    }

    public List<ItemStack> unpack(ItemStack stack, Level level, BlockPos pos, @Nullable Player player) {
        unpackLootTableIfPresent(stack, level, pos, player);
        PackageContents contents = stack.getOrDefault(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.EMPTY);
        stack.remove(Envelope.DataComponents.PACKAGE_CONTENTS);
        return contents.copyItems();
    }

    // --

    @Override
    public ItemStack seal(Level level, ItemStack stack, Seal seal) {
        ItemStack sealedLetter = stack.transmuteCopy(Envelope.Items.SEALED_PACKAGE.get());
        sealedLetter.set(Envelope.DataComponents.SEAL, seal);
        return sealedLetter;
    }
}