package io.github.mortuusars.envelope.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.PigeonFancyHatModel;
import io.github.mortuusars.envelope.client.model.PigeonLegBandModel;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.model.geom.EnvelopeModelLayers;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PigeonOnShoulderLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
    private final PigeonModel pigeonModel;
    private final PigeonLegBandModel legBandModel;
    private final PigeonFancyHatModel hatModel;

    public PigeonOnShoulderLayer(RenderLayerParent<T, PlayerModel<T>> renderer, EntityModelSet modelSet) {
        super(renderer);
        pigeonModel = new PigeonModel(modelSet.bakeLayer(EnvelopeModelLayers.PIGEON));
        legBandModel = new PigeonLegBandModel(pigeonModel, modelSet.bakeLayer(EnvelopeModelLayers.PIGEON_LEG_BAND));
        hatModel = new PigeonFancyHatModel(pigeonModel, modelSet.bakeLayer(EnvelopeModelLayers.PIGEON_FANCY_HAT));
    }

    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.render(poseStack, buffer, packedLight, livingEntity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        this.render(poseStack, buffer, packedLight, livingEntity, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
    }

    private void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity,
                        float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean leftShoulder) {
        CompoundTag compoundTag = leftShoulder ? livingEntity.getShoulderEntityLeft() : livingEntity.getShoulderEntityRight();
        EntityType.byString(compoundTag.getString("id"))
                .filter(type -> type == Envelope.EntityTypes.PIGEON.get())
                .ifPresent(
                        type -> {
                            poseStack.pushPose();
                            poseStack.scale(0.75f, 0.75f, 0.75f);
                            poseStack.translate(leftShoulder ? 0.55F : -0.55F, livingEntity.isCrouching() ? -1.2F : -1.5F, 0.05F);
                            Pigeon.Variant variant = Pigeon.Variant.byId(compoundTag.getInt("Variant"));
                            VertexConsumer vertexConsumer = buffer.getBuffer(this.pigeonModel.renderType(PigeonRenderer.getVariantTexture(variant)));
                            pigeonModel.renderOnShoulder(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                                    limbSwing, limbSwingAmount, netHeadYaw, headPitch, livingEntity.tickCount);
                            if (compoundTag.getBoolean("Homing")) {
                                legBandModel.renderOnShoulder(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                                        limbSwing, limbSwingAmount, netHeadYaw, headPitch, livingEntity.tickCount);
                            }

                            if (compoundTag.hasUUID("Owner")) {
                                UUID uuid = compoundTag.getUUID("Owner");
                                //TODO: fancy hat
                                /*if () {
                                    hatModel.renderOnShoulder(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY,
                                            limbSwing, limbSwingAmount, netHeadYaw, headPitch, livingEntity.tickCount);
                                }*/
                            }
                            poseStack.popPose();
                        }
                );
    }
}
