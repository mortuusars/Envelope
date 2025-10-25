package io.github.mortuusars.envelope.mixin.bugger;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.envelope.util.bugger.BuggerGui;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow @Final protected EntityRenderDispatcher entityRenderDispatcher;

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        BuggerGui.onRenderEntity(entityRenderDispatcher, entity, partialTick, poseStack, bufferSource, packedLight);
    }
}
