package io.github.mortuusars.envelope.client.model;

import io.github.mortuusars.envelope.world.entity.CourierBat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.animation.definitions.BatAnimation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class CourierBatModel extends HierarchicalModel<CourierBat> {
    private final ModelPart root;
    private final AnimationDefinition batFlyingWithBackpack;
    private final AnimationDefinition testAnimation;

    public CourierBatModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;

        batFlyingWithBackpack = AnimationDefinition.Builder.withLength(0.5F)
              .looping()
              .addAnimation(
                    "head",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "head",
                    new AnimationChannel(
                          AnimationChannel.Targets.POSITION,
                          new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.375F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "body",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "body",
                    new AnimationChannel(
                          AnimationChannel.Targets.POSITION,
                          new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.375F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "feet",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(-21.25F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "right_wing",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, -55.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 35.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 45.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "right_wing_tip",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.0417F, KeyframeAnimations.degreeVec(0.0F, 65.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, -135.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "left_wing",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 55.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -35.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -45.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "left_wing_tip",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.0417F, KeyframeAnimations.degreeVec(0.0F, -65.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, 135.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .build();

        testAnimation = AnimationDefinition.Builder.withLength(0.5F)
              .looping()
              .addAnimation(
                    "head",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "head",
                    new AnimationChannel(
                          AnimationChannel.Targets.POSITION,
//                          new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.375F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "body",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "body",
                    new AnimationChannel(
                          AnimationChannel.Targets.POSITION,
//                          new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, 2.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.375F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.4583F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "feet",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(-21.25F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "right_wing",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, -55.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 35.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 45.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "right_wing_tip",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.0417F, KeyframeAnimations.degreeVec(0.0F, 65.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, -135.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "left_wing",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.125F, KeyframeAnimations.degreeVec(0.0F, 55.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -35.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -45.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .addAnimation(
                    "left_wing_tip",
                    new AnimationChannel(
                          AnimationChannel.Targets.ROTATION,
//                          new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.0417F, KeyframeAnimations.degreeVec(0.0F, -65.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
//                          new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, 135.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                          new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -50.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
              )
              .build();
    }

    public @NotNull ModelPart root() {
        return this.root;
    }

    public void setupAnim(CourierBat entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        animate(entity.flyAnimationState, entity.hasMail() ? batFlyingWithBackpack : BatAnimation.BAT_FLYING, ageInTicks, 1.0F);
    }
}
