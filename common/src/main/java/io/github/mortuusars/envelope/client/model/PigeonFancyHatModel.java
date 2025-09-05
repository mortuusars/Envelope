package io.github.mortuusars.envelope.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

public class PigeonFancyHatModel extends HierarchicalModel<Pigeon> {
	public final PigeonModel pigeonModel;
	public final ModelPart root;
	public final ModelPart hat;

	public PigeonFancyHatModel(PigeonModel pigeonModel, ModelPart root) {
		this.pigeonModel = pigeonModel;
		this.root = root;
		hat = root.getChild("hat");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition part = mesh.getRoot();
		part.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -8.0F, -5.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(0, 9).addBox(-4.0F, -5.0F, -6.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 17.0F, -1.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public @NotNull ModelPart root() {
		return this.root;
	}

	public void renderOnShoulder(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
								 float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, int tickCount) {
		VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(PigeonRenderer.FANCY_HAT));
		renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, -1);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		poseStack.pushPose();
		pigeonModel.body.translateAndRotate(poseStack);
		hat.copyFrom(pigeonModel.head);
		super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}

	@Override
	public void setupAnim(Pigeon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}
}
