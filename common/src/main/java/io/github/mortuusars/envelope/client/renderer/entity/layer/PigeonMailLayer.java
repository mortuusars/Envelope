package io.github.mortuusars.envelope.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.model.PigeonModel;
import io.github.mortuusars.envelope.client.renderer.entity.PigeonRenderer;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PigeonMailLayer extends RenderLayer<Pigeon, PigeonModel> {
    protected final ItemRenderer itemRenderer;

    public PigeonMailLayer(PigeonRenderer pigeonRenderer, ItemRenderer itemRenderer) {
        super(pigeonRenderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Pigeon pigeon,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
//        if (pigeon.getDelivery().getMail().isEmpty()) return;
//        ItemStack mail = pigeon.getDelivery().getMail();
//        mail = new ItemStack(Envelope.Items.LETTER.get());

//        poseStack.pushPose();
//        getParentModel().body.translateAndRotate(poseStack);
//        poseStack.scale(0.5f, 0.5f, 0.5f);
//        poseStack.mulPose(Axis.XP.rotationDegrees(180));
//        poseStack.mulPose(Axis.YN.rotationDegrees((Minecrft.level().getGameTime() + partialTick) % 360 * 8));
//        poseStack.translate(0, 3, 0);
//        itemRenderer.renderStatic(pigeon, mail, ItemDisplayContext.FIXED, false, poseStack,
//                bufferSource, pigeon.level(), packedLight, LivingEntityRenderer.getOverlayCoords(pigeon, 0.0F), pigeon.getId());
//        poseStack.popPose();
    }
}
