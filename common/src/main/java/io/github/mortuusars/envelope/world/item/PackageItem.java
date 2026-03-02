package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.Platform;
import io.github.mortuusars.envelope.world.inventory.PackageMenu;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

public class PackageItem extends BlockItem implements Sealable {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, components, tooltipFlag);

        @Nullable SeededContainerLoot loot = stack.get(DataComponents.CONTAINER_LOOT);
        if (loot != null) {
            components.add(Component.translatable("container.envelope.package.contents_unknown").withStyle(ChatFormatting.GRAY));

            if (tooltipFlag.isAdvanced()) {
                components.add(Component.literal("Loot Table: ").withStyle(ChatFormatting.DARK_GRAY)
                      .append(Component.literal(loot.lootTable().location().toString()).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        if (tooltipFlag.isAdvanced()) {
            int xp = stack.getOrDefault(Envelope.DataComponents.PACKAGE_EXPERIENCE, 0);
            if (xp > 0) {
                components.add(Component.literal("Experience: " + xp).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    public double getBoxReturnChance(ItemStack stack) {
        return Config.Server.PACKAGE_PAPER_BOX_RETURN_CHANCE.get();
    }

    public ItemStack createBoxReturnItem(ItemStack stack) {
        return new ItemStack(Envelope.Items.PAPER_BOX.get());
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

        if (Config.Server.PACKAGE_SNEAK_QUICK_UNPACK.get() && player.isSecondaryUseActive()) {
            destroy(stack, level, player.position(), player);
            stack.shrink(1);
        } else {
            unpackLootTableIfPresent(stack, level, player.position(), player);
            dropExperience(stack, level, player.position());

            if (player instanceof ServerPlayer serverPlayer) {
                // Force update the stack on client before opening, so the client has correct initial state.
                player.inventoryMenu.broadcastChanges();

                SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                      (id, inventory, pl) -> new PackageMenu(id, inventory, hand), stack.getHoverName());
                Platform.openMenu(serverPlayer, menuProvider, buffer -> buffer.writeEnum(hand));
            }
        }

        player.level().playSound(player, player, Envelope.SoundEvents.PAPER_TEAR.get(), SoundSource.PLAYERS, 0.6f, 0.95f);
        return InteractionResultHolder.success(stack);
    }

    protected void unpackLootTableIfPresent(ItemStack stack, Level level, Vec3 pos, @Nullable Player player) {
        if (level instanceof ServerLevel serverLevel
              && stack.get(DataComponents.CONTAINER_LOOT) instanceof SeededContainerLoot(ResourceKey<LootTable> table, long seed)) {
            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(table);

            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                  .withParameter(LootContextParams.ORIGIN, pos);

            if (player != null) {
                builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
            }

            SimpleContainer container = new SimpleContainer(PackageContents.SLOTS);
            lootTable.fill(container, builder.create(LootContextParamSets.CHEST), seed);

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

    public void destroy(ItemStack stack, Level level, Vec3 pos, @Nullable Player player) {
        unpack(stack, level, pos, player).forEach(item ->
              Containers.dropItemStack(level, pos.x(), pos.y(), pos.z(), item));
        dropExperience(stack, level, pos);
        onDestroyed(stack, level, pos);
    }

    public void onDestroyed(ItemStack stack, Level level, Vec3 pos) {
        if (!level.isClientSide() && level.getRandom().nextDouble() < getBoxReturnChance(stack)) {
            Containers.dropItemStack(level, pos.x(), pos.y(), pos.z(), createBoxReturnItem(stack));
        }
    }

    public List<ItemStack> unpack(ItemStack stack, Level level, Vec3 pos, @Nullable Player player) {
        unpackLootTableIfPresent(stack, level, pos, player);
        PackageContents contents = stack.getOrDefault(Envelope.DataComponents.PACKAGE_CONTENTS, PackageContents.EMPTY);
        stack.remove(Envelope.DataComponents.PACKAGE_CONTENTS);
        return contents.copyItems();
    }

    public void dropExperience(ItemStack stack, Level level, Vec3 pos) {
        if (level instanceof ServerLevel serverLevel) {
            int points = stack.getOrDefault(Envelope.DataComponents.PACKAGE_EXPERIENCE, 0);
            stack.remove(Envelope.DataComponents.PACKAGE_EXPERIENCE);
            if (points > 0) {
                ExperienceOrb.award(serverLevel, pos, points);
            }
        }
    }

    // --

    @Override
    public ItemStack seal(Level level, ItemStack stack, Seal seal) {
        ItemStack sealedPackage = stack.transmuteCopy(Envelope.Items.SEALED_PACKAGE.get());
        sealedPackage.set(Envelope.DataComponents.SEAL, seal);
        return sealedPackage;
    }
}