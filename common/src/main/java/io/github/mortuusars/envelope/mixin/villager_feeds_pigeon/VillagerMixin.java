package io.github.mortuusars.envelope.mixin.villager_feeds_pigeon;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.VillagerPigeonFeeding;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager
      implements ReputationEventHandler, VillagerDataHolder, VillagerPigeonFeeding.FeedingVillager {
    @Unique
    public int envelope$pigeonFoodPickupDelay = 0; // Villagers pick up the seeds themselves, so we need to block that when feeding
    @Unique
    public int envelope$pigeonFeedCooldown = 0;

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void envelope$addPigeonFoodPickupDelay(int delay) {
        envelope$pigeonFoodPickupDelay = delay;
    }

    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    private void onCustomServerAiStep(CallbackInfo ci) {
        Villager villager = ((Villager) (Object) this);

        if (envelope$pigeonFoodPickupDelay > 0) envelope$pigeonFoodPickupDelay--;
        if (envelope$pigeonFeedCooldown > 0) envelope$pigeonFeedCooldown--;

        if (Config.Server.VILLAGER_FEEDING_PIGEONS.get()
              && envelope$pigeonFeedCooldown <= 0
              && VillagerPigeonFeeding.tryFeed(villager)) {
            envelope$pigeonFeedCooldown = VillagerPigeonFeeding.getFeedCooldown(villager);
        }
    }

    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void wantsToPickUp(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (envelope$pigeonFoodPickupDelay > 0 && stack.is(Envelope.Tags.Items.PIGEON_FOOD)) {
            cir.setReturnValue(false);
        }
    }

    // --

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void save(CompoundTag tag, CallbackInfo ci) {
        if (envelope$pigeonFoodPickupDelay > 0) tag.putInt("EnvelopeItemPickupDelay", envelope$pigeonFoodPickupDelay);
        if (envelope$pigeonFeedCooldown > 0) tag.putInt("EnvelopePigeonFeedCooldown", envelope$pigeonFeedCooldown);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void load(CompoundTag tag, CallbackInfo ci) {
        envelope$pigeonFoodPickupDelay = tag.getInt("EnvelopeItemPickupDelay");
        envelope$pigeonFeedCooldown = tag.getInt("EnvelopePigeonFeedCooldown");
    }
}
