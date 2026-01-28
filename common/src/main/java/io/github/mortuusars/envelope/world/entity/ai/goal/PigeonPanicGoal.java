package io.github.mortuusars.envelope.world.entity.ai.goal;

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
    public PigeonPanicGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    public PigeonPanicGoal(PathfinderMob mob, double speedModifier, TagKey<DamageType> panicCausingDamageTypes) {
        super(mob, speedModifier, panicCausingDamageTypes);
    }

    public PigeonPanicGoal(PathfinderMob mob, double speedModifier, Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes) {
        super(mob, speedModifier, panicCausingDamageTypes);
    }

    @Override
    protected boolean findRandomPosition() {
        if (!(mob.getLastDamageSource() instanceof DamageSource damageSource)) return false;
        if (!(damageSource.getDirectEntity() instanceof Entity entity)) return false;

        @Nullable Vec3 pos = PigeonAvoidEntityGoal.getPosAway(mob, 6, 3, entity.position());

        if (pos != null) {
            this.posX = pos.x;
            this.posY = pos.y;
            this.posZ = pos.z;
            return true;
        }

        return false;
    }
}
