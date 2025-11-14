package io.github.mortuusars.envelope.mixin.injected;

import io.github.mortuusars.envelope.world.service.EnvelopeContext;
import io.github.mortuusars.envelope.world.service.EnvelopeContextHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements EnvelopeContextHolder {
    @Shadow public abstract @NotNull MinecraftServer getServer();

    @Unique
    private @Nullable EnvelopeContext envelope$envelopeContext;

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension,
                               RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration,
                               Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug,
                               long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler,
              isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @SuppressWarnings("AddedMixinMembersNamePattern") // Probability of name collision is close to zero.
    @Override
    public @NotNull EnvelopeContext getEnvelopeContext() {
        if (dimension() != OVERWORLD) {
            return getServer().overworld().getEnvelopeContext();
        }
        if (envelope$envelopeContext == null) {
            envelope$envelopeContext = new EnvelopeContext((ServerLevel) (Object) this);
        }
        return envelope$envelopeContext;
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (envelope$envelopeContext != null) {
            getProfiler().popPush("envelope");
            envelope$envelopeContext.tick();
        }
    }
}
