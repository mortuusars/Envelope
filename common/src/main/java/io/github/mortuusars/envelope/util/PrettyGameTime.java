package io.github.mortuusars.envelope.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public class PrettyGameTime {
    public static final long GAME_HOUR = 1000;
    public static final long GAME_DAY = 24000;
    public static final long GAME_WEEK = 168000;
    public static final long GAME_MONTH = 672000;

    public static final long SECOND = 20;
    public static final long MINUTE = 1200;
    public static final long HOUR = 72000;
    public static final long DAY = 1728000;
    public static final long WEEK = 12096000;
    public static final long MONTH = 362880000;

    public static MutableComponent gameDuration(long ticks) {
        MutableComponent component = Component.empty();
        boolean needsSpace = false;

        int months = (int) (ticks / GAME_MONTH);
        if (months > 0) {
            component.append(Component.translatable("gui.envelope.time.months", months));
            needsSpace = true;
        }

        ticks -= (long) months * GAME_MONTH;

        int weeks = (int) (ticks / GAME_WEEK);
        if (weeks > 0) {
            if (needsSpace) {
                component.append(" ");
            }
            component.append(Component.translatable("gui.envelope.time.weeks", weeks));
            needsSpace = true;
        }

        ticks -= (long) weeks * GAME_WEEK;

        int days = (int) (ticks / GAME_DAY);
        if (days > 0) {
            if (needsSpace) {
                component.append(" ");
            }
            component.append(Component.translatable("gui.envelope.time.days", days));
            needsSpace = true;
        }

        ticks -= (long) days * GAME_WEEK;

        int hours = (int) (ticks / GAME_HOUR);
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

    public static MutableComponent duration(long ticks) {
        long secondsTotal = ticks / 20;

        long years = secondsTotal / 31104000;
        secondsTotal %= 31104000;
        long months = secondsTotal / 2592000;
        secondsTotal %= 2592000;
        long weeks = secondsTotal / 604800;
        secondsTotal %= 604800;
        long days = secondsTotal / 86400;
        secondsTotal %= 86400;
        long hours = secondsTotal / 3600;
        secondsTotal %= 3600;
        long minutes = secondsTotal / 60;

        List<Component> parts = new ArrayList<>();

        if (years > 0) parts.add(Component.translatable("gui.envelope.time.years", years));
        if (months > 0) parts.add(Component.translatable("gui.envelope.time.months", months));
        if (weeks > 0) parts.add(Component.translatable("gui.envelope.time.weeks", weeks));
        if (days > 0) parts.add(Component.translatable("gui.envelope.time.days", days));
        if (hours > 0) parts.add(Component.translatable("gui.envelope.time.hours", hours));
        if (minutes > 0) parts.add(Component.translatable("gui.envelope.time.minutes", minutes));
        if (parts.isEmpty()) parts.add(Component.translatable("gui.envelope.time.less_than_one_minute"));

        MutableComponent result = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) result.append(" ");
            result.append(parts.get(i));
        }

        return result;
    }

    public static MutableComponent durationLargest(long ticks) {
        long secondsTotal = ticks / 20;

        long years = secondsTotal / 31104000;
        if (years > 0) return Component.translatable("gui.envelope.time.years", years);
        secondsTotal %= 31104000;
        long months = secondsTotal / 2592000;
        if (months > 0) return Component.translatable("gui.envelope.time.months", months);
        secondsTotal %= 2592000;
        long weeks = secondsTotal / 604800;
        if (weeks > 0) return Component.translatable("gui.envelope.time.weeks", weeks);
        secondsTotal %= 604800;
        long days = secondsTotal / 86400;
        if (days > 0) return Component.translatable("gui.envelope.time.days", days);
        secondsTotal %= 86400;
        long hours = secondsTotal / 3600;
        if (hours > 0) return Component.translatable("gui.envelope.time.hours", hours);
        secondsTotal %= 3600;
        long minutes = secondsTotal / 60;
        if (minutes > 0) return Component.translatable("gui.envelope.time.minutes", minutes);
        else return Component.translatable("gui.envelope.time.less_than_one_minute");
    }
}
