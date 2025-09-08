package io.github.mortuusars.envelope.world.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class PigeonFlyingMoveControl extends MoveControl {
    private final int maxTurn;
    private final boolean hoversInPlace;

    public PigeonFlyingMoveControl(Mob mob, int maxTurn, boolean hoversInPlace) {
        super(mob);
        this.maxTurn = maxTurn;
        this.hoversInPlace = hoversInPlace;
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            this.mob.setNoGravity(true);
            double distX = this.wantedX - this.mob.getX();
            double distY = this.wantedY - this.mob.getY();
            double distZ = this.wantedZ - this.mob.getZ();
            double dist = distX * distX + distY * distY + distZ * distZ;
//            if (dist < 2.5000003E-7F) {
//                this.mob.setYya(0.0F);
//                this.mob.setZza(0.0F);
//                return;
//            }

            float h = (float)(Mth.atan2(distZ, distX) * 180.0F / (float)Math.PI) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), h, 90.0F));
            float speed;
            if (this.mob.onGround()) {
                speed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            } else {
                speed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED));
            }

            this.mob.setSpeed(speed);
            double j = Math.sqrt(distX * distX + distZ * distZ);
            if (Math.abs(distY) > 1.0E-5F || Math.abs(j) > 1.0E-5F) {
                float rot = (float)(-(Mth.atan2(distY, j) * 180.0F / (float)Math.PI));
                this.mob.setXRot(this.rotlerp(this.mob.getXRot(), rot, (float)this.maxTurn));
                this.mob.setYya((distY > 0.0 ? speed : -speed));
            }
        } else {
            if (!this.hoversInPlace) {
                this.mob.setNoGravity(false);
            }

            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }
    }
}
