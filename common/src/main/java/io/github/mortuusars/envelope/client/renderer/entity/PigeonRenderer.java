package io.github.mortuusars.envelope.client.renderer.entity;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.model.geom.EnvelopeModelLayers;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonBackpackLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonFancyHatLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonLegBandLayer;
import io.github.mortuusars.envelope.client.renderer.entity.layer.PigeonMailLayer;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class PigeonRenderer extends MobRenderer<Pigeon, PigeonModel> {
    public static final ResourceLocation GRAY = Envelope.resource("textures/entity/pigeon/pigeon_gray.png");
    public static final ResourceLocation BROWN = Envelope.resource("textures/entity/pigeon/pigeon_brown.png");
    public static final ResourceLocation WHITE = Envelope.resource("textures/entity/pigeon/pigeon_white.png");

    public static final ResourceLocation LEG_BAND_TEXTURE = Envelope.resource("textures/entity/pigeon/pigeon_leg_band.png");
    public static final ResourceLocation BACKPACK_TEXTURE = Envelope.resource("textures/entity/pigeon/pigeon_backpack.png");
    public static final ResourceLocation FANCY_HAT_TEXTURE = Envelope.resource("textures/entity/pigeon/pigeon_fancy_hat.png");

    public PigeonRenderer(EntityRendererProvider.Context context) {
        super(context, new PigeonModel(context.bakeLayer(EnvelopeModelLayers.PIGEON)), 0.35f);
        addLayer(new PigeonLegBandLayer(this, context.getModelSet()));
        addLayer(new PigeonBackpackLayer(this, context.getModelSet()));
        addLayer(new PigeonFancyHatLayer(this, context.getModelSet()));
        addLayer(new PigeonMailLayer(this, context.getItemRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(Pigeon entity) {
        return getVariantTexture(entity.getVariant());
    }

    @Override
    protected float getBob(Pigeon livingBase, float partialTick) {
        float f = Mth.lerp(partialTick, livingBase.oFlap, livingBase.flap);
        float g = Mth.lerp(partialTick, livingBase.oFlapSpeed, livingBase.flapSpeed);
        return (Mth.sin(f) + 1.0F) * g;
    }

    // --

    public static @NotNull ResourceLocation getVariantTexture(Pigeon.Variant variant) {
        return switch (variant) {
            case GRAY -> GRAY;
            case BROWN -> BROWN;
            case WHITE -> WHITE;
        };
    }
}
