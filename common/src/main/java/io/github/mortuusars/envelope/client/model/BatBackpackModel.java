package io.github.mortuusars.envelope.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.Envelope;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BatBackpackModel extends Model {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/entity/bat/misc/bat_backpack.png");

    private final ModelPart root;

    public BatBackpackModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
              "backpack",
              CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-3f, -1f, 0.999f, 6, 7, 5),
              PartPose.ZERO
        );

//        root.addOrReplaceChild(
//              "strap",
//              CubeListBuilder.create()
//                    .texOffs(0, 0)
//                    .addBox(0.75f, -1f, -1f, 1, 6f, 2, new CubeDeformation(0, 0, 0.01f)),
//              PartPose.rotation(0, 0, Mth.DEG_TO_RAD * 32.5f)
//        );

        root.addOrReplaceChild(
              "strap",
              CubeListBuilder.create()
                    .texOffs(0, 12)
                    .addBox(-0.5f, -3, -1, 1, 6, 2, new CubeDeformation(0, 0, 0.01f)),
              PartPose.offsetAndRotation(0, 2.5f, 0, 0, 0, Mth.DEG_TO_RAD * 32.5f)
        );

//        root.addOrReplaceChild(
//              "backpack",
//              CubeListBuilder.create()
//                    .texOffs(0, 0)
//                    .addBox(-4, 1.5f, -2.5f, 8, 5, 5),
//              PartPose.ZERO
//        );

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
