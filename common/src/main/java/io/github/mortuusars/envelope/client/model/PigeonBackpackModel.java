package io.github.mortuusars.envelope.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.jetbrains.annotations.NotNull;

public class PigeonBackpackModel extends HierarchicalModel<Pigeon> {
	public final PigeonModel pigeonModel;
	public final ModelPart root;
	public final ModelPart backpack;

	public PigeonBackpackModel(PigeonModel pigeonModel, ModelPart root) {
		this.pigeonModel = pigeonModel;
		this.root = root;
		backpack = root.getChild("backpack");
	}

	public static LayerDefinition createLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition part = mesh.getRoot();
		part.addOrReplaceChild("backpack", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, 5.0F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
			.texOffs(0, 28).addBox(-3.5F, -5.5F, 1.5F, 1.0F, 7.0F, 9.0F, new CubeDeformation(0.0F))
			.texOffs(0, 12).addBox(2.5F, -5.5F, 1.5F, 1.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 21.0F, -6.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public @NotNull ModelPart root() {
		return this.root;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		poseStack.pushPose();
		pigeonModel.body.translateAndRotate(poseStack);
		backpack.copyFrom(pigeonModel.torso);
		backpack.z = -6f;
		backpack.y = -3f;
		backpack.render(poseStack, buffer, packedLight, packedOverlay, color);
		poseStack.popPose();
	}

	@Override
	public void setupAnim(Pigeon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}
