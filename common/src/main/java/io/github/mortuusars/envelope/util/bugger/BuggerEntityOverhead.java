package io.github.mortuusars.envelope.util.bugger;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.envelope.client.util.Minecrft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class BuggerEntityOverhead {
    private static final ArrayList<Component> LINES = new ArrayList<>();
    private static final ArrayList<EntityDataDisplay> DATA = new ArrayList<>();

    public static void addData(EntityDataDisplay data) {
        DATA.add(data);
    }

    public static <T extends Entity> void onRenderEntity(EntityRenderDispatcher dispatcher, T entity, float partialTick,
                                                         PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!BuggerDebugScreen.active()) return;
        if (dispatcher.distanceToSqr(entity) > 4096.0) return;

        @Nullable Vec3 vec3 = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (vec3 == null) return;

        List<Component> lines = getOverheadEntityInfoLines(entity);
        if (lines.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(vec3.x, vec3.y + 0.5, vec3.z);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(0.015F, -0.015F, 0.015F);
        Matrix4f matrix4f = poseStack.last().pose();
        float opacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (opacity * 255.0F) << 24;
        Font font = Minecrft.get().font;

        float y = -5 - lines.size() * (font.lineHeight + 1);
        for (Component text : lines) {
            float x = (float) (-font.width(text) / 2);
            font.drawInBatch(text, x, y, 0x20FFFFFF, false, matrix4f,
                  bufferSource, Font.DisplayMode.SEE_THROUGH, backgroundColor, packedLight);
            font.drawInBatch(text, x, y, -1, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
            y += font.lineHeight + 1;
        }

        poseStack.popPose();
    }

    private static List<Component> getOverheadEntityInfoLines(Entity entity) {
        LINES.clear();
        DATA.forEach(data -> data.addLines(entity, LINES));
        return LINES;
    }

    public interface EntityDataDisplay {
        void addLines(Entity entity, ArrayList<Component> lines);

        default Component line(String text) {
            return Component.literal(text);
        }
    }
}
