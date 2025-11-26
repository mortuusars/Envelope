package io.github.mortuusars.envelope.util.bugger.page;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.util.bugger.JsonSyntaxHighlighter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataPage implements BuggerPage {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String getTitle() {
        return "Data";
    }

    @Override
    public List<String> getLeftLines() {
        @Nullable HitResult hitResult = Minecrft.get().hitResult;
        ArrayList<String> lines = new ArrayList<>();

        if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() != HitResult.Type.MISS) {
            BlockPos pos = blockHitResult.getBlockPos();
            Block block = Minecrft.level().getBlockState(pos).getBlock();

            lines.add(block.getName().getString());
            lines.add(BuiltInRegistries.BLOCK.getKey(block).toString());
            lines.add("");

            if (Minecrft.level().getBlockEntity(pos) instanceof BlockEntity be) {
                CompoundTag nbt = be.saveWithFullMetadata(Minecrft.level().registryAccess());
                lines.addAll(highlightAndSplitJson(CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, nbt)));
            }

            return lines;
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();

            lines.add(entity.getName().getString());
            lines.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            lines.add("");

            CompoundTag nbt = Util.make(new CompoundTag(), entity::save);
            lines.addAll(highlightAndSplitJson(CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, nbt)));

            return lines;
        } else {
            ItemStack itemInHand = getItemInHand();

            lines.add(itemInHand.getHoverName().getString());
            lines.add(BuiltInRegistries.ITEM.getKey(itemInHand.getItem()).toString());
            lines.add("");

            DataResult<JsonElement> encodeResult = ItemStack.CODEC.encodeStart(Minecrft.registryAccess()
                  .createSerializationContext(JsonOps.INSTANCE), itemInHand);
            lines.addAll(highlightAndSplitJson(encodeResult));

            return lines;
        }
    }

    protected List<String> highlightAndSplitJson(DataResult<JsonElement> encodingResult) {
        JsonElement json = encodingResult.result().orElse(new JsonObject());
        String jsonString = JsonSyntaxHighlighter.highlight(GSON.toJson(json));
        return Arrays.asList(jsonString.split("\n"));
    }

    @Override
    public List<String> getRightLines() {
        @Nullable HitResult hitResult = Minecrft.get().hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() != HitResult.Type.MISS) {
            return Minecrft.level().getBlockState(blockHitResult.getBlockPos()).getTags().map(key -> "#" + key.location()).toList();
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            //noinspection deprecation
            return entityHitResult.getEntity().getType().builtInRegistryHolder().tags().map(key -> "#" + key.location()).toList();
        } else {
            return getItemInHand().getTags().map(key -> "#" + key.location()).toList();
        }
    }

    private ItemStack getItemInHand() {
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        return mainHandItem.isEmpty() ? player.getItemInHand(InteractionHand.OFF_HAND) : mainHandItem;
    }
}
