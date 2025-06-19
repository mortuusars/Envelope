package io.github.mortuusars.envelope.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class PrettyGameTime {
    public static final int HOUR = 1000;
    public static final int DAY = 24000;
    public static final int WEEK = 168000;
    public static final int MONTH = 672000;

    public static Component duration(long ticks) {
        MutableComponent component = Component.empty();
        boolean needsSpace = false;

        int months = (int) (ticks / MONTH);
        if (months > 0) {
            component.append(Component.translatable("gui.envelope.time.months", months));
            needsSpace = true;
        }

        ticks -= (long) months * MONTH;

        int weeks = (int) (ticks / WEEK);
        if (weeks > 0) {
            if (needsSpace) {
                component.append(" ");
            }
            component.append(Component.translatable("gui.envelope.time.weeks", weeks));
            needsSpace = true;
        }

        ticks -= (long) weeks * WEEK;

        int days = (int) (ticks / DAY);
        if (days > 0) {
            if (needsSpace) {
                component.append(" ");
            }
            component.append(Component.translatable("gui.envelope.time.days", days));
            needsSpace = true;
        }

        ticks -= (long) days * WEEK;

        int hours = (int) (ticks / HOUR);
        if (hours > 0) {
            if (needsSpace) {
                component.append(" ");
            }
            component.append(Component.translatable("gui.envelope.time.hours", hours));
        } else if (component.getString().isEmpty()) {
            if (needsSpace) {
                component.append(" ");
            }
            component.append(Component.translatable("gui.envelope.time.less_than_one_hour"));
        }

        return component;
    }
}
