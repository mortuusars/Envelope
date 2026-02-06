package io.github.mortuusars.envelope.mixin.spawners;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.world.entity.spawner.BackgroundCourierSpawner;
import io.github.mortuusars.envelope.world.entity.spawner.FinishedBackgroundCourierSpawner;
import io.github.mortuusars.envelope.world.entity.spawner.Spawner;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {
    @Unique
    private final List<Spawner> envelope$spawners = List.of(
          new BackgroundCourierSpawner(),
          new FinishedBackgroundCourierSpawner()
    );

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess,
                               Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler,
                               boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler,
              isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (!MailService.operatesIn(this)) {
            return;
        }

        if (!Config.Server.DELIVERY_SPAWNING_RESPECTS_DOMOBSPAWNING_RULE.get()
              || getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            envelope$spawners.forEach(spawner -> spawner.tick((ServerLevel) (Object) this));
        }
    }
}
