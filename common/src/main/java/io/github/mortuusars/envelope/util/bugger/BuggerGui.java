package io.github.mortuusars.envelope.util.bugger;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.mixin.bugger.ScreenRenderLinesInvoker;
import io.github.mortuusars.envelope.util.bugger.page.BuggerPage;
import io.github.mortuusars.envelope.util.bugger.page.DataPage;
import io.github.mortuusars.envelope.util.bugger.page.VanillaDebugPage;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonholeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BuggerGui {
    private static final List<BuggerPage> pages = new ArrayList<>();
    private static int currentPage;
    private static int zoom;
    private static int scroll;

    static {
        pages.add(new VanillaDebugPage());
        pages.add(new DataPage());
    }

    public static boolean isActive() {
        return Bugger.isEnabled() && Minecrft.get().gui.getDebugOverlay().showDebugScreen();
    }

    public static void addPage(BuggerPage page) {
        pages.add(page);
    }

    public static BuggerPage getCurrentPage() {
        currentPage = Mth.clamp(currentPage, 0, pages.size() - 1);
        return pages.get(currentPage);
    }

    public static Optional<BuggerPage> getPreviousPage() {
        int page = currentPage - 1;
        return page >= 0 && page < pages.size()
              ? Optional.ofNullable(pages.get(page))
              : Optional.empty();
    }

    public static Optional<BuggerPage> getNextPage() {
        int page = currentPage + 1;
        return page >= 0 && page < pages.size()
              ? Optional.ofNullable(pages.get(page))
              : Optional.empty();
    }

    public static boolean render(GuiGraphics guiGraphics) {
        if (!isActive()) return false;

        Minecraft.getInstance().getProfiler().push("bugger");

        float scale = (zoom + 100) / 100f;

        //noinspection deprecation
        guiGraphics.drawManaged(() -> {
            drawPageTabs(guiGraphics);

            if (currentPage > 0) {
                BuggerPage page = getCurrentPage();

                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(scale, scale, scale);

                int correctedScroll;

                List<String> leftLines = page.getLeftLines();
                int maxScroll = Math.max(leftLines.size() - 8, 0);
                int effectiveScroll = Mth.clamp(scroll, 0, maxScroll);
                correctedScroll = Math.min(scroll, effectiveScroll);
                leftLines = leftLines.stream().skip(effectiveScroll).toList();
                drawLines(guiGraphics, leftLines, true);

                guiGraphics.pose().popPose();

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(Minecrft.get().getWindow().getGuiScaledWidth(), 0, 0);
                guiGraphics.pose().scale(scale, scale, scale);
                guiGraphics.pose().translate(Minecrft.get().getWindow().getGuiScaledWidth(), 0, 0);

                List<String> rightLines = page.getRightLines();
                maxScroll = Math.max(rightLines.size() - 8, 0);
                effectiveScroll = Mth.clamp(scroll, 0, maxScroll);
                correctedScroll = Math.max(correctedScroll, effectiveScroll);
                rightLines = rightLines.stream().skip(effectiveScroll).toList();
                drawLines(guiGraphics, rightLines, false);
                guiGraphics.pose().popPose();

                scroll = correctedScroll;
            }
        });

        getCurrentPage().render(guiGraphics, Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true), scale);

        Minecraft.getInstance().getProfiler().pop();
        return currentPage > 0;
    }

    private static void drawPageTabs(GuiGraphics guiGraphics) {
        int titleBgColor = 0x90505050;
        int sideBgColor = 0x50505050;
        int titleFontColor = 0xFFFFFFFF;
        int sideFontColor = 0xFFEEEEEE;

        String prevTitle = getPreviousPage().map(buggerPage -> "‹ " + buggerPage.getTitle()).orElse("");
        String title = getCurrentPage().getTitle();
        String nextTitle = getNextPage().map(buggerPage -> buggerPage.getTitle() + " ›").orElse("");

        int titleWidth = Minecraft.getInstance().font.width(title);
        int titleX = guiGraphics.guiWidth() / 2 - titleWidth / 2;
        guiGraphics.fill(titleX - 4, 1, titleX + titleWidth + 4, 10, titleBgColor);
        guiGraphics.drawString(Minecraft.getInstance().font, title, titleX, 2, titleFontColor, false);

        if (!prevTitle.isEmpty()) {
            int prevWidth = Minecraft.getInstance().font.width(prevTitle);
            int prevX = titleX - prevWidth - 7;
            guiGraphics.fill(prevX - 1, 1, prevX + prevWidth + 1, 10, sideBgColor);
            guiGraphics.drawString(Minecraft.getInstance().font, prevTitle, prevX, 2, sideFontColor, false);
        }

        if (!nextTitle.isEmpty()) {
            int nextWidth = Minecraft.getInstance().font.width(nextTitle);
            int nextX = titleX + titleWidth + 7;
            guiGraphics.fill(nextX - 1, 1, nextX + nextWidth + 1, 10, sideBgColor);
            guiGraphics.drawString(Minecraft.getInstance().font, nextTitle, nextX, 2, sideFontColor, false);
        }
    }

    private static void drawLines(GuiGraphics guiGraphics, List<String> lines, boolean leftSide) {
        ((ScreenRenderLinesInvoker) Minecraft.getInstance().getDebugOverlay()).drawLines(guiGraphics, lines, leftSide);
    }

//    public static List<String> splitString(String text, int size) {
//        List<String> ret = new ArrayList<>((text.length() + size - 1) / size);
//
//        for (int start = 0; start < text.length(); start += size) {
//            ret.add(text.substring(start, Math.min(text.length(), start + size)));
//        }
//        return ret;
//    }

    public static <T extends Entity> void onRenderEntity(EntityRenderDispatcher dispatcher, T entity, float partialTick,
                                                         PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!BuggerGui.isActive()) return;
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
        if (entity instanceof Pigeon pigeon) {
            if (pigeon.getDelivery().isPresent()) {
                Delivery delivery = pigeon.getDelivery().get();
                return List.of(
                      Component.empty()
                            .append(delivery.getSenderAddress().getDisplayName())
                            .append(" → ")
                            .append(delivery.getRecipientAddress().getDisplayName()),
                      Component.literal(WordUtils.capitalize(delivery.getPhase().getType()
                            .getSerializedName().replace('_', ' '))));
            }

            List<Component> list = new ArrayList<>();
            PigeonholeHandler handler = pigeon.getPigeonholeHandler();
            if (handler.wantsToEnterPigeonhole(entity.level())) {
                list.add(Component.literal(handler.getCurrentPos() != null ? "Going to Pigeonhole" : "Looking for Pigeonhole"));
            } else {
                list.add(Component.literal("Would want to enter after: " + handler.getWantCooldown() / 20));
                if (handler.getEnterCooldown() > 0) {
                    list.add(Component.literal("Could enter after: " + handler.getEnterCooldown() / 20));
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    // -- Input

    public static boolean onMousePress(int button, int action, int modifiers) {
        if (!isActive()) return false;
        if (Minecraft.getInstance().screen != null) return false;
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE && Screen.hasShiftDown()) {
            scroll = 0;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE && Screen.hasControlDown()) {
            zoom = 0;
            return true;
        }
        return getCurrentPage().onMousePress(button, action, modifiers);
    }

    public static boolean onMouseScroll(double amount) {
        if (!isActive()) return false;
        if (Minecraft.getInstance().screen != null) return false;

        if (Screen.hasShiftDown()) {
            scroll = Mth.clamp(scroll - (int) amount * 3, 0, 1000);
            return true;
        }

        if (Screen.hasControlDown()) {
            zoom = Mth.clamp(zoom + (int) amount * 5, -50, 100);
            return true;
        }

        return getCurrentPage().onMouseScroll(amount);
    }

    public static boolean onKeyAction(int action, int key, int scanCode, int modifiers) {
        if (!isActive()) return false;
        if (Minecraft.getInstance().screen != null) return false;
        return action == InputConstants.PRESS && BuggerGui.onKeyPress(key, scanCode, modifiers)
              || action == InputConstants.REPEAT && BuggerGui.onKeyRepeat(key, scanCode, modifiers)
              || action == InputConstants.RELEASE && BuggerGui.onKeyRelease(key, scanCode, modifiers);
    }

    public static boolean onKeyPress(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_LEFT) {
            prevPage();
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            nextPage();
            return true;
        }
        if (key == InputConstants.KEY_END && Screen.hasControlDown()) {
            test();
            return true;
        }
        return getCurrentPage().onKeyPress(key, scanCode, modifiers);
    }

    private static void test() {
    }

    public static boolean onKeyRepeat(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_LEFT) {
            prevPage();
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            nextPage();
            return true;
        }
        return getCurrentPage().onKeyRepeat(key, scanCode, modifiers);
    }

    public static boolean onKeyRelease(int key, int scanCode, int modifiers) {
        return getCurrentPage().onKeyRelease(key, scanCode, modifiers);
    }

    // --

    public static void prevPage() {
        changePage(currentPage - 1);
    }

    public static void nextPage() {
        changePage(currentPage + 1);
    }

    public static void changePage(int index) {
        int newPage = Mth.clamp(index, -1, pages.size() - 1);
        if (currentPage != newPage) {
            currentPage = newPage;
            scroll = 0;
            zoom = 0;
        }
    }
}
