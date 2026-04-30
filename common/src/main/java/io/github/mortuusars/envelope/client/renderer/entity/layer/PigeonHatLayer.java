package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class PigeonHatLayer extends RenderLayer<Pigeon, PigeonModel> {
    public static final ModelLayerLocation PIGEON_HAT = new ModelLayerLocation(Envelope.resource("pigeon_hat"), "main");

    public static final ResourceLocation TEXTURE = Envelope.resource("textures/entity/pigeon/misc/pigeon_hat.png");

    protected final PigeonModel model;

    public PigeonHatLayer(RenderLayerParent<Pigeon, PigeonModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        model = new PigeonModel(modelSet.bakeLayer(PIGEON_HAT));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Pigeon pigeon, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!pigeon.isBaby() && pigeon.hasMailmanHat()) {
            coloredCutoutModelCopyLayerRender(getParentModel(), model,TEXTURE, poseStack, buffer, packedLight, pigeon,
                  limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, -1);
        }
    }
}
