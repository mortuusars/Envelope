package io.github.mortuusars.envelope.mixin.spawners;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.mortuusars.envelope.world.entity.spawner.BackgroundCourierSpawner;
import io.github.mortuusars.envelope.world.entity.spawner.FinishedBackgroundCourierSpawner;
import io.github.mortuusars.envelope.world.entity.spawner.PigeonSpawner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.CustomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge has a special event to do this, but fabric doesn't seem to. So it's easier to do it here for both platforms.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;get(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;"))
    private void addToSpawnersList(ChunkProgressListener listener, CallbackInfo ci, @Local LocalRef<List<CustomSpawner>> spawners) {
        List<CustomSpawner> modifiedSpawnersList = new ArrayList<>(spawners.get());
        modifiedSpawnersList.add(new PigeonSpawner());
        modifiedSpawnersList.add(new BackgroundCourierSpawner());
        modifiedSpawnersList.add(new FinishedBackgroundCourierSpawner());
        spawners.set(modifiedSpawnersList);
    }
}