package io.github.mortuusars.envelope.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

public class PigeonLegBandModel extends HierarchicalModel<Pigeon> {
	public final PigeonModel pigeonModel;
	public final ModelPart root;
	public final ModelPart legBand;

	public PigeonLegBandModel(PigeonModel pigeonModel, ModelPart root) {
		this.pigeonModel = pigeonModel;
		this.root = root;
		legBand = root.getChild("leg_band");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition part = mesh.getRoot();
		part.addOrReplaceChild("leg_band", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.5F, -0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 22.0F, 0.0F));
		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public @NotNull ModelPart root() {
		return this.root;
	}

	public void renderOnShoulder(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
								 float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, int tickCount) {
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(PigeonRenderer.LEG_BAND));
		renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, -1);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		poseStack.pushPose();
		pigeonModel.body.translateAndRotate(poseStack);
		pigeonModel.torso.translateAndRotate(poseStack);
		legBand.copyFrom(pigeonModel.rightLeg);
		super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}

	@Override
	public void setupAnim(Pigeon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}
}
