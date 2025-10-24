package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import io.github.mortuusars.envelope.world.mail.address.validation.PigeonholeAddressValidator;
import io.github.mortuusars.envelope.world.mail.address.validation.Validator;
import io.github.mortuusars.envelope.network.Packets;
import io.github.mortuusars.envelope.network.packet.serverbound.PigeonholeAddressTagApplyC2SP;
import io.github.mortuusars.envelope.util.EnvelopeSymbols;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PigeonholeAddressTagScreen extends AddressTagScreen {
    protected final LocalPlayer player;
    protected final BlockPos pos;
    protected final BlockState state;
    protected final Validator.CachedValidator<String> addressValidator;

    public PigeonholeAddressTagScreen(InteractionHand hand, AllAddresses knownAddresses,
                                      BlockPos pos, Optional<Address.Pigeonhole> existingAddress) {
        super(hand, knownAddresses);
        this.player = Minecrft.player();
        this.pos = pos;
        this.state = Minecrft.level().getBlockState(pos);
        this.existingAddress = existingAddress.map(Address.class::cast);
        this.addressValidator = new Validator.CachedValidator<>(
                new PigeonholeAddressValidator(player, existingAddress.map(Address.class::cast))
                        .setKnownAddresses(knownAddresses));
    }

    // -- Address

    @Override
    protected String getInitialAddressValue() {
        return existingAddress
                .map(Address::id)
                .orElseGet(() ->
                        Optional.ofNullable(player.getItemInHand(hand).get(Envelope.DataComponents.ADDRESS))
                                .map(Address::getDisplayName)
                                .map(Component::getString)
                                .orElse(""));
    }

    @Override
    protected @Nullable FormattedString getAutocompleteSuggestion(String addressId) {
        return null; // Don't suggest anything
    }

    // --

    @Override
    protected ItemStack getTarget() {
        return new ItemStack(state.getBlock().asItem());
    }

    protected void updateConfirmButton() {
        if (confirmButton == null) return; // Not initialized yet

        confirmButton.active = canConfirm();

        MutableComponent confirmTooltip = Component.translatable("gui.envelope.confirm");

        if (!canConfirm()) {
            getAddressValidator().getCurrentIssues().forEach(issue -> {
                confirmTooltip.append("\n").append(issue.translate());
            });
            confirmButton.setTooltip(Tooltip.create(confirmTooltip));
            return;
        }

        if (isRenaming() && !isCurrentIdSameAsExistingAddress()) {
            confirmTooltip.append("\n")
                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.inbox")
                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)))
                    .append("\n")
                    .append(Component.translatable("gui.envelope.pigeonhole_address.rename_warning.traveling")
                            .withStyle(Style.EMPTY.withColor(0xFFE76A6A)));
        }
        confirmButton.setTooltip(Tooltip.create(confirmTooltip));
    }

    // -- Events

    @Override
    protected void addressTextChanged(FormattedString text) {
        super.addressTextChanged(text);
        getAddressValidator().setValue(getCurrentAddressId());
    }

    @Override
    protected void confirm() {
        if (!canConfirm()) {
            return;
        }

        Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f));
        int slot = this.hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
        Packets.sendToServer(new PigeonholeAddressTagApplyC2SP(slot, getCurrentAddressId().trim(), pos));
        close();
    }

    // -- Render

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!stillValid()) {
            close();
        }

        updateConfirmButton();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderExperienceCost(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderAddressType(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
//        boolean isValid = getMenu().getValidationState() != PigeonholeAddressTagMenu.AddressValidation.ERR_TAKEN;
        int color = /*isValid ?*/ 0xFF7B593D/* : 0xFFFA5951*/;
        guiGraphics.drawString(font, EnvelopeSymbols.ADDRESS_PIGEONHOLE,
                leftPos + 12, topPos + 18, color, false);
//        if (!isValid && isHovering(9, 17, 9, 9, mouseX, mouseY)) {
//            guiGraphics.renderTooltip(font, getMenu().getValidationState().translate(), mouseX, mouseY);
//        }
    }

    protected void renderExperienceCost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isCurrentIdSameAsExistingAddress()) return;

        int cost = Config.Server.PIGEONHOLE_ADDRESS_EXPERIENCE_LEVELS_COST.get();
        if (cost <= 0) return;

        boolean hasEnough = player.experienceLevel >= cost;
        ResourceLocation sprite = Envelope.resource("address_tag/experience" + (hasEnough ? "" : "_disabled"));

        int x = 150;
        int y = 4;

        guiGraphics.blitSprite(sprite, leftPos + x, topPos + y, 11, 11);

        // Below is rendering of a xp level number with outline
        // Mojang did this with texture, but in our case it needs to be dynamic (because it's configurable)

        String text = Integer.toString(cost);
        int centerColor = hasEnough ? 0xFFC8FF8F : 0xFF8C605D;
        int outlineColor = hasEnough ? 0xFF2D2102 : 0xFF47352F;

        x += 7;
        y += 2;

        guiGraphics.drawString(font, text, leftPos + x, topPos + y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x + 1, topPos + y - 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x + 1, topPos + y, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x + 1, topPos + y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x, topPos + y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x - 1, topPos + y + 1, outlineColor, false);
        guiGraphics.drawString(font, text, leftPos + x - 1, topPos + y, outlineColor, false);

        guiGraphics.drawString(font, text, leftPos + x, topPos + y, centerColor, false);
    }

    // --

    protected boolean stillValid() {
        return player.getItemInHand(hand).getItem() instanceof AddressTagItem
                && player.level().getBlockState(pos).getBlock() instanceof PigeonholeBlock
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0D;
    }

    // -- Validation

    public Validator.CachedValidator<String> getAddressValidator() {
        return addressValidator;
    }

    protected boolean canConfirm() {
        return getAddressValidator().getCurrentIssues().isEmpty();
    }
}
