package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class MailAddressTooltipComponent implements ClientTooltipComponent {
    private static final ItemStack ADDRESS_TAG = new ItemStack(Envelope.Items.ADDRESS_TAG.get());

    private final @NotNull Component compactComponent;         // <address> > ️<address>
    private final @NotNull Component fullSenderComponent;      // Sender: ️<address>
    private final @NotNull Component fullRecipientComponent;   // Recipient: <address>

    public MailAddressTooltipComponent(@Nullable Address sender, @Nullable Address recipient) {
        compactComponent = AddressFormatter.senderToRecipient(sender, recipient);
        fullSenderComponent = AddressFormatter.fullSender(sender);
        fullRecipientComponent = AddressFormatter.fullRecipient(recipient);
    }

    @Override
    public int getWidth(Font font) {
        if (Screen.hasShiftDown()) {
            return Math.max(19, Math.max(font.width(fullSenderComponent), font.width(fullRecipientComponent)));
        }
        return 19 + font.width(compactComponent);
    }

    @Override
    public int getHeight() {
        if (Screen.hasShiftDown()) {
            return 16 + (!isEmpty(fullSenderComponent) ? 10 : 0) + (!isEmpty(fullRecipientComponent) ? 10 : 0);
        }
        return 16;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.renderFakeItem(ADDRESS_TAG, x - 1, y - 1, 0);
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        if (Screen.hasShiftDown()) {
            y += 16;

            if (!isEmpty(fullSenderComponent)) {
                drawText(font, fullSenderComponent, x, y, matrix, bufferSource);
                y += 10;
            }

            if (!isEmpty(fullRecipientComponent)) {
                drawText(font, fullRecipientComponent, x, y, matrix, bufferSource);
            }
        } else {
            drawText(font, compactComponent, x + 18, y + 3, matrix, bufferSource);
        }
    }

    private void drawText(Font font, Component component, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        font.drawInBatch(component, x, y, 0, true,
              matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
    }

    private boolean isEmpty(Component component) {
        return component.equals(CommonComponents.EMPTY);
    }
}
