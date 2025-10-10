package io.github.mortuusars.envelope.compat.jei;

import io.github.mortuusars.envelope.Envelope;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class EnvelopeJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Envelope.resource("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }
}