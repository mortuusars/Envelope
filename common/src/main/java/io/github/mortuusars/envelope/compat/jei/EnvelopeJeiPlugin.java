package io.github.mortuusars.envelope.compat.jei;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.screen.PigeonholeAddressScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class EnvelopeJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Envelope.resource("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Hides JEI side panels
        registration.addGenericGuiContainerHandler(PigeonholeAddressScreen.class, new IGuiContainerHandler<PigeonholeAddressScreen>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull PigeonholeAddressScreen containerScreen) {
                return List.of(new Rect2i(0, 0,
                        Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                        Minecraft.getInstance().getWindow().getGuiScaledHeight()));
            }
        });
    }
}