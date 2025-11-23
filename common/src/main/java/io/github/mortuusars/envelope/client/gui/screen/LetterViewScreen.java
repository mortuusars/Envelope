package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.LetterViewScreenClosedS2CP;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
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
    public static final ResourceLocation TATTERED_TEXTURE = Envelope.resource("textures/gui/tattered_letter.png");
    public static final ResourceLocation TATTERED_OVERLAY = Envelope.resource("textures/gui/tattered_letter_overlay.png");
    public static final ResourceLocation SEALED_TEXTURE = Envelope.resource("textures/gui/sealed_letter.png");

    protected final ItemAndStack<LetterItem> letter;
    protected final InteractionHand hand;

    protected final Style style;

    protected int maxLines = 19;
    protected int imageWidth, imageHeight, leftPos, topPos;
    protected List<FormattedCharSequence> lines;

    public LetterViewScreen(ItemStack letter, InteractionHand hand) {
        super(Component.empty());
        this.letter = new ItemAndStack<>(letter);
        this.hand = hand;
        this.style = Style.of(letter);
    }

    @Override
    public boolean isPauseScreen() {
        return Config.Server.LETTER_PAUSE.get();
    }

    @Override
    protected void init() {
        imageWidth = 196;
        imageHeight = 244;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        createLines(letter.getOrDefault(Envelope.DataComponents.LETTER_CONTENT, LetterContent.EMPTY).text());
    }

    protected void createLines(Component text) {
        lines = font.split(text, 160);
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines); // Modifiable
            int lastLineIndex = maxLines - 1;
            FormattedCharSequence ellipsis = CommonComponents.ELLIPSIS.getVisualOrderText();
            lines.set(lastLineIndex, FormattedCharSequence.composite(lines.get(lastLineIndex), ellipsis));
            lines.set(lastLineIndex + 1, FormattedCharSequence.composite(ellipsis, lines.get(lastLineIndex + 1)));
        }
    }

    // -- Render

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            guiGraphics.drawString(font, lines.get(i), leftPos + 18, topPos + 22 + i * font.lineHeight, 0xFF7B593D, false);
        }

        @Nullable ResourceLocation overlay = style.getOverlay();
        if (overlay != null) {
            RenderSystem.enableBlend();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            guiGraphics.blit(overlay, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }

        if (lines.size() > maxLines
              && mouseX >= leftPos + 18 && mouseX < leftPos + 18 + 160
              && mouseY >= topPos + 22 && mouseY < topPos + 22 + 192) {
            List<FormattedCharSequence> leftovers = lines.stream().skip(maxLines).toList();
            guiGraphics.renderTooltip(font, leftovers, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(style.getTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void onClose() {
        super.onClose();
        int slot = this.hand == InteractionHand.MAIN_HAND ? Minecrft.player().getInventory().selected : Inventory.SLOT_OFFHAND;
        Packets.sendToServer(new LetterViewScreenClosedS2CP(slot));
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

    public enum Style {
        REGULAR(REGULAR_TEXTURE, null),
        TATTERED(TATTERED_TEXTURE, TATTERED_OVERLAY),
        SEALED(SEALED_TEXTURE, null);

        private final ResourceLocation texture;
        private final @Nullable ResourceLocation overlay;

        Style(ResourceLocation texture, @Nullable ResourceLocation overlay) {
            this.texture = texture;
            this.overlay = overlay;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public @Nullable ResourceLocation getOverlay() {
            return overlay;
        }

        public static Style of(ItemStack stack) {
            if (stack.is(Envelope.Items.TATTERED_LETTER.get())) return TATTERED;
            if (stack.is(Envelope.Items.SEALED_LETTER.get())) return SEALED;
            return REGULAR;
        }
    }
}
