package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.BatBackpackModel;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.ambient.Bat;

public class BatBackpackLayer extends RenderLayer<Bat, BatModel> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Envelope.resource("bat_backpack"), "main");

    protected final BatBackpackModel model;

    public BatBackpackLayer(RenderLayerParent<Bat, BatModel> renderer, ModelPart root) {
        super(renderer);
        model = new BatBackpackModel(root);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Bat bat, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
//        if (!bat.hasMail()) {
//            return;
//        }

        poseStack.pushPose();

        BatModel batModel = getParentModel();
        ModelPart body = batModel.root().getChild("body");
        body.translateAndRotate(poseStack);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(BatBackpackModel.TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}