package io.github.mortuusars.envelope.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.Register;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatFormatter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegisterImpl {
    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.BLOCK, Envelope.resource(id), supplier.get());
        return () -> obj;
    }

    public static <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Envelope.resource(id), supplier.get());
        return () -> obj;
    }

    public static <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(Register.BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks) {
        return BlockEntityType.Builder.of(blockEntitySupplier::create, validBlocks).build();
    }

    public static Supplier<PoiType> poiType(ResourceKey<PoiType> key, int ticketCount, int searchDistance, Supplier<Set<BlockState>> states) {
        PoiType type = PointOfInterestHelper.register(key.location(), ticketCount, searchDistance, states.get());
        return () -> type;
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.ITEM, Envelope.resource(id), supplier.get());
        return () -> obj;
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        MobCategory category, float width, float height,
                                                                        int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, Envelope.resource(id),
                EntityType.Builder.of(factory, category)
                        .sized(width, height)
                        .clientTrackingRange(clientTrackingRange)
                        .alwaysUpdateVelocity(velocityUpdates)
                        .updateInterval(updateInterval)
                        .build());
        return () -> type;
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory, MobCategory category, boolean receiveVelocityUpdates, Consumer<EntityType.Builder<T>> typeBuilder) {
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category);
        typeBuilder.accept(builder);
        builder.alwaysUpdateVelocity(receiveVelocityUpdates);
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, Envelope.resource(id), builder.build());
        return () -> type;
    }

    public static void entityDataSerializer(String id, EntityDataSerializer<?> serializer) {
        EntityDataSerializers.registerSerializer(serializer);
    }

    public static <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.SOUND_EVENT, Envelope.resource(id), supplier.get());
        return () -> obj;
    }

    public static <T extends MenuType<E>, E extends AbstractContainerMenu> Supplier<MenuType<E>> menuType(String id, Register.MenuTypeSupplier<E> supplier) {
        ExtendedScreenHandlerType<E, byte[]> type = new ExtendedScreenHandlerType<>((syncId, inventory, data) -> {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inventory.player.registryAccess());
            E menu = supplier.create(syncId, inventory, buffer);
            buffer.release();
            return menu;
        }, ByteBufCodecs.BYTE_ARRAY.mapStream(Function.identity()));

        Registry.register(BuiltInRegistries.MENU, Envelope.resource(id), type);

        return () -> {
            return type;
        };
    }

    public static Supplier<RecipeType<?>> recipeType(String id, Supplier<RecipeType<?>> supplier) {
        RecipeType<?> obj = Registry.register(BuiltInRegistries.RECIPE_TYPE, Envelope.resource(id), supplier.get());
        return () -> obj;
    }

    public static Supplier<RecipeSerializer<?>> recipeSerializer(String id, Supplier<RecipeSerializer<?>> supplier) {
        RecipeSerializer<?> obj = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Envelope.resource(id), supplier.get());
        return () -> obj;
    }

    public static <T extends CriterionTrigger<?>> Supplier<T> criterionTrigger(String name, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.TRIGGER_TYPES, Envelope.resource(name), supplier.get());
        return () -> obj;
    }

    public static <T extends ItemSubPredicate.Type<?>> Supplier<T> itemSubPredicate(String name, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE, Envelope.resource(name), supplier.get());
        return () -> obj;
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>>
    Supplier<ArgumentTypeInfo<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        ArgumentTypeRegistry.registerArgumentType(Envelope.resource(id), infoClass, argumentTypeInfo);
        return () -> argumentTypeInfo;
    }

    public static <T extends FeatureConfiguration> Supplier<Feature<?>> worldGenFeature(String name, Supplier<Feature<T>> featureSupplier) {
        Feature<T> feature = Registry.register(BuiltInRegistries.FEATURE, name, featureSupplier.get());
        return () -> feature;
    }

    public static <T> DataComponentType<T> dataComponentType(String name, Consumer<DataComponentType.Builder<T>> builderConsumer) {
        var builder = DataComponentType.<T>builder();
        builderConsumer.accept(builder);
        var componentType = builder.build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Envelope.resource(name), componentType);
    }

    public static <T extends ParticleType<? extends ParticleOptions>> Supplier<T> particleType(String name, Supplier<T> supplier) {
        T particleType = Registry.register(BuiltInRegistries.PARTICLE_TYPE, name, supplier.get());
        return () -> particleType;
    }

    public static Supplier<ResourceLocation> stat(ResourceLocation location, StatFormatter formatter) {
        net.minecraft.core.Registry.register(BuiltInRegistries.CUSTOM_STAT, location, location);
        net.minecraft.stats.Stats.CUSTOM.get(location, formatter);
        return () -> location;
    }
}
