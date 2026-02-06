package io.github.mortuusars.envelope.mixin.injected;

import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.MailServiceHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements MailServiceHolder {
    @Shadow
    public abstract @NotNull MinecraftServer getServer();

    @Unique
    private MailService envelope$mailService;

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension,
                               RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration,
                               Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug,
                               long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler,
              isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(MinecraftServer server, Executor dispatcher, LevelStorageSource.LevelStorageAccess levelStorageAccess,
                        ServerLevelData serverLevelData, ResourceKey<Level> dimension, LevelStem levelStem,
                        ChunkProgressListener progressListener, boolean isDebug, long biomeZoomSeed,
                        List<CustomSpawner> customSpawners, boolean tickTime, RandomSequences randomSequences, CallbackInfo ci) {
        if (dimension() == OVERWORLD) { // Mail works only in overworld, hence why it's only instantiated on it.
            envelope$mailService = MailService.create((ServerLevel) (Object) this);
        }
    }

    @SuppressWarnings("AddedMixinMembersNamePattern") // Probability of name collision is close to zero.
    @Override
    public @NotNull MailService getEnvelopeMailService() {
        if (dimension() != OVERWORLD) {
            return getServer().overworld().getEnvelopeMailService();
        }
        return Objects.requireNonNull(envelope$mailService, "Attempting to get MailService too early.");
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (envelope$mailService != null) {
            getProfiler().popPush("envelope_mail_service");
            envelope$mailService.tick();
        }
    }
}