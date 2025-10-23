package io.github.mortuusars.envelope.mixin.injected;

import io.github.mortuusars.envelope.world.EnvelopeContext;
import io.github.mortuusars.envelope.world.EnvelopeContextHolder;
import io.github.mortuusars.envelope.world.PlayerInformation;
import io.github.mortuusars.envelope.world.PlayerInformationHolder;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements EnvelopeContextHolder, PlayerInformationHolder {
    @SuppressWarnings("DataFlowIssue")
    @Unique
    private final EnvelopeContext envelope$envelopeContext = new EnvelopeContext((ServerLevel) (Object) this);
    @SuppressWarnings("DataFlowIssue")
    @Unique
    private final PlayerInformation envelope$playerInformation = new PlayerInformation((ServerLevel) (Object) this);

    @SuppressWarnings("AddedMixinMembersNamePattern") // Probability of name collision is close to zero.
    @Override
    public EnvelopeContext getEnvelopeContext() {
        return envelope$envelopeContext;
    }

    @SuppressWarnings("AddedMixinMembersNamePattern") // Probability of name collision is close to zero.
    @Override
    public PlayerInformation getEnvelopePlayerInformation() {
        return envelope$playerInformation;
    }
}
