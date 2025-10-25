package io.github.mortuusars.envelope.util.bugger.page;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.util.bugger.JsonSyntaxHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DataPage implements BuggerPage {
    @Override
    public String getTitle() {
        return "Data";
    }

    @Override
    public List<String> getLeftLines() {
        @Nullable HitResult hitResult = Minecrft.get().hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            ItemStack itemInHand = getItemInHand();

            if (itemInHand.isEmpty()) {
                return List.of("Air");
            }

            JsonElement json = ItemStack.CODEC.encodeStart(Minecrft.registryAccess().createSerializationContext(JsonOps.INSTANCE), itemInHand).result().orElse(new JsonObject());
            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

            jsonString = JsonSyntaxHighlighter.highlight(jsonString);

            List<String> lines = new ArrayList<>(Arrays.stream(jsonString.split("\n")).toList());
            lines.addFirst("");
            lines.addFirst(itemInHand.getHoverName().getString());
            return lines;
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            @Nullable BlockEntity blockEntity = Minecrft.level().getBlockEntity(blockPos);
            if (blockEntity != null) {
                CompoundTag beTag = blockEntity.saveWithFullMetadata(Minecrft.level().registryAccess());
                JsonElement json = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, beTag).result().orElse(new JsonObject());

                String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

                jsonString = JsonSyntaxHighlighter.highlight(jsonString);

                List<String> lines = new ArrayList<>(Arrays.stream(jsonString.split("\n")).toList());
                lines.addFirst("");
                lines.addFirst(blockEntity.getBlockState().getBlock().getName().getString());
                return lines;
            } else {
                return List.of(Minecrft.level().getBlockState(blockPos).getBlock().getName().getString());
            }
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();

            CompoundTag entityTag = new CompoundTag();
            entity.save(entityTag);

            JsonElement json = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, entityTag).result().orElse(new JsonObject());

            String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(json);

            jsonString = JsonSyntaxHighlighter.highlight(jsonString);

            List<String> lines = new ArrayList<>(Arrays.stream(jsonString.split("\n")).toList());
            lines.addFirst("");
            lines.addFirst(entity.getName().getString());
            return lines;
        }

        return Collections.emptyList();
    }

    private ItemStack getItemInHand() {
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        return mainHandItem.isEmpty() ? player.getItemInHand(InteractionHand.OFF_HAND) : mainHandItem;
    }
}
