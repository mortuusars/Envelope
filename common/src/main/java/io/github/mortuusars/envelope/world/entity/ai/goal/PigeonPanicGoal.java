package io.github.mortuusars.envelope.world.entity.ai.goal;

import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class PigeonPanicGoal extends PanicGoal {
    public PigeonPanicGoal(Pigeon pigeon, double speedModifier) {
        super(pigeon, speedModifier);
    }

    public PigeonPanicGoal(Pigeon pigeon, double speedModifier, TagKey<DamageType> panicCausingDamageTypes) {
        super(pigeon, speedModifier, panicCausingDamageTypes);
    }

    public PigeonPanicGoal(Pigeon pigeon, double speedModifier, Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes) {
        super(pigeon, speedModifier, panicCausingDamageTypes);
    }

    @Override
    protected boolean findRandomPosition() {
        if (!(mob.getLastDamageSource() instanceof DamageSource damageSource)) return false;
        if (!(damageSource.getDirectEntity() instanceof Entity entity)) return false;

        @Nullable Vec3 pos = PigeonAvoidEntityGoal.getPosAway(mob, 14, 8, entity.position());

        if (pos != null) {
            this.posX = pos.x;
            this.posY = pos.y;
            this.posZ = pos.z;
            return true;
        }

        return false;
    }
}
