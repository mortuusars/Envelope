package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.StoredMail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.PigeonholeMenuMailActionC2SP;
import io.github.mortuusars.envelope.util.PrettyGameTime;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;

public class PigeonholeScreen extends AbstractContainerScreen<PigeonholeMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/pigeonhole.png");

    public static final WidgetSprites ADDRESS_BUTTON_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/address_button"));
    public static final WidgetSprites ADDRESS_ATTENTION_BUTTON_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/address_attention_button"), Envelope.resource("pigeonhole/address_button_highlighted"));
    public static final WidgetSprites ADDRESS_DEFAULT_BUTTON_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/address_default_button"));

    public static final WidgetSprites REGULAR_MAIL_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("pigeonhole/mail_button"));

    public static final WidgetSprites ICON_ADDRESS_PIGEONHOLE_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_pigeonhole"));
    public static final WidgetSprites ICON_ADDRESS_PLAYER_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_player"));
    public static final WidgetSprites ICON_ADDRESS_NPC_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_npc"));
    public static final WidgetSprites ICON_ADDRESS_MAIL_SERVICE_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_mail_service"));
    public static final WidgetSprites ICON_ADDRESS_UNKNOWN_SPRITES = Sprites.normalAndHighlighted(Envelope.resource("pigeonhole/icon_unknown"));
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

    protected List<Occupant> occupantsData = new ArrayList<>();
    protected List<@Nullable LivingEntity> occupants = new ArrayList<>();
    protected Int2ObjectMap<Rect2i> occupantAreas = new Int2ObjectOpenHashMap<>(Map.of(
          0, new Rect2i(183, 30, 32, 32),
          1, new Rect2i(145, 47, 32, 32),
          2, new Rect2i(179, 68, 32, 32)
    ));

    @Nullable
    protected StoredMail hoveredMail;

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

    public PigeonholeScreen(PigeonholeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        updateOccupantsData(getMenu().getBlockEntity().getImmutableOccupants());
    }

    // --

    public void updateOccupantsData(List<Occupant> occupantsData) {
        if (this.occupantsData.size() == occupantsData.size() && Minecrft.level().getGameTime() % 40 != 0) {
            return;
        }

        this.occupantsData = occupantsData;

        occupants.clear();
        occupants.add(0, null);
        occupants.add(1, null);
        occupants.add(2, null);

        for (Occupant occupant : this.occupantsData) {
            if (occupant.slot() < 3 && EntityType.loadEntityRecursive(occupant.entityData().copyTag(),
                  Minecrft.level(), Function.identity()) instanceof LivingEntity entity) {
                entity.setOnGround(true);
                if (entity instanceof Pigeon pigeon) {
                    pigeon.setSitting(true);
                }
                occupants.set(occupant.slot(), entity);
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateOccupantsData(getMenu().getBlockEntity().getImmutableOccupants());
        this.occupants.forEach(o -> {
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
              Component.translatable("gui.envelope.pigeonhole.address"));
        addressButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.address")
              .append("\n")
              .append(Component.translatable("gui.envelope.pigeonhole.address.tooltip"))));
        addRenderableWidget(addressButton);

        addressAttentionButton = new ImageButton(leftPos + titleLabelX - 11, topPos + 4, 10, 10,
              ADDRESS_ATTENTION_BUTTON_SPRITES,
              button -> setAsDefaultAddress(),
              Component.translatable("gui.envelope.pigeonhole.address"));
        addressAttentionButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.address")
              .append("\n")
              .append(Component.translatable("gui.envelope.pigeonhole.address.tooltip"))));
        addRenderableWidget(addressAttentionButton);

        addressDefaultButton = new ImageButton(leftPos + titleLabelX - 11, topPos + 4, 10, 10, ADDRESS_DEFAULT_BUTTON_SPRITES, btn -> {
        });
        addressDefaultButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.address.default")
              .append("\n")
              .append(Component.translatable("gui.envelope.pigeonhole.address.default.tooltip"))));
        addressDefaultButton.active = false;
        addRenderableWidget(addressDefaultButton);

        newMailButton = new ImageButton(leftPos + 7, topPos + 21, 8, 8,
              NEW_MAIL_INDICATOR_SPRITES,
              button -> {
                  refreshMail();
                  scrollTo(0);
              });
        newMailButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.pigeonhole.mail.tooltip.new_mail")
              .append("\n")
              .append(Component.translatable("gui.envelope.pigeonhole.mail.tooltip.new_mail.click_to_refresh"))));
        addRenderableWidget(newMailButton);

        updateButtons();
    }

    protected void setAsDefaultAddress() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PigeonholeMenu.ADDRESS_BUTTON_ID);
    }

    protected void refreshMail() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PigeonholeMenu.REFRESH_MAIL_BUTTON_ID);
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

        renderOccupants(guiGraphics, mouseX, mouseY, partialTick);
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

        if (slot.isActive() && slot.getContainerSlot() == PigeonholeBlockEntity.SLOT_MAIL
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

        List<StoredMail> mail = getMenu().getMail();
        hoveredMail = null;
        scroll = Math.clamp(scroll, 0, Math.max(0, mail.size() - MAX_INBOX_MAIL_BUTTONS));

        for (int i = 0; i < Math.min(mail.size(), MAX_INBOX_MAIL_BUTTONS); i++) {
            int index = i + scroll;

            StoredMail item = mail.get(index);
            int x = 8;
            int y = 33 + 18 * i;
            boolean isHovering = isHovering(x + 1, y + 1, 115, 16, mouseX, mouseY);
            if (isHovering) {
                hoveredMail = item;
            }
            renderMailButton(guiGraphics, partialTick, mouseX, mouseY, item, leftPos + x, topPos + y);
        }

        if (!getMenu().getSlot(PigeonholeBlockEntity.SLOT_FOOD).hasItem()) {
            guiGraphics.blit(TEXTURE, leftPos + 227, topPos + 62, 314, 0, 16, 16, 512, 256);
        }
        if (!getMenu().getSlot(PigeonholeBlockEntity.SLOT_MAIL).hasItem()) {
            guiGraphics.blit(TEXTURE, leftPos + 247, topPos + 61, 330, 0, 18, 18, 512, 256);
        }

        renderScrollBar(guiGraphics, partialTick, mouseX, mouseY);
    }

    protected void renderOccupants(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Nest BG
        occupantAreas.forEach((i, area) -> {
            guiGraphics.blit(TEXTURE, leftPos + area.getX() - 1, topPos + area.getY() + area.getHeight() - 11,
                  0, 348, 0, 34, 16, 512, 256);
        });

        // Entity
        for (int i = 0; i < Math.min(3, occupants.size()); i++) {
            LivingEntity entity = occupants.get(i);
            if (entity == null) continue;

            Rect2i area = occupantAreas.get(i);

            int yRotOffset = i == 0 ? 25
                  : i == 1 ? -25 : 15;

            renderEntityFollowsMouse(guiGraphics,
                  leftPos + area.getX(), topPos + area.getY(),
                  leftPos + area.getX() + area.getWidth(), topPos + area.getY() + area.getHeight(),
                  Math.min(area.getWidth(), area.getHeight()), 0, mouseX, mouseY, entity, yRotOffset);
        }

        // Nest FG
        occupantAreas.forEach((i, area) -> {
            guiGraphics.blit(TEXTURE, leftPos + area.getX() - 1, topPos + area.getY() + area.getHeight() - 11,
                  300, 348, 16, 34, 16, 512, 256);
        });
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);

        FormattedCharSequence inbox = Component.empty()
              .append(inboxLabel)
              .append(" (" + (getMenu().getMail().isEmpty()
                    ? Component.translatable("gui.envelope.pigeonhole.empty").getString()
                    : getMenu().getMail().size()))
              .append(")")
              .getVisualOrderText();
        int inboxLabelX = 71 - font.width(inbox) / 2;
        guiGraphics.drawString(font, inbox, inboxLabelX, 21, 0x404040, false);

        int sendLabelX = 220 - font.width(sendLabel) / 2;
        guiGraphics.drawString(font, sendLabel, sendLabelX, 21, 0x404040, false);

        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    protected void renderMailButton(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, StoredMail mail, int x, int y) {
        boolean isHovered = hoveredMail == mail;

        guiGraphics.blitSprite(REGULAR_MAIL_BUTTON_SPRITES.get(true, isHovered), x, y, 0, 117, 18);

        guiGraphics.renderItem(mail.getItemForReading(), x + 2, y + 1);

        WidgetSprites iconSprites = getMailIconSprites(mail);
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

    protected void renderMailTooltip(GuiGraphics guiGraphics, int x, int y, StoredMail hoveredMail) {
        if (x >= leftPos + 8 && x < leftPos + 28) {
            guiGraphics.renderTooltip(font, getTooltipFromContainerItem(hoveredMail.getItemForReading()),
                  hoveredMail.getItemForReading().getTooltipImage(), x, y);
            return;
        }

        List<Component> tooltip = new ArrayList<>();

        Address sender = getDisplayedSender(hoveredMail);
        if (font.width(sender.toString()) > 76) {
            tooltip.add(AddressFormatter.of(sender)
                  .withIcon()
                  .withIconColor(AddressFormatter.NEUTRAL_COLOR)
                  .withColor(ChatFormatting.WHITE)
                  .toComponent());
        }

        DeliveryLog deliveryLog = hoveredMail.getLog();

        if (!Screen.hasShiftDown() || deliveryLog.isEmpty()) {
            deliveryLog.getLastExceptionRecord().ifPresent(record -> {
                int color = switch (record.status()) {
                    case RETURNED -> 0xFFD47F46;
                    case REJECTED -> 0xFFD7503E;
                    case UNCLAIMED -> 0xFFDDC649;
                    default -> throw new IllegalStateException("Unexpected value: " + record.status());
                };
                tooltip.add(record.status().translate().withStyle(Style.EMPTY.withColor(color)));
            });

            deliveryLog.getLastRecordOfType(DeliveryRecord.Status.ARRIVED)
                  .flatMap(DeliveryRecord::timestamp)
                  .ifPresent(arrivedAt -> {
                      long ageTicks = Minecrft.level().getGameTime() - arrivedAt;
                      tooltip.add(Component.translatable("gui.envelope.time.elapsed", PrettyGameTime.durationLargest(ageTicks)));
                  });
        } else {
            if (!deliveryLog.isEmpty()) {
                tooltip.add(Component.translatable("gui.envelope.delivery.log"));
                for (DeliveryRecord record : deliveryLog.records()) {
                    tooltip.add(record.translate(Minecrft.level().getGameTime()));
                }
            }
        }

        if (!tooltip.isEmpty()) {
            guiGraphics.renderTooltip(font, tooltip, Optional.empty(), x, y);
        }
    }

    /**
     * Copy of InventoryScreen#renderEntityInInventoryFollowsMouse but with several changes to entity rotations,
     * to keep body from rotating that much, as Pigeons are supposed to be sitting in the nest.
     */
    public static void renderEntityFollowsMouse(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2,
                                                int scale, float yOffset, float mouseX, float mouseY, LivingEntity entity, int yRotOffset) {
        float f = (float) (x1 + x2) / 2.0F;
        float g = (float) (y1 + y2) / 2.0F;
        guiGraphics.enableScissor(x1, y1, x2, y2);
        float h = (float) Math.atan((f - mouseX) / 40.0F);
        float i = (float) Math.atan((g - mouseY) / 40.0F);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX((i - 2f) * 5.0F * (float) (Math.PI / 180.0));
        quaternionf.mul(quaternionf2);
        float j = entity.yBodyRot;
        float k = entity.getYRot();
        float l = entity.getXRot();
        float m = entity.yHeadRotO;
        float n = entity.yHeadRot;
        entity.yBodyRot = 180.0F + h * 5.0F + yRotOffset;
        entity.setYRot(180.0F + h * 40.0F);
        entity.setXRot(-i * 30.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        float o = entity.getScale();
        Vector3f vector3f = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset * o, 0.0F);
        float p = (float) scale / o;
        InventoryScreen.renderEntityInInventory(guiGraphics, f, g, p, vector3f, quaternionf, quaternionf2, entity);
        entity.yBodyRot = j;
        entity.setYRot(k);
        entity.setXRot(l);
        entity.yHeadRotO = m;
        entity.yHeadRot = n;
        guiGraphics.disableScissor();
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

            PigeonholeMenu.MailAction action = PigeonholeMenu.MailAction.PICK_UP;
            if (Screen.hasShiftDown()) {
                if (Screen.hasControlDown()) {
                    action = PigeonholeMenu.MailAction.MOVE_ALL_TO_INVENTORY;
                } else {
                    action = PigeonholeMenu.MailAction.MOVE_TO_INVENTORY;
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
        if (isMouseOver(mailArea, mouseX, mouseY) || isMouseOver(scrollBarArea, mouseX, mouseY)) {
            scroll((int) -scrollY);
            return true;
        }
        return false;
    }

    // --

    protected Address getDisplayedSender(StoredMail mail) {
        return mail.getLog().getLastExceptionRecord()
              .map(DeliveryRecord::address)
              .orElse(mail.getSender());
    }

    protected WidgetSprites getMailIconSprites(StoredMail mail) {
        return mail.getLog().getLastExceptionRecord()
              .map(record -> switch (record.status()) {
                  case RETURNED -> ICON_RETURNED_SPRITES;
                  case REJECTED -> ICON_REJECTED_SPRITES;
                  case UNCLAIMED -> ICON_UNCLAIMED_SPRITES;
                  default -> throw new IllegalStateException("Unexpected value: " + record.status());
              })
              .orElseGet(() -> {
                  Address sender = mail.getOrDefault(Envelope.DataComponents.SENDER, Address.UNKNOWN);
                  if (sender == Address.UNKNOWN) return ICON_ADDRESS_UNKNOWN_SPRITES;
                  if (sender == Address.MAIL_SERVICE) return ICON_ADDRESS_MAIL_SERVICE_SPRITES;
                  if (sender.type() == Address.Type.PIGEONHOLE) return ICON_ADDRESS_PIGEONHOLE_SPRITES;
                  if (sender.type() == Address.Type.PLAYER) return ICON_ADDRESS_PLAYER_SPRITES;
                  if (sender.type() == Address.Type.ENTITY) return ICON_ADDRESS_NPC_SPRITES;
                  return ICON_ADDRESS_UNKNOWN_SPRITES;
              });
    }

    protected boolean isMouseOver(Rect2i rect, double mouseX, double mouseY) {
        return mouseX >= rect.getX() && mouseX < rect.getX() + rect.getWidth()
              && mouseY >= rect.getY() && mouseY < rect.getY() + rect.getHeight();
    }
}
