package io.github.mortuusars.envelope.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.github.mortuusars.envelope.world.mail.address.AllAddresses;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddressBoxSuggestions extends AbstractWidget {
    protected final Font font = Minecrft.get().font;

    protected final EditBox editBox;
    protected final AllAddresses addresses;

    protected final List<Suggestion> suggestions = new ArrayList<>();
    protected int maxDisplayedLines;
    protected int selectedIndex = 0;
    protected int scroll = 0;
    protected boolean show;

    public AddressBoxSuggestions(EditBox addressBox, AllAddresses addresses, int maxDisplayedLines) {
        super(addressBox.getX(), addressBox.getY() + addressBox.getHeight() + 3, addressBox.getWidth(), 1, Component.empty());
        this.editBox = addressBox;
        this.addresses = addresses;
        this.maxDisplayedLines = maxDisplayedLines;
    }

    public void update() {
        suggestions.clear();
        selectedIndex = 0;
        setScroll(0);
        editBox.setSuggestion(null);

        String text = editBox.getValue();

        if (!addresses.isKnown(text)) {
            suggestions.addAll(SharedSuggestionProvider.suggest(
                        addresses.stream().map(Address::getString),
                        new SuggestionsBuilder(text, 0)
                  )
                  .join()
                  .getList());
            show = !text.isEmpty();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!show) {
            return;
        }

        int lineHeight = 11;

        setHeight(lineHeight * Math.min(maxDisplayedLines, suggestions.size()));
        int y = getY();

        int hoveredIndex = getHoveredSuggestionIndex(mouseX, mouseY);

        for (int i = scroll; i < Math.min(maxDisplayedLines + scroll, suggestions.size()); i++) {
            Suggestion suggestion = suggestions.get(i);

            boolean selected = i == selectedIndex;

            int textColor = selected ? 0xFFEDB548 : 0xFFFFFFFF;
            int fillColor = selected ? 0xD0000000 : 0xD0111111;

            guiGraphics.fill(getX() - 13, y, getX() + getWidth(), y + lineHeight, fillColor);

            String icon = addresses.byName(suggestion.getText()).map(AddressFormatter::getIcon).orElse("");
            if (!icon.isBlank()) {
                int iconColor = selected ? textColor : 0xFFAAAAAA;
                guiGraphics.drawString(font, icon, getX() - 9, y + 1, iconColor, true);
            }

            String text = suggestion.getText();
            if (font.width(text) > getWidth()) {
                String ellipsis = "...";
                text = font.plainSubstrByWidth(suggestion.getText(), getWidth() - font.width(ellipsis)) + ellipsis;

                if (hoveredIndex == i) {
                    guiGraphics.renderTooltip(font, Component.literal(suggestion.getText()), mouseX, mouseY);
                }
            }
            guiGraphics.drawString(font, text, getX(), y + 1, textColor, true);

            y += lineHeight;
        }

        renderScrollBar(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void renderScrollBar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!suggestions.isEmpty() && suggestions.size() > maxDisplayedLines) {
            int trackHeight = getHeight() - 2;

            int maxScroll = suggestions.size() - maxDisplayedLines;
            int thumbHeight = trackHeight * maxDisplayedLines / suggestions.size();

            float scrollRatio = maxScroll != 0 ? (float) scroll / maxScroll : 0;

            int thumbY = (int) ((trackHeight - thumbHeight) * scrollRatio);

            guiGraphics.fill(getX() + getWidth(), getY(), getX() + getWidth() + 3, getY() + getHeight(), 0xD0111111);
            guiGraphics.fill(getX() + getWidth() + 1, getY() + thumbY + 1,
                  getX() + getWidth() + 2, getY() + thumbY + thumbHeight + 1, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_LCONTROL) {
            show = !show;
            return true;
        }

        if (!show) {
            return false;
        }

        if (keyCode == InputConstants.KEY_UP) {
            select(selectedIndex - 1);
            return true;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            select(selectedIndex + 1);
            return true;
        }
        if (keyCode == InputConstants.KEY_TAB) {
            getSelected().ifPresent(suggestion -> editBox.setValue(suggestion.getText()));
            Minecrft.get().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return true;
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!show) {
            return;
        }

        int hoveredSuggestion = getHoveredSuggestionIndex(mouseX, mouseY);
        if (hoveredSuggestion >= 0 && selectedIndex != hoveredSuggestion) {
            select(hoveredSuggestion);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!show) {
            return false;
        }

        if (isActive() && isHoveredOrFocused()) {
            //noinspection SuspiciousNameCombination
            setScroll(scroll - Mth.sign(scrollY));
            return true;
        }

        return false;
    }

    public void scrollTo(int index) {
        int currentStart = scroll;
        int currentEnd = scroll + maxDisplayedLines - 1;

        if (index < currentStart) {
            setScroll(index);
        } else if (index > currentEnd) {
            setScroll(index - maxDisplayedLines + 1);
        } else {
            setScroll(scroll); // Update just in case
        }
    }

    public void setScroll(int value) {
        scroll = Mth.clamp(value, 0, Math.max(0, suggestions.size() - maxDisplayedLines));
    }

    public void select(int index) {
        if (index < 0) {
            index = suggestions.size() - 1;
        } else if (index >= suggestions.size()) {
            index = 0;
        }

        selectedIndex = Mth.clamp(index, 0, suggestions.size() - 1);
        scrollTo(selectedIndex);
        getSelected().ifPresent(suggestion -> {
            String suffix = calculateSuggestionSuffix(editBox.getValue(), suggestion.getText());
            editBox.setSuggestion(suffix);
        });
    }

    protected @Nullable String calculateSuggestionSuffix(String inputText, String suggestionText) {
        return suggestionText.startsWith(inputText) ? suggestionText.substring(inputText.length()) : null;
    }

    public Optional<Suggestion> getSelected() {
        if (selectedIndex < 0 || selectedIndex >= suggestions.size()) return Optional.empty();
        return Optional.of(suggestions.get(selectedIndex));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!show) {
            return;
        }

        int hoveredSuggestion = getHoveredSuggestionIndex(mouseX, mouseY);
        if (hoveredSuggestion >= 0 && hoveredSuggestion < suggestions.size()) {
            editBox.setValue(suggestions.get(hoveredSuggestion).getText());
        }
    }

    public int getHoveredSuggestionIndex(double mouseX, double mouseY) {
        if (suggestions.isEmpty()
              || mouseX < getX() || mouseX >= getX() + getWidth()
              || mouseY < getY() || mouseY >= getY() + 11 * Math.min(maxDisplayedLines, suggestions.size())) {
            return -1;
        }

        int y = (int) (mouseY - getY());

        return Mth.clamp(y / 11 + scroll, scroll, suggestions.size() - 1);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
