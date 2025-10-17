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
	public final ModelPart root;
	public final ModelPart body;
	public final ModelPart torso;
	public final ModelPart head;
	public final ModelPart leftLeg;
	public final ModelPart rightLeg;
	public final ModelPart tail;
	public final ModelPart leftWing;
	public final ModelPart rightWing;

	public PigeonModel(ModelPart root) {
		this.root = root;
		body = root.getChild("body");
		torso = body.getChild("torso");
		head = torso.getChild("head");
		leftLeg = torso.getChild("left_leg");
		rightLeg = torso.getChild("right_leg");
		tail = torso.getChild("tail");
		leftWing = torso.getChild("left_wing");
		rightWing = torso.getChild("right_wing");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 14).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 16).addBox(0.0F, -2.0F, -7.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, -1.0F));
		PartDefinition leftLeg = torso.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(9, 26).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(6, 28).addBox(-1.0F, 2.0F, -2.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, -2.0F, 0.0F));
		PartDefinition rightLeg = torso.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(7, 26).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(0, 28).addBox(-2.0F, 2.0F, -2.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -2.0F, 0.0F));
		PartDefinition tail = torso.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, 0.0F, -1.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 4.0F, 0.7854F, 0.0F, 0.0F));
		PartDefinition leftWing = torso.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(46, 3).mirror().addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(4.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.3927F));
		PartDefinition rightWing = torso.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(32, 3).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	public void setupAnim(Pigeon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.setupAnim(getState(entity), entity, entity.tickCount, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
	}

	private void setupAnim(PigeonModel.State state, @Nullable Pigeon pigeon, int tickCount, float limbSwing, float limbSwingAmount,
						   float ageInTicks, float netHeadYaw, float headPitch) {
		body.xRot = 0f;
		body.y = 24F;
		head.xRot = headPitch * (float) (Math.PI / 180.0);
		head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
		head.zRot = 0.0F;
		leftLeg.y = -2f;
		rightLeg.y = -2f;
		leftLeg.z = 0;
		rightLeg.z = 0;
		leftLeg.xRot = 0f;
		rightLeg.xRot = 0f;
		leftLeg.yRot = 0f;
		rightLeg.yRot = 0f;
		tail.xRot = 0f;

		if (pigeon != null && pigeon.isBaby()) {
			head.xScale = 1.3f;
			head.yScale = 1.3f;
			head.zScale = 1.3f;
		} else {
			head.xScale = 1f;
			head.yScale = 1f;
			head.zScale = 1f;
		}

		switch (state) {
			case STANDING:
				tail.xRot = 1.015F + Mth.cos(limbSwing * 0.6662F) * 0.3F * limbSwingAmount;
				leftWing.zRot = -0.3f - 0.0873F - ageInTicks;
				rightWing.zRot = 0.3f + 0.0873F + ageInTicks;
				leftLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
				rightLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
				break;
			case FLYING:
				leftWing.zRot = -0.3f + -0.0873F - ageInTicks;
				rightWing.zRot = 0.3f + 0.0873F + ageInTicks;
				leftLeg.xRot = 0.85f;
				rightLeg.xRot = 0.85f;

				float anim = ((tickCount + Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true)) % 60) / 60;
				// Back and forth
				anim *= 2;
				if (anim > 1) {
					anim = 2 - anim;
				}
				anim = ((float) EasingFunction.EASE_IN_OUT_QUAD.ease(anim));
				body.xRot = anim * 0.1f;
				tail.xRot = 1.3f + (anim * 0.1f);
				break;
			case ON_SHOULDER:
				leftLeg.xRot = 0f;
				rightLeg.xRot = 0f;
				break;
			case SITTING:
				tail.xRot = 1.3f;
				body.y = 26F;

				leftWing.zRot = -0.3f - 0.0873F - ageInTicks;
				rightWing.zRot = 0.3f + 0.0873F + ageInTicks;

				// Funny sitting
				if (pigeon != null && pigeon.getUUID().hashCode() % 8 == 0) {
					leftLeg.xRot = -1.57f;
					rightLeg.xRot = -1.57f;
					leftLeg.yRot = -0.3f;
					rightLeg.yRot = 0.3f;
					leftLeg.y = -2.1f;
					rightLeg.y = -2.1f;
					leftLeg.z = -4f;
					rightLeg.z = -4f;
				} else {
					leftLeg.xRot = 0f;
					rightLeg.xRot = 0f;
					leftLeg.yRot = 0f;
					rightLeg.yRot = 0f;
					leftLeg.y = 0f;
					rightLeg.y = 0f;
					leftLeg.z = 0f;
					rightLeg.z = 0f;
				}

				break;
			default:
				break;
		}
	}

	private PigeonModel.State getState(Pigeon pigeon) {
		if (pigeon.isSitting()) {
			return PigeonModel.State.SITTING;
		} else {
			return pigeon.isFlying() ? PigeonModel.State.FLYING : PigeonModel.State.STANDING;
		}
	}

	@Override
	protected @NotNull Iterable<ModelPart> headParts() {
		return ImmutableList.of();
	}

	@Override
	protected @NotNull Iterable<ModelPart> bodyParts() {
		return ImmutableList.of(this.body);
	}

	public enum State {
		FLYING,
		STANDING,
		SITTING,
		ON_SHOULDER;
	}
}
