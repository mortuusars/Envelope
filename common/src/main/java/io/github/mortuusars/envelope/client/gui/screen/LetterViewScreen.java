package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.LetterViewScreenClosedS2CP;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LetterViewScreen extends Screen {
    public static final ResourceLocation REGULAR_TEXTURE = Envelope.resource("textures/gui/letter.png");
    public static final ResourceLocation TATTERED_TEXTURE = Envelope.resource("textures/gui/letter_tattered.png");
    public static final ResourceLocation TATTERED_OVERLAY = Envelope.resource("textures/gui/letter_tattered_overlay.png");

    protected final ItemAndStack<LetterItem> letter;
    protected final @Nullable InteractionHand hand;
    protected final boolean isTattered;

    protected int imageWidth, imageHeight, leftPos, topPos;
    protected int maxTextWidth;
    protected int maxTextHeight;
    protected int maxTextLines;

    protected List<FormattedCharSequence> lines;

    public LetterViewScreen(ItemStack letter, @Nullable InteractionHand hand) {
        super(Component.empty());
        this.letter = new ItemAndStack<>(letter);
        this.hand = hand;
        this.isTattered = letter.has(Envelope.DataComponents.LETTER_TATTERED);
    }

    @Override
    public boolean isPauseScreen() {
        return Config.Server.LETTER_PAUSE.get();
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 192;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        maxTextWidth = 142;
        maxTextHeight = 144;
        maxTextLines = maxTextHeight / font.lineHeight;
        createLines(letter.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY).text());
    }

    protected void createLines(Component text) {
        lines = font.split(text, maxTextWidth);
        if (lines.size() > maxTextLines) {
            lines = new ArrayList<>(lines);
            int lastLineIndex = maxTextLines - 1;
            FormattedCharSequence ellipsis = CommonComponents.ELLIPSIS.getVisualOrderText();
            lines.set(lastLineIndex, FormattedCharSequence.composite(lines.get(lastLineIndex), ellipsis));
            lines.set(lastLineIndex + 1, FormattedCharSequence.composite(ellipsis, lines.get(lastLineIndex + 1)));
        }
    }

    // -- Render

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        final int x = leftPos + 17;
        final int y = topPos + 21;
        int textColor = 0xFF7B593D;

        for (int i = 0; i < Math.min(lines.size(), maxTextLines); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * font.lineHeight, textColor, false);
        }

        if (isTattered) {
            RenderSystem.enableBlend();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            guiGraphics.blit(TATTERED_OVERLAY, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }

        @Nullable Style style = getComponentStyleAt(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            guiGraphics.renderComponentHoverEffect(font, style, mouseX, mouseY);
        } else {
            if (lines.size() > maxTextLines
                  && mouseX >= x && mouseX < x + maxTextWidth
                  && mouseY >= y && mouseY < y + maxTextHeight) {
                List<FormattedCharSequence> leftovers = lines.stream().skip(maxTextLines).toList();
                guiGraphics.renderTooltip(font, leftovers, mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        ResourceLocation texture = isTattered ? TATTERED_TEXTURE : REGULAR_TEXTURE;
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Nullable
    public Style getComponentStyleAt(double mouseX, double mouseY) {
        if (lines.isEmpty()
              || mouseX < leftPos + 17 || mouseX >= leftPos + 17 + maxTextWidth
              || mouseY < topPos + 21 || mouseY >= topPos + 21 + maxTextHeight) {
            return null;
        }

        int x = (int) mouseX - (leftPos + 17);
        int y = (int) mouseY - (topPos + 21);

        int linesCount = Math.min(lines.size(), maxTextLines);
        if (y < font.lineHeight * linesCount + linesCount) {
            int clickedLine = y / font.lineHeight;
            if (clickedLine >= 0 && clickedLine < lines.size()) {
                FormattedCharSequence text = lines.get(clickedLine);
                return font.getSplitter().componentStyleAtWidth(text, x);
            }

            return null;
        }

        return null;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (hand != null) {
            int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
            Packets.sendToServer(new LetterViewScreenClosedS2CP(slot));
        }
    }

    // -- Input

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            @Nullable Style style = getComponentStyleAt(x, y);
            if (handleComponentClicked(style)) {
                return true;
            }
        }

        return super.mouseClicked(x, y, button);
    }

    public boolean handleComponentClicked(@Nullable Style style) {
        if (style == null) {
            return false;
        }

        @Nullable ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return false;
        }

        boolean handled = super.handleComponentClicked(style);
        if (handled && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
            onClose();
        }

        return handled;
    }
}
