package io.github.mortuusars.envelope.integration.sable;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.integration.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional compatibility with Sable moving structures.
 * <p>
 * Projects local plot-grid coordinates into shared world space for navigation and distance checks.
 * <p>
 * This uses reflection against {@code dev.ryanhcode.sable.Sable.HELPER} rather than a compile-time
 * dependency on Sable or sable-companion. That keeps Envelope buildable without Sable present, but
 * method signatures on Sable's side can change between releases. Long term, consider JiJ'ing
 * <a href="https://github.com/ryanhcode/sable-companion">sable-companion</a> for a stable API surface.
 */
public final class MovingStructureCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SABLE_CLASS = "dev.ryanhcode.sable.Sable";
    private static final String PROJECT_METHOD = "projectOutOfSubLevel";

    private static volatile boolean initializationAttempted = false;
    private static volatile boolean initializationFailed = false;
    private static volatile boolean failureLogged = false;
    private static @Nullable Object helperInstance;
    private static @Nullable Method projectOutOfSubLevelMethod;

    private MovingStructureCompat() {
    }

    public static boolean isAvailable() {
        return Mods.SABLE.isLoaded() && ensureInitialized();
    }

    public static Vec3 projectOutOfSubLevel(@Nullable Level level, Vec3 pos) {
        if (level == null || pos == null || !Mods.SABLE.isLoaded()) {
            return pos;
        }

        if (!ensureInitialized()) {
            return pos;
        }

        try {
            Object projected = projectOutOfSubLevelMethod.invoke(helperInstance, level, pos);
            if (projected instanceof Vec3 vec3) {
                return vec3;
            }
            LOGGER.debug("Sable projection returned a non-Vec3 value; using the original position.");
        } catch (ReflectiveOperationException e) {
            logProjectionFailure("Failed to project a position out of a Sable sub-level.", e);
        }

        return pos;
    }

    public static Vec3 getGlobalCenter(@Nullable Level level, BlockPos pos) {
        return projectOutOfSubLevel(level, Vec3.atCenterOf(pos));
    }

    public static BlockPos getNavigationPos(@Nullable Level level, BlockPos localPos) {
        return BlockPos.containing(getGlobalCenter(level, localPos));
    }

    private static boolean ensureInitialized() {
        if (projectOutOfSubLevelMethod != null && helperInstance != null) {
            return true;
        }

        if (initializationAttempted) {
            return !initializationFailed;
        }

        synchronized (MovingStructureCompat.class) {
            if (projectOutOfSubLevelMethod != null && helperInstance != null) {
                return true;
            }
            if (initializationAttempted) {
                return !initializationFailed;
            }

            initializationAttempted = true;
            try {
                Class<?> sableClass = Class.forName(SABLE_CLASS);
                Field helperField = sableClass.getField("HELPER");
                helperInstance = helperField.get(null);
                projectOutOfSubLevelMethod = helperInstance.getClass()
                      .getMethod(PROJECT_METHOD, Level.class, net.minecraft.core.Position.class);
                return true;
            } catch (ReflectiveOperationException e) {
                initializationFailed = true;
                logInitializationFailure("Failed to initialize Sable compatibility hooks.", e);
                return false;
            }
        }
    }

    private static void logProjectionFailure(String message, ReflectiveOperationException e) {
        LOGGER.warn(message, e);
    }

    private static void logInitializationFailure(String message, ReflectiveOperationException e) {
        if (failureLogged) {
            LOGGER.debug(message, e);
        } else {
            failureLogged = true;
            LOGGER.warn(message, e);
        }
    }
}
