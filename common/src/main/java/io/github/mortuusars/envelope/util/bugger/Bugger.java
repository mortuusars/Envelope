package io.github.mortuusars.envelope.util.bugger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.mixin.bugger.ScreenRenderLinesInvoker;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Bugger {
    public static int page = -1;

    private static int zoom;
    private static int scroll;

    public static boolean onKeyPress(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_UP) up();
        if (key == InputConstants.KEY_DOWN) down();
        if (key == InputConstants.KEY_INSERT) zoom = 0;
        if (key == InputConstants.KEY_HOME) scroll = 0;
        if (key == InputConstants.KEY_LEFT) page = Mth.clamp(page - 1, -1, 1);
        if (key == InputConstants.KEY_RIGHT) page = Mth.clamp(page + 1, -1, 1);

        if (key == InputConstants.KEY_END) test();
        return false;
    }

    public static boolean onKeyRepeat(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_UP) up();
        if (key == InputConstants.KEY_DOWN) down();
        if (key == InputConstants.KEY_LEFT) page = Mth.clamp(page - 1, -1, 1);
        if (key == InputConstants.KEY_RIGHT) page = Mth.clamp(page + 1, -1, 1);
        return false;
    }

    public static boolean onKeyRelease(int key, int scanCode, int modifiers) {
        return false;
    }

    private static void test() {
    }

    // --

    private static void up() {
        if (Screen.hasControlDown()) {
            boolean shift = Screen.hasShiftDown();
            zoom = shift ? zoom + 5 : zoom + 1;
        } else {
            boolean shift = Screen.hasShiftDown();
            scroll = Math.max(shift ? scroll - 5 : scroll - 1, 0);
        }
    }

    private static void down() {
        if (Screen.hasControlDown()) {
            boolean shift = Screen.hasShiftDown();
            zoom = shift ? zoom - 5 : zoom - 1;
        } else {
            boolean shift = Screen.hasShiftDown();
            scroll = Math.max(shift ? scroll + 5 : scroll + 1, 0);
        }
    }

    public static void renderMainPage(GuiGraphics guiGraphics) {
        float scale = (zoom + 100) / 100f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);
        List<String> leftLines = collectLeftLines().stream().skip(scroll).toList();
        ((ScreenRenderLinesInvoker) Minecraft.getInstance().getDebugOverlay()).drawLines(guiGraphics, leftLines, true);
        List<String> rightLines = collectRightLines().stream().skip(scroll).toList();
        ((ScreenRenderLinesInvoker) Minecraft.getInstance().getDebugOverlay()).drawLines(guiGraphics, rightLines, false);
        guiGraphics.pose().popPose();
    }

    private static List<String> collectLeftLines() {
        List<String> lines = new ArrayList<>();

        return lines;
    }

    private static List<String> collectRightLines() {
        List<String> lines = new ArrayList<>();

        return lines;
    }

    private static ItemStack getItemInHand() {
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        return mainHandItem.isEmpty() ? player.getItemInHand(InteractionHand.OFF_HAND) : mainHandItem;
    }

    public static List<String> splitString(String text, int size) {
        List<String> ret = new ArrayList<>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

    public static void renderTagPage(GuiGraphics guiGraphics) {
        List<String> tagLines = getTagPageLines();

        int maxScroll = Math.max(tagLines.size() - 8, 0);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        List<String> lines = tagLines.stream().skip(scroll).toList();

        float scale = (zoom + 100) / 100f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, scale);
        ((ScreenRenderLinesInvoker) Minecrft.get().getDebugOverlay()).drawLines(guiGraphics, lines, true);
        guiGraphics.pose().popPose();
    }

    private static @NotNull List<String> getTagPageLines() {
        @Nullable HitResult hitResult = Minecrft.get().hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            ItemStack itemInHand = getItemInHand();

            JsonElement json = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemInHand).result().orElse(new JsonObject());
            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

            jsonString = JsonSyntaxHighlighter.highlight(jsonString);

            List<String> lines = new ArrayList<>(Arrays.stream(jsonString.split("\n")).toList());
            lines.addFirst("");
            lines.addFirst(itemInHand.getHoverName().getString());
            return lines;
        } else if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            @Nullable BlockEntity blockEntity = Minecrft.level().getBlockEntity(blockPos);
            if (blockEntity != null) {
                CompoundTag beTag = blockEntity.saveWithFullMetadata(Minecrft.level().registryAccess());
                JsonElement json = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, beTag).result().orElse(new JsonObject());

                String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

                jsonString = JsonSyntaxHighlighter.highlight(jsonString);

                List<String> lines = new ArrayList<>(Arrays.stream(jsonString.split("\n")).toList());
                lines.addFirst("");
                lines.addFirst(blockEntity.getBlockState().getBlock().getName().getString());
                return lines;
            } else {
                return List.of(Minecrft.level().getBlockState(blockPos).getBlock().getName().getString());
            }
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();

            CompoundTag entityTag = new CompoundTag();
            entity.save(entityTag);

            JsonElement json = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, entityTag).result().orElse(new JsonObject());

            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

            jsonString = JsonSyntaxHighlighter.highlight(jsonString);

            List<String> lines = new ArrayList<>(Arrays.stream(jsonString.split("\n")).toList());
            lines.addFirst("");
            lines.addFirst(entity.getName().getString());
            return lines;
        }

        return Collections.emptyList();
    }

    public static boolean shouldShowOverheadEntityInfo(Entity entity) {
        return PlatformHelper.isInDevEnv() && Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen();
    }

    public static void renderOverheadEntityInfo(EntityRenderDispatcher dispatcher, Entity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (dispatcher.distanceToSqr(entity) > 4096.0) {
            return;
        }

        @Nullable Vec3 vec3 = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (vec3 == null) {
            return;
        }

        List<Component> lines = getOverheadEntityInfoLines(entity);

        if (lines.isEmpty()) {
            return;
        }

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
        if (entity instanceof Pigeon pigeon) {
            if (pigeon.delivery() != null) {
                return List.of(
                      Component.empty()
                            .append(pigeon.delivery().getSenderAddress().getDisplayName())
                            .append(" → ")
                            .append(pigeon.delivery().getRecipientAddress().getDisplayName()),
                      Component.literal(WordUtils.capitalize(pigeon.delivery().getPhase().getType().getSerializedName().replace('_', ' '))));
            }

            List<Component> list = new ArrayList<>();
            PigeonholeHandler handler = pigeon.getPigeonholeHandler();
            if (handler.wantsToEnterPigeonhole()) {
                list.add(Component.literal(handler.getPigeonholePos() != null ? "Going to Pigeonhole" : "Looking for Pigeonhole"));
            } else {
                list.add(Component.literal("Would want to enter after: " + handler.getCooldownBeforeWantingToEnterPigeonhole() / 20));
                if (handler.getCooldownBeforeEnteringPigeonhole() > 0) {
                    list.add(Component.literal("Could enter after: " + handler.getCooldownBeforeEnteringPigeonhole() / 20));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
