package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.CharredPigeonModel;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class CharredPigeonBackpackLayer extends RenderLayer<CharredPigeon, CharredPigeonModel> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Envelope.resource("charred_pigeon_backpack"), "main");

    public static final ResourceLocation TEXTURE = Envelope.resource("textures/entity/charred_pigeon/charred_pigeon_backpack.png");

    protected final CharredPigeonModel model;

    public CharredPigeonBackpackLayer(RenderLayerParent<CharredPigeon, CharredPigeonModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        model = new CharredPigeonModel(modelSet.bakeLayer(MODEL_LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CharredPigeon pigeon, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!pigeon.isBaby() && pigeon.hasMail()) {
            coloredCutoutModelCopyLayerRender(getParentModel(), model, TEXTURE, poseStack, buffer, packedLight, pigeon,
                  limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, -1);
        }
    }
}
