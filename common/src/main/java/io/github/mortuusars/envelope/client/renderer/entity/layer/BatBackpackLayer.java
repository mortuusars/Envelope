package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.BatBackpackModel;
import io.github.mortuusars.envelope.client.model.CourierBatModel;
import io.github.mortuusars.envelope.world.entity.CourierBat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class BatBackpackLayer extends RenderLayer<CourierBat, CourierBatModel> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Envelope.resource("bat_backpack"), "main");

    protected final BatBackpackModel model;

    public BatBackpackLayer(RenderLayerParent<CourierBat, CourierBatModel> renderer, ModelPart root) {
        super(renderer);
        model = new BatBackpackModel(root);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CourierBat bat, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!bat.hasMail()) {
            return;
        }

        poseStack.pushPose();

        CourierBatModel batModel = getParentModel();
        ModelPart body = batModel.root().getChild("body");
        body.translateAndRotate(poseStack);
//        ModelPart feet = body.getChild("feet");
//        feet.translateAndRotate(poseStack);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(BatBackpackModel.TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}