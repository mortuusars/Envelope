package io.github.mortuusars.envelope.client.renderer.entity;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.CharredPigeonModel;
import io.github.mortuusars.envelope.client.renderer.entity.layer.CharredPigeonBackpackLayer;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class CharredPigeonRenderer extends MobRenderer<CharredPigeon, CharredPigeonModel> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Envelope.resource("charred_pigeon"), "main");

    public static final ResourceLocation TEXTURE = Envelope.resource("textures/entity/charred_pigeon/charred_pigeon.png");

    public CharredPigeonRenderer(EntityRendererProvider.Context context) {
        super(context, new CharredPigeonModel(context.bakeLayer(MODEL_LAYER)), 0.35f);
        addLayer(new CharredPigeonBackpackLayer(this, context.getModelSet()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CharredPigeon entity) {
        return TEXTURE;
    }

    @Override
    protected float getBob(CharredPigeon pigeon, float partialTick) {
        float f = Mth.lerp(partialTick, pigeon.oFlap, pigeon.flap);
        float g = Mth.lerp(partialTick, pigeon.oFlapSpeed, pigeon.flapSpeed);
        return (Mth.sin(f) + 1.0F) * g;
    }
}