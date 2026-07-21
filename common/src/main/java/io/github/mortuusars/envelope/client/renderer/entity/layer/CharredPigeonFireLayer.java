package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.CharredPigeonModel;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class CharredPigeonFireLayer extends RenderLayer<CharredPigeon, CharredPigeonModel> {
    public static final RenderType RENDER_TYPE = RenderType.eyes(Envelope.resource("textures/entity/charred_pigeon/charred_pigeon_fire.png"));
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/entity/charred_pigeon/charred_pigeon_fire.png");

    public CharredPigeonFireLayer(RenderLayerParent<CharredPigeon, CharredPigeonModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CharredPigeon pigeon, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(
              RenderType.entityTranslucentEmissive(Envelope.resource("textures/entity/charred_pigeon/charred_pigeon_fire.png"))),
              0xFFFFFFFF, OverlayTexture.NO_OVERLAY);
    }
}
