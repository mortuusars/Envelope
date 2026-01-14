package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.item.component.NewMail;
import io.github.mortuusars.envelope.world.item.component.mail.MailDeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.MailDeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.MailboxMenuInboxActionC2SP;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MailboxScreen extends AbstractContainerScreen<MailboxMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/mailbox.png");

    public static final WidgetSprites ADDRESS_BUTTON_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/address_button"));
    public static final WidgetSprites ADDRESS_ATTENTION_BUTTON_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/address_attention_button"), Envelope.resource("mailbox/address_button_highlighted"));
    public static final WidgetSprites ADDRESS_DEFAULT_BUTTON_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/address_default_button"));

    public static final WidgetSprites REGULAR_MAIL_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("mailbox/mail_button"));

    public static final WidgetSprites ICON_ADDRESS_BLOCK_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_mailbox"));
    public static final WidgetSprites ICON_ADDRESS_PLAYER_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_player"));
    public static final WidgetSprites ICON_ADDRESS_ENTITY_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_npc"));
    public static final WidgetSprites ICON_ADDRESS_MAIL_SERVICE_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_mail_service"));
    public static final WidgetSprites ICON_ADDRESS_UNKNOWN_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_unknown"));
    public static final WidgetSprites ICON_RETURNED_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_returned"));
    public static final WidgetSprites ICON_REJECTED_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_rejected"));
    public static final WidgetSprites ICON_UNCLAIMED_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("mailbox/icon_unclaimed"));

    public static final WidgetSprites NEW_MAIL_INDICATOR_SPRITES = Sprites.normalOnly(Envelope.resource("mailbox/new_mail_indicator"));

    protected static final int SCROLL_THUMB_TOP_HEIGHT = 3;
    protected static final int SCROLL_THUMB_MID_HEIGHT = 4;
    protected static final int SCROLL_THUMB_BOT_HEIGHT = 2;
    protected static final int SCROLL_THUMB_HEIGHT = SCROLL_THUMB_TOP_HEIGHT + SCROLL_THUMB_MID_HEIGHT + SCROLL_THUMB_BOT_HEIGHT;

    protected static final int MAX_INBOX_MAIL_BUTTONS = 9;

    protected Component inboxLabel = Component.translatable("gui.envelope.mailbox.inbox");
    protected Component sendLabel = Component.translatable("gui.envelope.mailbox.send");

    @Nullable
    protected ItemStack hoveredMail;

    protected ImageButton addressButton;
    protected ImageButton addressAttentionButton;
    protected ImageButton addressDefaultButton;
    protected ImageButton newMailButton;

    protected Rect2i mailArea = new Rect2i(0, 0, 0, 0);
    protected Rect2i scrollBarArea = new Rect2i(0, 0, 0, 0);
    protected Rect2i scrollThumb = new Rect2i(0, 0, 0, 0);
    protected int scroll = 0;
    protected int scrollAtDragStart = 0;
    protected boolean isDraggingScrollbar = false;
    protected double dragDelta = 0;

    public MailboxScreen(MailboxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // --

    @Override
    protected void init() {
        imageWidth = 308;
        imageHeight = 203;
        titleLabelX = Math.max(17, (imageWidth / 2) - (font.width(title) / 2) + 5);
        titleLabelY = 5;
        inventoryLabelX = 140;
        inventoryLabelY = imageHeight - 94;
        super.init();
        mailArea = new Rect2i(leftPos + 8, topPos + 32, 117, 162);
        scrollBarArea = new Rect2i(leftPos + 128, topPos + 33, 6, 161);

        addressButton = new ImageButton(leftPos + titleLabelX - 11, topPos + 4, 10, 10,
              ADDRESS_BUTTON_SPRITES,
              button -> setAsDefaultAddress(),
              Component.translatable("gui.envelope.mailbox.address"));
        addressButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.mailbox.address")
              .append("\n")
              .append(Component.translatable("gui.envelope.mailbox.address.tooltip"))));
        addRenderableWidget(addressButton);

        addressAttentionButton = new ImageButton(leftPos + titleLabelX - 11, topPos + 4, 10, 10,
              ADDRESS_ATTENTION_BUTTON_SPRITES,
              button -> setAsDefaultAddress(),
              Component.translatable("gui.envelope.mailbox.address"));
        addressAttentionButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.mailbox.address")
              .append("\n")
              .append(Component.translatable("gui.envelope.mailbox.address.tooltip"))));
        addRenderableWidget(addressAttentionButton);

        addressDefaultButton = new ImageButton(leftPos + titleLabelX - 11, topPos + 4, 10, 10, ADDRESS_DEFAULT_BUTTON_SPRITES, btn -> {
        });
        addressDefaultButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.mailbox.address.default")
              .append("\n")
              .append(Component.translatable("gui.envelope.mailbox.address.default.tooltip"))));
        addressDefaultButton.active = false;
        addRenderableWidget(addressDefaultButton);

        newMailButton = new ImageButton(leftPos + 7, topPos + 21, 8, 8,
              NEW_MAIL_INDICATOR_SPRITES,
              button -> {
                  refreshMail();
                  scrollTo(0);
              });
        newMailButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.mailbox.mail.tooltip.new_mail")
              .append("\n")
              .append(Component.translatable("gui.envelope.mailbox.mail.tooltip.new_mail.click_to_refresh"))));
        addRenderableWidget(newMailButton);

        updateButtons();
    }

    protected void setAsDefaultAddress() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, MailboxMenu.ADDRESS_BUTTON_ID);
    }

    protected void refreshMail() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, MailboxMenu.REFRESH_MAIL_BUTTON_ID);
    }

    protected void updateScrollThumb() {
        int minSize = SCROLL_THUMB_TOP_HEIGHT + SCROLL_THUMB_MID_HEIGHT + SCROLL_THUMB_BOT_HEIGHT;

        int totalButtons = getMenu().getMail().size();
        float ratio = MAX_INBOX_MAIL_BUTTONS / (float) Math.max(totalButtons, 1);
        int size = Mth.clamp(Mth.ceil(scrollBarArea.getHeight() * ratio), minSize, scrollBarArea.getHeight());
        int midSize = size - SCROLL_THUMB_TOP_HEIGHT - SCROLL_THUMB_BOT_HEIGHT;
        int correctedMidSize = Math.max(midSize - (midSize % SCROLL_THUMB_MID_HEIGHT), SCROLL_THUMB_MID_HEIGHT);
        size = SCROLL_THUMB_TOP_HEIGHT + correctedMidSize + SCROLL_THUMB_BOT_HEIGHT;

        float topRowPos = (float) scroll / Math.max(1, totalButtons - MAX_INBOX_MAIL_BUTTONS);
        int pos = (int) Mth.map(topRowPos, 0f, 1f, 0f, scrollBarArea.getHeight() - size);

        scrollThumb = new Rect2i(scrollBarArea.getX(), scrollBarArea.getY() + pos, scrollBarArea.getWidth(), size);
    }

    protected void updateButtons() {
        addressButton.visible = !getMenu().isDefaultAddress() && getMenu().hasDefaultAddress();
        addressAttentionButton.visible = !getMenu().hasDefaultAddress();
        addressDefaultButton.visible = getMenu().isDefaultAddress();
        newMailButton.visible = getMenu().hasNewMail();
    }

    // --

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);

        // Extend slot highlight for mail slot to cover whole rectangle (mail slot is slightly bigger):

        int mouseX = (int) (Minecrft.get().mouseHandler.xpos()
              * (double) Minecrft.get().getWindow().getGuiScaledWidth()
              / (double) Minecrft.get().getWindow().getScreenWidth());
        int mouseY = (int) (Minecrft.get().mouseHandler.ypos()
              * (double) Minecrft.get().getWindow().getGuiScaledHeight()
              / (double) Minecrft.get().getWindow().getScreenHeight());

        if (slot.isActive() && slot.getContainerSlot() == MailboxBlockEntity.SLOT_MAIL
              && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
            guiGraphics.fillGradient(RenderType.guiOverlay(), slot.x - 1, slot.y - 1,
                  slot.x + 17, slot.y, 0x80FFFFFF, 0x80FFFFFF, 0);
            guiGraphics.fillGradient(RenderType.guiOverlay(), slot.x - 1, slot.y,
                  slot.x, slot.y + 16, 0x80FFFFFF, 0x80FFFFFF, 0);
            guiGraphics.fillGradient(RenderType.guiOverlay(), slot.x + 16, slot.y,
                  slot.x + 17, slot.y + 16, 0x80FFFFFF, 0x80FFFFFF, 0);
            guiGraphics.fillGradient(RenderType.guiOverlay(), slot.x - 1, slot.y + 16,
                  slot.x + 17, slot.y + 17, 0x80FFFFFF, 0x80FFFFFF, 0);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);

        int addressBarX = titleLabelX - 18;
        int addressBarWidth = imageWidth - (addressBarX * 2);
        // Left
        guiGraphics.blit(TEXTURE, leftPos + addressBarX, topPos, 0, imageHeight, 5, 15, 512, 256);
        // Middle
        guiGraphics.blit(TEXTURE, leftPos + addressBarX + 5, topPos, 5, imageHeight, addressBarWidth - 10, 15, 512, 256);
        // Right
        guiGraphics.blit(TEXTURE, leftPos + addressBarX + addressBarWidth - 5, topPos, 303, imageHeight, 5, 15, 512, 256);

        List<ItemStack> mail = getMenu().getMail();
        hoveredMail = null;
        scroll = Math.clamp(scroll, 0, Math.max(0, mail.size() - MAX_INBOX_MAIL_BUTTONS));

        for (int i = 0; i < Math.min(mail.size(), MAX_INBOX_MAIL_BUTTONS); i++) {
            int index = i + scroll;

            ItemStack item = mail.get(index);
            int x = 8;
            int y = 33 + 18 * i;
            boolean isHovering = isHovering(x + 1, y + 1, 115, 16, mouseX, mouseY);
            if (isHovering) {
                hoveredMail = item;
            }
            renderMailButton(guiGraphics, partialTick, mouseX, mouseY, item, leftPos + x, topPos + y);
        }

        if (!getMenu().getSlot(MailboxBlockEntity.SLOT_FOOD).hasItem()) {
            guiGraphics.blit(TEXTURE, leftPos + 227, topPos + 62, 314, 0, 16, 16, 512, 256);
        }
        if (!getMenu().getSlot(MailboxBlockEntity.SLOT_MAIL).hasItem()) {
            guiGraphics.blit(TEXTURE, leftPos + 247, topPos + 61, 330, 0, 18, 18, 512, 256);
        }

        renderScrollBar(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        FormattedCharSequence inbox = Component.empty()
              .append(inboxLabel)
              .append(" (" + (getMenu().getMail().isEmpty()
                    ? Component.translatable("gui.envelope.mailbox.empty").getString()
                    : getMenu().getMail().size()))
              .append(")")
              .getVisualOrderText();
        int inboxLabelX = 71 - font.width(inbox) / 2;
        guiGraphics.drawString(font, inbox, inboxLabelX, 21, 0x404040, false);

        int sendLabelX = 220 - font.width(sendLabel) / 2;
        guiGraphics.drawString(font, sendLabel, sendLabelX, 21, 0x404040, false);

        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    protected void renderMailButton(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, ItemStack mail, int x, int y) {
        boolean isHovered = hoveredMail == mail;

        guiGraphics.blitSprite(REGULAR_MAIL_BUTTON_SPRITES.get(true, isHovered), x, y, 0, 117, 18);

        guiGraphics.renderItem(mail, x + 2, y + 1);

        WidgetSprites iconSprites = getDisplayedIcon(mail);
        ResourceLocation iconSprite = isHovered ? iconSprites.enabledFocused() : iconSprites.enabled();
        guiGraphics.blitSprite(iconSprite, x + 23, y + 4, 0, 10, 10);

        String sender = getDisplayedSender(mail).getName().getString();
        if (font.width(sender) > 76) {
            sender = font.plainSubstrByWidth(sender, 72) + "...";
        }
        guiGraphics.drawString(font, sender, x + 36, y + 5, 0xFF886447, false);
    }

    protected void renderScrollBar(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        updateScrollThumb();

        int state = 0;
        if (!canScroll()) {
            state = 2;
        } else if (isDraggingScrollbar || isMouseOver(scrollThumb, mouseX, mouseY)) {
            state = 1;
        }

        // Top
        guiGraphics.blit(TEXTURE, scrollThumb.getX(), scrollThumb.getY(),
              308, state * SCROLL_THUMB_HEIGHT, scrollThumb.getWidth(), SCROLL_THUMB_TOP_HEIGHT, 512, 256);

        // Middle
        int middlePartsCount = (scrollThumb.getHeight() - SCROLL_THUMB_TOP_HEIGHT - SCROLL_THUMB_BOT_HEIGHT) / SCROLL_THUMB_MID_HEIGHT;

        for (int i = 0; i < middlePartsCount; i++) {
            guiGraphics.blit(TEXTURE, scrollThumb.getX(),
                  scrollThumb.getY() + SCROLL_THUMB_TOP_HEIGHT + i * SCROLL_THUMB_MID_HEIGHT,
                  308, state * SCROLL_THUMB_HEIGHT + SCROLL_THUMB_TOP_HEIGHT,
                  scrollThumb.getWidth(), SCROLL_THUMB_MID_HEIGHT, 512, 256);
        }

        if (!canScroll()) {
            // Special case to allow full size scroll thumb fill all available area.
            guiGraphics.blit(TEXTURE, scrollThumb.getX(),
                  scrollThumb.getY() + SCROLL_THUMB_TOP_HEIGHT + middlePartsCount * SCROLL_THUMB_MID_HEIGHT - 1,
                  308, state * SCROLL_THUMB_HEIGHT + SCROLL_THUMB_TOP_HEIGHT,
                  scrollThumb.getWidth(), SCROLL_THUMB_MID_HEIGHT, 512, 256);
            guiGraphics.blit(TEXTURE, scrollThumb.getX(), scrollThumb.getY() + SCROLL_THUMB_TOP_HEIGHT + (middlePartsCount * SCROLL_THUMB_MID_HEIGHT) + 1,
                  308, SCROLL_THUMB_TOP_HEIGHT + SCROLL_THUMB_MID_HEIGHT + state * SCROLL_THUMB_HEIGHT,
                  scrollThumb.getWidth(), SCROLL_THUMB_BOT_HEIGHT, 512, 256);
        } else {
            // Bottom
            guiGraphics.blit(TEXTURE, scrollThumb.getX(), scrollThumb.getY() + SCROLL_THUMB_TOP_HEIGHT + (middlePartsCount * SCROLL_THUMB_MID_HEIGHT),
                  308, SCROLL_THUMB_TOP_HEIGHT + SCROLL_THUMB_MID_HEIGHT + state * SCROLL_THUMB_HEIGHT,
                  scrollThumb.getWidth(), SCROLL_THUMB_BOT_HEIGHT, 512, 256);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);

        if (hoveredMail != null) {
            renderMailTooltip(guiGraphics, x, y, hoveredMail);
        }
    }

    protected void renderMailTooltip(GuiGraphics guiGraphics, int x, int y, ItemStack hoveredMail) {
        if (x >= leftPos + 8 && x < leftPos + 28) {
            guiGraphics.renderTooltip(font, getTooltipFromContainerItem(hoveredMail), hoveredMail.getTooltipImage(), x, y);
            return;
        }

        if (x >= leftPos + 31 && x < leftPos + 41 && y >= topPos + 37 && y < topPos + 47) {
            guiGraphics.renderTooltip(font, getDisplayedIconName(hoveredMail), x, y);
            return;
        }

        List<Component> tooltip = new ArrayList<>();

        // Show full sender address if it doesn't fit on the widget
        Address sender = getDisplayedSender(hoveredMail);
        if (font.width(sender.toString()) > 76) {
            tooltip.add(AddressFormatter.of(sender)
                  .withIcon()
                  .withIconColor(AddressFormatter.NEUTRAL_COLOR)
                  .withColor(ChatFormatting.WHITE)
                  .toComponent());
        }

        MailDeliveryLog deliveryLog = NewMail.getLog(hoveredMail);
        if (!deliveryLog.isEmpty()) {
            tooltip.add(Component.translatable("gui.envelope.delivery.log"));
            for (MailDeliveryRecord record : deliveryLog.records()) {
                tooltip.add(record.translate(Minecrft.level().getGameTime()));
            }
        }

        if (!tooltip.isEmpty()) {
            guiGraphics.renderTooltip(font, tooltip, Optional.empty(), x, y);
        }
    }

    // -- Scroll

    public boolean canScroll() {
        return getMenu().getMail().size() > MAX_INBOX_MAIL_BUTTONS;
    }

    public void scroll(int amount) {
        scrollTo(scroll + amount);
    }

    public void scrollTo(int buttonIndex) {
        int maxScrollWhenAtEnd = Math.max(0, getMenu().getMail().size() - MAX_INBOX_MAIL_BUTTONS);
        scroll = Mth.clamp(buttonIndex, 0, maxScrollWhenAtEnd);
        updateScrollThumb();
    }

    // -- Input


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_HOME) {
            scroll(Integer.MIN_VALUE);
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            scroll(Integer.MAX_VALUE);
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            scroll(-1);
            return true;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            scroll(1);
            return true;
        }


        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && hoveredMail != null) {
            int index = getMenu().getMail().indexOf(hoveredMail);
            if (index == -1) return false;

            MailboxMenu.MailAction action = MailboxMenu.MailAction.PICK_UP;
            if (Screen.hasShiftDown()) {
                if (Screen.hasControlDown()) {
                    action = MailboxMenu.MailAction.MOVE_ALL_TO_INVENTORY;
                } else {
                    action = MailboxMenu.MailAction.MOVE_TO_INVENTORY;
                }
            }

            if (getMenu().doMailAction(Minecrft.player(), index, action)) {
                Minecrft.player().playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
                Packets.sendToServer(new MailboxMenuInboxActionC2SP(index, action));
            }
        }

        if (canScroll()) {
            if (isMouseOver(scrollThumb, mouseX, mouseY)) {
                setDragging(true);
                isDraggingScrollbar = true;
                dragDelta = 0;
                scrollAtDragStart = scroll;
                return true;
            } else if (isMouseOver(scrollBarArea, mouseX, mouseY)) {
                int direction = mouseY < scrollThumb.getY() ? -1 : 1;
                scroll(MAX_INBOX_MAIL_BUTTONS * direction);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isDraggingScrollbar || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        dragDelta += dragY;

        double threshold = (double) scrollBarArea.getHeight() / Math.max(getMenu().getMail().size(), 1);
        int amount = (int) (dragDelta / threshold);
        if (amount != 0 || scroll != scrollAtDragStart) {
            scrollTo(scrollAtDragStart + amount);
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mailArea, mouseX, mouseY) || isMouseOver(scrollBarArea, mouseX, mouseY)) {
            scroll((int) -scrollY);
            return true;
        }
        return false;
    }

    // --

    protected WidgetSprites getDisplayedIcon(ItemStack mail) {
        return switch (NewMail.getStatus(mail)) {
            case RETURNED -> ICON_RETURNED_SPRITES;
            case REGULAR -> {
                Address sender = NewMail.getSender(mail);
                if (sender == Address.UNKNOWN) yield ICON_ADDRESS_UNKNOWN_SPRITES;
                if (sender == Address.MAIL_SERVICE) yield ICON_ADDRESS_MAIL_SERVICE_SPRITES;
                if (sender.type() == Address.Type.BLOCK) yield ICON_ADDRESS_BLOCK_SPRITES;
                if (sender.type() == Address.Type.PLAYER) yield ICON_ADDRESS_PLAYER_SPRITES;
                if (sender.type() == Address.Type.ENTITY) yield ICON_ADDRESS_ENTITY_SPRITES;
                yield ICON_ADDRESS_UNKNOWN_SPRITES;
            }
        };
    }

    protected Component getDisplayedIconName(ItemStack hoveredMail) {
        return switch (NewMail.getStatus(hoveredMail)) {
            case RETURNED -> Component.translatable("gui.envelope.mail.status.returned");
            case REGULAR -> {
                Address address = NewMail.getSender(hoveredMail);
                if (address.equals(Address.UNKNOWN)) yield Component.translatable("address.envelope.unknown");
                if (address.equals(Address.MAIL_SERVICE)) yield Component.translatable("address.envelope.mail_service");
                yield switch (address.type()) {
                    case BLOCK -> Component.translatable("address.envelope.type.block");
                    case PLAYER -> Component.translatable("address.envelope.type.player");
                    case ENTITY -> Component.translatable("address.envelope.type.entity");
                };
            }
        };
    }

    protected Address getDisplayedSender(ItemStack mail) {
        return NewMail.getSender(mail);
    }

    // --

    protected boolean isMouseOver(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX < rect.getX() + rect.getWidth()
              && mouseY >= rect.getY() && mouseY < rect.getY() + rect.getHeight();
    }
}
