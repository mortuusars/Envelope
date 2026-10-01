package io.github.mortuusars.envelope.client.renderer.entity;

import io.github.mortuusars.envelope.client.model.CourierBatModel;
import io.github.mortuusars.envelope.client.renderer.entity.layer.BatBackpackLayer;
import io.github.mortuusars.envelope.world.entity.CourierBat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class CourierBatRenderer extends MobRenderer<CourierBat, CourierBatModel> {
    private static final ResourceLocation BAT_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/bat.png");

    public CourierBatRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierBatModel(context.bakeLayer(ModelLayers.BAT)), 0.25F);
        addLayer(new BatBackpackLayer(this, context.bakeLayer(BatBackpackLayer.MODEL_LAYER)));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CourierBat entity) {
        return BAT_LOCATION;
    }
}
