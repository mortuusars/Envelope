package io.github.mortuusars.envelope.client.model;

import com.google.common.collect.ImmutableList;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.util.EasingFunction;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PigeonModel extends AgeableListModel<Pigeon> {
    private final ModelPart root;

    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart hat;

    private final ModelPart body;
    private final ModelPart backpackStraps;
    private final ModelPart backpackBag;

    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public PigeonModel(ModelPart root) {
        // bodyYOffset = 24 * (babyBodyScale - 1)
        // babyYHeadOffset = I have not been able to crack the code. so have a magic number instead.
        super(true, 6.7F, 0, 1.75F, 2.1F, 24F * 1.1F);
        this.root = root;

        head = root.getChild("head");
        beak = head.getChild("beak");
        hat = head.getChild("hat");

        body = root.getChild("body");
        backpackStraps = body.getChild("backpack_straps");
        backpackBag = body.getChild("backpack_bag");

        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        tail = root.getChild("tail");
        leftWing = root.getChild("left_wing");
        rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        PartDefinition head = part.addOrReplaceChild("head",
              CubeListBuilder.create()
                    .texOffs(0, 14)
                    .addBox(-3.0F, -4.0F, -5.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE),
              PartPose.offset(0.0F, 16.0F, -1.0F));

        PartDefinition beak = head.addOrReplaceChild("beak",
              CubeListBuilder.create()
                    .texOffs(0, 14)
                    .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.01F))
                    .texOffs(0, 16)
                    .addBox(0.0F, 0.0F, -2.0F, 0.0F, 1.0F, 1.0F, CubeDeformation.NONE),
              PartPose.offset(0.0F, -1.0F, -5.0F));

        PartDefinition hat = head.addOrReplaceChild("hat",
              CubeListBuilder.create()
                    .texOffs(1, 46)
                    .addBox(-3.5F, -2.0F, -3.5F, 7.0F, 2.0F, 7.0F, CubeDeformation.NONE)
                    .texOffs(0, 55)
                    .addBox(-3.5F, 0.0F, -4.5F, 7.0F, 1.0F, 8.0F, CubeDeformation.NONE),
              PartPose.offsetAndRotation(0.0F, -4.0F, -2.0F, -0.1745F, 0.0F, 0.0F));

        PartDefinition body = part.addOrReplaceChild("body",
              CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 6.0F, 8.0F, CubeDeformation.NONE),
              PartPose.offset(0.0F, 24.05F, 0.0F));

        PartDefinition backpackStraps = body.addOrReplaceChild("backpack_straps",
              CubeListBuilder.create()
                    .texOffs(32, 50)
                    .addBox(-4.0F, -3.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0, 0.01F, 0.01F)),
              PartPose.offset(0.0F, -5.0F, 0.0F));

        PartDefinition backpackBag = body.addOrReplaceChild("backpack_bag",
              CubeListBuilder.create()
                    .texOffs(32, 38)
                    .addBox(-5.0F, -3.0F, -3.0F, 10.0F, 6.0F, 6.0F, CubeDeformation.NONE),
              PartPose.offsetAndRotation(0.0F, -9.0F, 2.0F, 0.7854F, 0.0F, 0.0F));


        PartDefinition leftLeg = part.addOrReplaceChild("left_leg", CubeListBuilder.create()
                    .texOffs(9, 26)
                    .addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, CubeDeformation.NONE)
                    .texOffs(6, 28)
                    .addBox(-1.0F, 2.0F, -3.0F, 3.0F, 0.0F, 3.0F, CubeDeformation.NONE),
              PartPose.offset(1.0F, 22.0F, 1.0F));

        PartDefinition rightLeg = part.addOrReplaceChild("right_leg", CubeListBuilder.create()
                    .texOffs(7, 26)
                    .addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, CubeDeformation.NONE)
                    .texOffs(0, 28)
                    .addBox(-2.0F, 2.0F, -3.0F, 3.0F, 0.0F, 3.0F, CubeDeformation.NONE),
              PartPose.offset(-1.0F, 22.0F, 1.0F));

        PartDefinition tail = part.addOrReplaceChild("tail",
              CubeListBuilder.create()
                    .texOffs(24, 0)
                    .addBox(-3.0F, 0.0F, -1.0F, 6.0F, 5.0F, 1.0F, CubeDeformation.NONE),
              PartPose.offsetAndRotation(0.0F, 19.0F, 4.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition leftWing = part.addOrReplaceChild("left_wing",
              CubeListBuilder.create()
                    .texOffs(46, 3).mirror()
                    .addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F, CubeDeformation.NONE).mirror(false),
              PartPose.offsetAndRotation(4.0F, 17.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition rightWing = part.addOrReplaceChild("right_wing",
              CubeListBuilder.create()
                    .texOffs(32, 3)
                    .addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F, CubeDeformation.NONE),
              PartPose.offsetAndRotation(-4.0F, 17.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setupAnim(Pigeon pigeon, float limbSwing, float limbSwingAmount, float bob, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);

        head.xRot = headPitch * (float) (Math.PI / 180.0);
        head.yRot = netHeadYaw * (float) (Math.PI / 180.0);

        switch (getState(pigeon)) {
            case FLYING -> {
                leftWing.zRot = -bob;
                rightWing.zRot = bob;

                leftLeg.xRot = 0.75f * limbSwingAmount;
                leftLeg.yRot = -0.15f;
                rightLeg.xRot = 0.75f * limbSwingAmount;
                rightLeg.yRot = 0.15f;

                // Rock back and forth
                float anim = ((pigeon.tickCount + Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true)) % 60) / 60;
                anim *= 2;
                if (anim > 1) {
                    anim = 2 - anim;
                }
                anim = ((float) EasingFunction.EASE_IN_OUT_QUAD.ease(anim));
                body.xRot = anim * 0.1f;
                head.xRot = -anim * 0.05f;
                head.z -= 0.75F * (anim);

                leftWing.xRot += (anim * 0.2f);
                leftWing.y -= 0.35F * (anim);
                leftWing.z -= 0.5F * (anim);

                rightWing.xRot += (anim * 0.2f);
                rightWing.y -= 0.35F * (anim);
                rightWing.z -= 0.5F * (anim);

                tail.xRot += (limbSwingAmount * 0.75F) + (anim * 0.1f);
                tail.y -= 0.35F * (anim);
                tail.z -= 0.5F * (anim);
            }
            case STANDING -> {
                leftWing.zRot = -0.3927F;
                rightWing.zRot = 0.3927F;
                leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
                rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            }
            case SITTING -> {
                body.y += 1.9F;
                head.y += pigeon.isBaby() ? 1.9F : 2.9F;

                leftWing.zRot = -0.2F;
                rightWing.zRot = 0.2F;
                leftWing.y += 2;
                rightWing.y += 2;

                tail.xRot = 0.75f;
            }
        }
    }

    public PigeonModel.State getState(@Nullable Pigeon pigeon) {
        if (pigeon == null) return State.STANDING;
        if (pigeon.isSitting()) return State.SITTING;
        return pigeon.isFlying() ? State.FLYING : State.STANDING;
    }

    @Override
    protected @NotNull Iterable<ModelPart> headParts() {
        return ImmutableList.of(head);
    }

    @Override
    protected @NotNull Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(body, rightLeg, leftLeg, rightWing, leftWing, tail);
    }

    public enum State {
        FLYING,
        STANDING,
        SITTING
    }
}
