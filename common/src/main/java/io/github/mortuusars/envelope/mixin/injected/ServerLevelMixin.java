package io.github.mortuusars.envelope.mixin.injected;

import io.github.mortuusars.envelope.world.service.EnvelopeContext;
import io.github.mortuusars.envelope.world.service.EnvelopeContextHolder;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements EnvelopeContextHolder {
    @SuppressWarnings("DataFlowIssue")
    @Unique
    private final EnvelopeContext envelope$envelopeContext = new EnvelopeContext((ServerLevel) (Object) this);

    @SuppressWarnings("AddedMixinMembersNamePattern") // Probability of name collision is close to zero.
    @Override
    public EnvelopeContext getEnvelopeContext() {
        return envelope$envelopeContext;
    }
}
