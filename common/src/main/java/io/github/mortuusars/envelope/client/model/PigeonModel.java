package io.github.mortuusars.envelope.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class PigeonModel extends HierarchicalModel<Pigeon> {
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart torso;
	private final ModelPart head;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;
	private final ModelPart tail;
	private final ModelPart leftWing;
	private final ModelPart rightWing;

	public PigeonModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.torso = this.body.getChild("torso");
		this.head = this.torso.getChild("head");
		this.leftLeg = this.torso.getChild("left_leg");
		this.rightLeg = this.torso.getChild("right_leg");
		this.tail = this.torso.getChild("tail");
		this.leftWing = this.torso.getChild("left_wing");
		this.rightWing = this.torso.getChild("right_wing");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 14).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 16).addBox(0.0F, -2.0F, -7.0F, 0.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, -1.0F));
		PartDefinition leftLeg = torso.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(9, 26).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(6, 28).addBox(-1.0F, 2.0F, -2.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, -2.0F, 0.0F));
		PartDefinition right_leg = torso.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(7, 26).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(0, 28).addBox(-2.0F, 2.0F, -2.0F, 3.0F, 0.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(0, 31).addBox(-1.5F, 0.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -2.0F, 0.0F));
		PartDefinition tail = torso.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, 0.0F, -1.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 4.0F, 0.7854F, 0.0F, 0.0F));
		PartDefinition leftWing = torso.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(46, 3).mirror().addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(4.0F, -7.0F, 0.0F, 0.0F, 0.0F, -0.3927F));
		PartDefinition rightWing = torso.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(32, 3).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -7.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public @NotNull ModelPart root() {
		return this.root;
	}

	public void setupAnim(Pigeon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.setupAnim(getState(entity), entity.tickCount, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
	}

	public void prepareMobModel(Pigeon entity, float limbSwing, float limbSwingAmount, float partialTick) {
		this.prepare(getState(entity));
	}

	public void renderOnShoulder(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
								 float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, int tickCount) {
		this.prepare(PigeonModel.State.ON_SHOULDER);
		this.setupAnim(PigeonModel.State.ON_SHOULDER, tickCount, limbSwing, limbSwingAmount, 0.0F, netHeadYaw, headPitch);
		this.root.render(poseStack, buffer, packedLight, packedOverlay);
	}

	private void setupAnim(PigeonModel.State state, int tickCount, float limbSwing, float limbSwingAmount,
						   float ageInTicks, float netHeadYaw, float headPitch) {
		body.y = 24F;
		head.xRot = headPitch * (float) (Math.PI / 180.0);
		head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
		head.zRot = 0.0F;

		switch (state) {
			case STANDING:
				tail.xRot = 1.015F + Mth.cos(limbSwing * 0.6662F) * 0.3F * limbSwingAmount;
				leftWing.zRot = -0.3f + -0.0873F - ageInTicks;
				rightWing.zRot = 0.3f + 0.0873F + ageInTicks;
				leftLeg.xRot = leftLeg.xRot + Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
				rightLeg.xRot = rightLeg.xRot + Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
				leftLeg.xRot = 0f;
				rightLeg.xRot = 0f;
				break;
			case FLYING:
				leftWing.zRot = -0.3f + -0.0873F - ageInTicks;
				rightWing.zRot = 0.3f + 0.0873F + ageInTicks;
				leftLeg.xRot = 0.85f;
				rightLeg.xRot = 0.85f;
				break;
			case ON_SHOULDER:
				leftLeg.xRot = 0f;
				rightLeg.xRot = 0f;
				break;
			case SITTING:
				tail.xRot = 1.6f;
				body.y = 26.5F;
				break;
			default:
				break;
		}
	}

	private void prepare(PigeonModel.State state) {
//		switch (state) {
//			case FLYING:
////				this.leftLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
////				this.rightLeg.xRot += (float) (Math.PI * 2.0 / 9.0);
//			case STANDING:
//			case ON_SHOULDER:
//			default:
//				this.body.y = 24F;
//				break;
//			case SITTING:
//				this.body.y = 26.5F;
//				break;
//		}
	}

	private PigeonModel.State getState(Pigeon pigeon) {
		if (pigeon.isInSittingPose()) {
			return PigeonModel.State.SITTING;
		} else {
			return pigeon.isFlying() ? PigeonModel.State.FLYING : PigeonModel.State.STANDING;
		}
	}

	@Environment(EnvType.CLIENT)
	public enum State {
		FLYING,
		STANDING,
		SITTING,
		ON_SHOULDER;
	}
}
