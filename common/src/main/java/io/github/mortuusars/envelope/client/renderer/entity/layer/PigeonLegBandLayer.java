package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.client.model.PigeonLegBandModel;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.model.geom.EnvelopeModelLayers;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class PigeonLegBandLayer extends RenderLayer<Pigeon, PigeonModel> {
    protected final PigeonModel pigeonModel;
    protected final PigeonLegBandModel legBandModel;

    public PigeonLegBandLayer(RenderLayerParent<Pigeon, PigeonModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        pigeonModel = renderer.getModel();
        legBandModel = new PigeonLegBandModel(pigeonModel, modelSet.bakeLayer(EnvelopeModelLayers.PIGEON_LEG_BAND));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Pigeon pigeon, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (pigeon.isDelivering()) {
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(PigeonRenderer.LEG_BAND));
            legBandModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
