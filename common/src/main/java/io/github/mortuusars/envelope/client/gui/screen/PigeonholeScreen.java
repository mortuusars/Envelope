package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.PigeonholeMenuMailActionC2SP;
import io.github.mortuusars.envelope.util.PrettyGameTime;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PigeonholeScreen extends AbstractContainerScreen<PigeonholeMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/pigeonhole.png");

    public static final WidgetSprites ADDRESS_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("pigeonhole/address_button"));
    public static final WidgetSprites ADDRESS_MAIN_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("pigeonhole/address_main_button"));

    public static final WidgetSprites REGULAR_MAIL_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("pigeonhole/mail_button"));
    public static final WidgetSprites ICON_PLAYER_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_player"));
    public static final WidgetSprites ICON_NPC_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_npc"));
    public static final WidgetSprites ICON_REJECTED_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_rejected"));
    public static final WidgetSprites ICON_RETURNED_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_returned"));
    public static final WidgetSprites ICON_UNCLAIMED_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_unclaimed"));

    public static final WidgetSprites NEW_MAIL_INDICATOR_SPRITES = Sprites.normalOnly(Envelope.resource("pigeonhole/new_mail_indicator"));

    protected static final int SCROLL_THUMB_TOP_HEIGHT = 3;
    protected static final int SCROLL_THUMB_MID_HEIGHT = 4;
    protected static final int SCROLL_THUMB_BOT_HEIGHT = 2;
    protected static final int SCROLL_THUMB_HEIGHT = SCROLL_THUMB_TOP_HEIGHT + SCROLL_THUMB_MID_HEIGHT + SCROLL_THUMB_BOT_HEIGHT;

    protected static final int MAX_INBOX_MAIL_BUTTONS = 9;

    protected Component inboxLabel = Component.translatable("gui.envelope.pigeonhole.inbox");
    protected Component sendLabel = Component.translatable("gui.envelope.pigeonhole.send");

    protected List<@Nullable LivingEntity> occupants = new ArrayList<>();
    protected Int2ObjectMap<Rect2i> occupantAreas = new Int2ObjectOpenHashMap<>(Map.of(
            0, new Rect2i(183, 32, 32, 32),
            1, new Rect2i(145, 49, 32, 32),
            2, new Rect2i(179, 70, 32, 32)
    ));

    @Nullable
    protected ItemStack hoveredMail;

    protected ImageButton addressButton;
    protected ImageButton addressMainButton;
    protected ImageButton newMailButton;

    protected Rect2i mailArea = new Rect2i(0, 0, 0, 0);
    protected Rect2i scrollBarArea = new Rect2i(0, 0, 0, 0);
    protected Rect2i scrollThumb = new Rect2i(0, 0, 0, 0);
    protected int scroll = 0;
    protected int scrollAtDragStart = 0;
    protected boolean isDraggingScrollbar = false;
    protected double dragDelta = 0;

    public PigeonholeScreen(PigeonholeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // --

    public void setOccupantsData(List<PigeonholeBlockEntity.Occupant> occupantsData) {
        occupants.clear();
        occupants.add(0, null);
        occupants.add(1, null);
        occupants.add(2, null);

        for (PigeonholeBlockEntity.Occupant occupant : occupantsData) {
            if (occupant.slot() < 3 && occupant.createEntity(Minecrft.level(), BlockPos.ZERO) instanceof LivingEntity entity) {
                entity.setOnGround(true);
                occupants.set(occupant.slot(), entity);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        occupants.forEach(o -> {
            if (o != null) {
                o.tick();
            }
        });
    }

    // --

    @Override
    protected void init() {
        imageWidth = 308;
        imageHeight = 203;
        titleLabelX = 159 - (font.width(title) / 2) + 6;
        titleLabelY = 5;
        inventoryLabelX = 140;
        inventoryLabelY = imageHeight - 94;
        super.init();
        mailArea = new Rect2i(leftPos + 8, topPos + 32, 117, 162);
        scrollBarArea = new Rect2i(leftPos + 128, topPos + 33, 6, 161);

        addressButton = new ImageButton(leftPos + titleLabelX - 12, topPos + 4, 10, 10, ADDRESS_BUTTON_SPRITES, btn -> {
            Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PigeonholeMenu.ADDRESS_BUTTON_ID);
        }, Component.translatable("gui.envelope.pigeonhole.address"));
        addressButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.address")
                .append("\n")
                .append(Component.translatable("gui.envelope.pigeonhole.address.tooltip"))));

        addRenderableWidget(addressButton);

        addressMainButton = new ImageButton(leftPos + titleLabelX - 12, topPos + 4, 10, 10, ADDRESS_MAIN_BUTTON_SPRITES, btn -> {});
        addressMainButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.address.main")
                .append("\n")
                .append(Component.translatable("gui.envelope.pigeonhole.address.main.tooltip"))));
        addressMainButton.active = false;
        addRenderableWidget(addressMainButton);

        newMailButton = new ImageButton(leftPos + 7, topPos + 21, 8, 8, NEW_MAIL_INDICATOR_SPRITES, btn -> {
            Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PigeonholeMenu.REFRESH_MAIL_BUTTON_ID);
            scrollTo(0);
        });
        newMailButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.mail.tooltip.new_mail")
                .append("\n")
                .append(Component.translatable("gui.envelope.pigeonhole.mail.tooltip.new_mail.click_to_refresh"))));
        addRenderableWidget(newMailButton);
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
        addressButton.visible = !getMenu().isMain();
        addressMainButton.visible = getMenu().isMain();
        newMailButton.visible = getMenu().hasNewMail();
    }

    // --

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderOccupants(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderOccupants(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < Math.min(3, occupants.size()); i++) {
            LivingEntity entity = occupants.get(i);
            if (entity == null) {
                continue;
            }

            Rect2i area = occupantAreas.get(i);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0.5, 50);
            guiGraphics.fill(leftPos + area.getX() + 8, topPos + area.getY() + area.getHeight() - 3,
                    leftPos + area.getX() + area.getWidth() - 8, topPos + area.getY() + area.getHeight() - 1, 0xFF555555);
            guiGraphics.pose().popPose();

            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                    leftPos + area.getX(), topPos + area.getY(),
                    leftPos + area.getX() + area.getWidth(), topPos + area.getY() + area.getHeight(),
                    Math.min(area.getWidth(), area.getHeight()), 0, mouseX, mouseY, entity);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        FormattedCharSequence inbox = Component.empty()
                .append(inboxLabel)
                .append(" - " + (getMenu().getMail().isEmpty()
                        ? Component.translatable("gui.envelope.pigeonhole.empty").getString()
                        : getMenu().getMail().size()))
                .getVisualOrderText();
        int inboxLabelX = 71 - font.width(inbox) / 2;
        guiGraphics.drawString(font, inbox, inboxLabelX, 21, 0x404040, false);

        int sendLabelX = 220 - font.width(sendLabel) / 2;
        guiGraphics.drawString(font, sendLabel, sendLabelX, 21, 0x404040, false);

        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);

        List<ItemStack> mail = getMenu().getMail();

        hoveredMail = null;

        scroll = Math.clamp(scroll, 0, Math.max(0, mail.size() - MAX_INBOX_MAIL_BUTTONS));

        for (int i = 0; i < Math.min(mail.size(), MAX_INBOX_MAIL_BUTTONS); i++) {
            int index = i + scroll;

            ItemStack item = mail.get(index);
            int x = 8;
            int y = 33 + 18 * i;
            boolean isHovering = isHovering(x + 1, y + 1, 117, 16, mouseX, mouseY);
            if (isHovering) {
                hoveredMail = item;
            }
            renderMailButton(guiGraphics, partialTick, mouseX, mouseY, item, leftPos + x, topPos + y);
        }

        renderScrollBar(guiGraphics, partialTick, mouseX, mouseY);
    }

    protected void renderMailButton(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, ItemStack mail, int x, int y) {
        boolean isHovered = hoveredMail == mail;

        guiGraphics.blitSprite(REGULAR_MAIL_BUTTON_SPRITES.get(true, isHovered), x, y, 0, 117, 18);

        WidgetSprites iconSprites = getMailIconSprites(mail);
        ResourceLocation iconSprite = isHovered ? iconSprites.enabledFocused() : iconSprites.enabled();
        guiGraphics.blitSprite(iconSprite, x + 23, y + 4, 0, 10, 10);

        guiGraphics.renderItem(mail, x + 2, y + 1);

        Component senderName = mail.getOrDefault(Envelope.DataComponents.MAIL_SENDER, Address.UNKNOWN).getDisplayName();
        FormattedCharSequence sender;
        if (font.split(senderName, 78).size() > 1) {
            sender = FormattedCharSequence.composite(
                    font.split(senderName, 72).getFirst(),
                    Component.literal("...").getVisualOrderText());
        } else {
            sender = senderName.getVisualOrderText();
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

        if (hoveredMail == null) {
            return;
        }

        if (x >= leftPos + 8 && x < leftPos + 28) {
            guiGraphics.renderTooltip(font, getTooltipFromContainerItem(hoveredMail),
                    Optional.empty(), x, y);
        } else {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(hoveredMail.getHoverName());

            Component senderName = hoveredMail.getOrDefault(Envelope.DataComponents.MAIL_SENDER, Address.UNKNOWN).getDisplayName();
            if (font.split(senderName, 110).size() > 1) {
                tooltip.add(Component.translatable("gui.envelope.pigeonhole.mail.tooltip.sender", senderName));
            }

            MailTravelingLog.of(hoveredMail).getLastRecord().ifPresent(record -> {
                long receivedAt = record.timestamp() + record.duration();
                long ageTicks = Minecrft.level().getGameTime() - receivedAt;
                tooltip.add(Component.translatable("gui.envelope.pigeonhole.mail.tooltip.age", PrettyGameTime.durationLargest(ageTicks)));
            });

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

            PigeonholeMenu.Action action = PigeonholeMenu.Action.PICK_UP;
            if (Screen.hasShiftDown()) {
                if (Screen.hasControlDown()) {
                    action = PigeonholeMenu.Action.MOVE_ALL_TO_INVENTORY;
                } else {
                    action = PigeonholeMenu.Action.MOVE_TO_INVENTORY;
                }
            }

            if (getMenu().doMailAction(Minecrft.player(), index, action)) {
                Minecrft.player().playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
                Packets.sendToServer(new PigeonholeMenuMailActionC2SP(index, action));
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
        if (isMouseOver(mailArea, mouseX, mouseY)) {
            scroll((int) -scrollY);
            return true;
        }
        return false;
    }

    // --

    protected WidgetSprites getMailIconSprites(ItemStack mail) {
        return ICON_NPC_SPRITES;
//        return switch (mail.status()) {
//            case RETURNED -> ICON_RETURNED_SPRITES;
//            case REJECTED -> ICON_REJECTED_SPRITES;
//            case UNCLAIMED -> ICON_UNCLAIMED_SPRITES;
//            case REGULAR -> /*mail.sender().type() == Address.Type.PLAYER
//                    ? ICON_PLAYER_SPRITES
//                    :*/ ICON_NPC_SPRITES;
//        };
    }

    protected boolean isMouseOver(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX < rect.getX() + rect.getWidth()
                && mouseY >= rect.getY() && mouseY < rect.getY() + rect.getHeight();
    }
}
