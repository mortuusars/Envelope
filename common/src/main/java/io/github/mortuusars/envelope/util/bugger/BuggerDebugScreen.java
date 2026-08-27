package io.github.mortuusars.envelope.util.bugger;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.util.bugger.page.BuggerPage;
import io.github.mortuusars.envelope.util.bugger.page.DataPage;
import io.github.mortuusars.envelope.util.bugger.page.LogPage;
import io.github.mortuusars.envelope.util.bugger.page.VanillaDebugPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extension of vanilla debug (F3) screen. Allows displaying different pages. Vanilla page is not changed.
 */
public class BuggerDebugScreen {
    private static final List<BuggerPage> pages = new ArrayList<>();
    private static int currentPageIndex;
    private static BuggerPage currentPage;
    private static int zoom = 0;
    private static int scroll;

    static {
        pages.add(new LogPage());
        pages.add(new VanillaDebugPage());
        pages.add(new DataPage());

        // Set to vanilla debug page
        currentPageIndex = 1;
        currentPage = pages.get(1);
        currentPage.activated();
    }

    public static boolean active() {
        return Bugger.isEnabled() && Minecrft.get().gui.getDebugOverlay().showDebugScreen();
    }

    public static boolean isOnVanillaDebugPage() {
        return currentPage instanceof VanillaDebugPage;
    }

    public static void addPage(BuggerPage page) {
        pages.add(page);
    }

    public static void prevPage() {
        setPage(currentPageIndex - 1);
    }

    public static void nextPage() {
        setPage(currentPageIndex + 1);
    }

    public static void setPage(int index) {
        int newPage = Mth.clamp(index, 0, pages.size() - 1);
        if (currentPageIndex != newPage) {
            currentPage.deactivated();

            currentPageIndex = newPage;
            currentPage = pages.get(currentPageIndex);
            currentPage.activated();
            scroll = 0;
        }
    }

    public static Optional<BuggerPage> getPreviousPage() {
        int page = currentPageIndex - 1;
        return page >= 0 && page < pages.size()
              ? Optional.of(pages.get(page))
              : Optional.empty();
    }

    public static Optional<BuggerPage> getNextPage() {
        int page = currentPageIndex + 1;
        return page >= 0 && page < pages.size()
              ? Optional.of(pages.get(page))
              : Optional.empty();
    }

    @SuppressWarnings("deprecation")
    public static boolean render(GuiGraphics guiGraphics) {
        if (!active()) return false;

        Minecraft.getInstance().getProfiler().push("bugger");

        if (zoom == 0) {
            zoom = ((int) Minecrft.get().getWindow().getGuiScale());
        }

        guiGraphics.drawManaged(() -> {
            drawPageTabs(guiGraphics);

            float scale = (float) (zoom / Minecrft.get().getWindow().getGuiScale());

            if (!isOnVanillaDebugPage()) {
                List<String> leftLines = currentPage.getLeftLines();
                List<String> rightLines = currentPage.getRightLines();

                int correctedScroll = scroll;
                int maxScroll = Math.max(leftLines.size() - 8, 0);
                int effectiveScroll = Mth.clamp(scroll, 0, maxScroll);

                if (!leftLines.isEmpty()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(scale, scale, scale);

                    correctedScroll = Math.min(scroll, effectiveScroll);
                    leftLines = leftLines.stream().skip(effectiveScroll).toList();
                    drawLines(guiGraphics, leftLines, true);

                    guiGraphics.pose().popPose();
                }

                if (!rightLines.isEmpty()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(Minecrft.get().getWindow().getGuiScaledWidth(), 0, 0);
                    guiGraphics.pose().scale(scale, scale, scale);
                    guiGraphics.pose().translate(-Minecrft.get().getWindow().getGuiScaledWidth(), 0, 0);

                    maxScroll = Math.max(rightLines.size() - 8, 0);
                    effectiveScroll = Mth.clamp(scroll, 0, maxScroll);
                    correctedScroll = Math.max(correctedScroll, effectiveScroll);
                    rightLines = rightLines.stream().skip(effectiveScroll).toList();
                    drawLines(guiGraphics, rightLines, false);
                    guiGraphics.pose().popPose();
                }

                scroll = correctedScroll;
            }

            currentPage.render(guiGraphics, Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true), scale);
        });


        Minecraft.getInstance().getProfiler().pop();
        return !isOnVanillaDebugPage();
    }

    private static void drawPageTabs(GuiGraphics guiGraphics) {
        int titleBgColor = 0x90505050;
        int sideBgColor = 0x50505050;
        int titleFontColor = 0xFFFFFFFF;
        int sideFontColor = 0xFFEEEEEE;

        String prevTitle = getPreviousPage().map(buggerPage -> "‹ " + buggerPage.getTitle()).orElse("");
        String title = currentPage.getTitle();
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
        Font font = Minecrft.get().font;
        int y = 2;

        for (String line : lines) {
            if (Strings.isNullOrEmpty(line)) {
                y += font.lineHeight;
                continue;
            }

            boolean splitted = false;

            for (FormattedCharSequence chars : font.split(FormattedText.of(line), Minecrft.get().getWindow().getWidth() / zoom - 4)) {
                int lineWidth = font.width(chars);
                int x = leftSide ? 2 : guiGraphics.guiWidth() - 2 - lineWidth;
                if (splitted) {
                    x += 4;
                }
                guiGraphics.fill(x - 1, y - 1, x + lineWidth + 1, y + font.lineHeight - 1, 0x90505050);
                guiGraphics.drawString(font, chars, x, y, 0xFFE0E0E0, false);
                y += font.lineHeight;
                splitted = true;
            }
        }
    }

    // -- Input

    public static boolean onMousePress(int button, int action, int modifiers) {
        if (!active()) return false;
        if (Minecraft.getInstance().screen != null) return false;
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE && Screen.hasShiftDown()) {
            scroll = 0;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE && Screen.hasControlDown()) {
            zoom = ((int) Minecrft.get().getWindow().getGuiScale());
            return true;
        }
        return currentPage.onMousePress(button, action, modifiers);
    }

    public static boolean onMouseScroll(double amount) {
        if (!active()) return false;
        if (Minecraft.getInstance().screen != null) return false;

        if (Screen.hasShiftDown()) {
            scroll = Math.max(scroll - (int) amount * 3, 0);
            return true;
        }

        if (Screen.hasControlDown()) {
            if (amount > 0)
                zoomIn();
            else
                zoomOut();
            return true;
        }

        return currentPage.onMouseScroll(amount);
    }

    private static void zoomIn() {
        zoom = Mth.clamp(zoom + 1, 1, 8);
    }

    private static void zoomOut() {
        zoom = Mth.clamp(zoom - 1, 1, 8);
    }

    public static boolean onKeyAction(int action, int key, int scanCode, int modifiers) {
        if (!active()) return false;
        if (Minecraft.getInstance().screen != null) return false;
        return action == InputConstants.PRESS && BuggerDebugScreen.onKeyPress(key, scanCode, modifiers)
              || action == InputConstants.REPEAT && BuggerDebugScreen.onKeyRepeat(key, scanCode, modifiers)
              || action == InputConstants.RELEASE && BuggerDebugScreen.onKeyRelease(key, scanCode, modifiers);
    }

    public static boolean onKeyPress(int key, int scanCode, int modifiers) {
        if (currentPage.onKeyPress(key, scanCode, modifiers)) {
            return true;
        }
        if (key == InputConstants.KEY_LEFT) {
            prevPage();
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            nextPage();
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            scroll = Math.max(scroll + 3, 0);
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            scroll = Math.max(scroll - 3, 0);
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            scroll = 0;
            return true;
        }
        if (key == InputConstants.KEY_END) {
            scroll = Integer.MAX_VALUE;
            return true;
        }
        if (key == InputConstants.KEY_INSERT && Screen.hasControlDown()) {
            test();
            return true;
        }
        if ((key == InputConstants.KEY_ADD || key == InputConstants.KEY_EQUALS) && Screen.hasControlDown()) {
            zoomIn();
            return true;
        }
        if ((key == 333 /*KEY_SUBTRACT*/ || key == InputConstants.KEY_MINUS) && Screen.hasControlDown()) {
            zoomOut();
            return true;
        }
        return false;
    }

    public static boolean onKeyRepeat(int key, int scanCode, int modifiers) {
        if (currentPage.onKeyRepeat(key, scanCode, modifiers)) {
            return true;
        }
        if (key == InputConstants.KEY_LEFT) {
            prevPage();
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            nextPage();
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            scroll = Math.max(scroll + 3, 0);
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            scroll = Math.max(scroll - 3, 0);
            return true;
        }
        if ((key == InputConstants.KEY_ADD || key == InputConstants.KEY_EQUALS) && Screen.hasControlDown()) {
            zoomIn();
            return true;
        }
        if ((key == 333 /*KEY_SUBTRACT*/ || key == InputConstants.KEY_MINUS) && Screen.hasControlDown()) {
            zoomOut();
            return true;
        }
        return false;
    }

    public static boolean onKeyRelease(int key, int scanCode, int modifiers) {
        return currentPage.onKeyRelease(key, scanCode, modifiers);
    }

    private static void test() {
        boolean a = true;
    }
}
