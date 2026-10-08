package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SoulboundSealStampItem extends SealStampItem {
    public SoulboundSealStampItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return super.isEnabled(enabledFeatures)
              && (!Config.Server.SPEC.isLoaded() || Config.Server.SOULBOUND_SEAL_STAMP_ENABLED.get());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double time = /*(Minecrft.level().getGameTime() + Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true)) / */20.0;
        double beat = Math.pow((Math.sin(time * Math.PI * 2 * 1) + 1.0) * 0.5, 10);
        float delta = (float) Mth.lerp(beat, 0, 1);
        return FastColor.ARGB32.lerp(delta, 0xFF08ABB3, 0xFF40E7FF);
    }

    // --

    @Override
    public Optional<EitherHolder<SealMaterial>> getMaterial(ItemStack stack) {
        return Optional.of(new EitherHolder<>(SealMaterial.SCULK));
    }

    @Override
    public boolean canDyeWith(ItemStack stack, DyeColor color) {
        return false;
    }

    @Override
    public boolean canApplyGold(ItemStack stack, Player player) {
        return false;
    }

    @Override
    public Seal createSeal(ItemStack stack, Player player) {
        if (player.isCreative()) {
            player.displayClientMessage(Component.translatable("item.envelope.soulbound_seal_stamp.error.in_creative_mode")
                  .withStyle(ChatFormatting.RED), true);
            return super.createSeal(stack, player);
        }

        if (Config.Server.SOULBOUND_SEAL_STAMP_CREATES_LOCK.get()) {
            return Seal.copy(super.createSeal(stack, player), player.registryAccess())
                  .lockedBy(player)
                  .build();
        }

        return super.createSeal(stack, player);
    }

    @Override
    protected void onSealApplied(ItemStack stampStack, Player player, SealableItem sealable, ItemStack target, Seal seal) {
        super.onSealApplied(stampStack, player, sealable, target, seal);

        player.level().playSound(null, player, SoundEvents.SCULK_BLOCK_SPREAD,
              SoundSource.PLAYERS, 1f, player.getRandom().nextFloat() * 0.3f + 0.85f);

        if (Config.Server.SOULBOUND_SEAL_STAMP_CREATES_LOCK.get()) {
            seal.lock().ifPresent(lock -> lock.lock(player.level()));
        }

        if (Config.Server.SOULBOUND_SEAL_STAMP_CONSUMABLE.get()) {
            stampStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            if (stampStack.isEmpty()) {
                stampStack.setCount(1); // Empty stacks do not return proper components and 'transmuteCopy' returns ItemStack.EMPTY

                ResourceKey<SealMaterial> originalMaterial = Optional.ofNullable(stampStack.get(Envelope.DataComponents.SEAL_STAMP_MATERIAL))
                      .map(EitherHolder::key)
                      .orElse(SealMaterial.WAX);

                @Nullable DyeColor color = SealMaterial.colorFromMaterial(originalMaterial);

                Item sealStamp = color != null
                      ? Envelope.Items.DYED_SEAL_STAMPS.getOrDefault(color, Envelope.Items.SEAL_STAMP).get()
                      : Envelope.Items.SEAL_STAMP.get();

                ItemStack newStampStack = stampStack.transmuteCopy(sealStamp);
                newStampStack.remove(DataComponents.DAMAGE);
                newStampStack.remove(DataComponents.MAX_DAMAGE);

                player.getSlot(Player.HELD_ITEM_SLOT).set(newStampStack);
                player.level().playSound(null, player, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.75f, 1f);
            }
        }
    }
}
