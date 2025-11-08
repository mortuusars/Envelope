package io.github.mortuusars.envelope.world.entity;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;

public record SpawnableEntityData(Either<CustomData, EntityType<?>> dataOrType) {
    public static final Codec<SpawnableEntityData> CODEC = Codec.either(
                CustomData.CODEC.validate(data -> {
                    if (data.isEmpty()) return DataResult.error(() -> "Entity data cannot be empty.");
                    return DataResult.success(data);
                }),
                BuiltInRegistries.ENTITY_TYPE.byNameCodec())
          .xmap(SpawnableEntityData::new, SpawnableEntityData::dataOrType);

    private static final Logger LOGGER = LogUtils.getLogger();

    public SpawnableEntityData(CustomData data) {
        this(Either.left(data));
        Preconditions.checkArgument(!data.isEmpty(), "Entity data cannot be empty.");
    }

    public SpawnableEntityData(EntityType<?> type) {
        this(Either.right(type));
    }

    public static SpawnableEntityData of(Entity entity, List<String> ignoredTags) {
        Preconditions.checkArgument(!entity.isPassenger() && !entity.isRemoved() && entity.getType().canSerialize(),
              "Cannot create SpawnableEntityData: entity '" + entity + "' is passenger, removed or the type is not serializable.");

        CompoundTag tag = new CompoundTag();

        if (!entity.save(tag)) {
            LOGGER.error("Failed to save entity '{}' to a tag. Entity is passenger, " +
                  "about to be removed or entity type is not serializable.", entity);
        }

        ignoredTags.forEach(tag::remove);

        Preconditions.checkState(!tag.isEmpty(), "Entity tag cannot be empty.");
        Preconditions.checkState(tag.contains("id", Tag.TAG_STRING), "Entity tag does not contain an 'id': " + tag);

        return new SpawnableEntityData(CustomData.of(tag));
    }

    // --

    public boolean isNew() {
        return dataOrType().right().isPresent();
    }

    @SuppressWarnings("deprecation") // We do not modify the tag returned by CustomData#getUnsafe
    public @Nullable Entity createEntity(ServerLevel level) {
        return dataOrType.map(
              data -> EntityType.loadEntityRecursive(data.getUnsafe(), level, Function.identity()),
              type -> type.create(level)
        );
    }
}
