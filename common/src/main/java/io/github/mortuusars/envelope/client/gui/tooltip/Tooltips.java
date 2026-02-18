package io.github.mortuusars.envelope.client.gui.tooltip;

import com.google.common.base.Preconditions;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Tooltips {
    public static <T> Map<T, Tooltip> createMap(List<T> values, Function<T, Component> convertFunc) {
        Preconditions.checkArgument(!values.isEmpty(), "values list must not be empty.");
        Map<T, Tooltip> map = new HashMap<>();
        for (T value : values) {
            map.put(value, Tooltip.create(convertFunc.apply(value)));
        }
        return map;
    }
}
