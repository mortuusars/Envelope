package io.github.mortuusars.envelope.mixin.injected;

import io.github.mortuusars.envelope.world.pigeonhole.PigeonholeManager;
import io.github.mortuusars.envelope.world.pigeonhole.PigeonholeManagerHolder;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements PigeonholeManagerHolder {
    @Unique
    private final PigeonholeManager envelope$pigeonholeManager = new PigeonholeManager(((ServerLevel) (Object) this));

    @SuppressWarnings("AddedMixinMembersNamePattern") // Probability of name collision is close to zero.
    @Override
    public PigeonholeManager getEnvelopePigeonholeManager() {
        return envelope$pigeonholeManager;
    }
}
