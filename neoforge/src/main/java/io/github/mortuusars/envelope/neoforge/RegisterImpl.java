package io.github.mortuusars.envelope.neoforge;

import com.mojang.brigadier.arguments.ArgumentType;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.Register;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataSerializer;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegisterImpl {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Envelope.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Envelope.ID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Envelope.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Envelope.ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Envelope.ID);
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, Envelope.ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Envelope.ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Envelope.ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Envelope.ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Envelope.ID);
    public static final DeferredRegister<CriterionTrigger<?>> CRITERION_TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Envelope.ID);
    public static final DeferredRegister<ItemSubPredicate.Type<?>> ITEM_SUB_PREDICATES = DeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE, Envelope.ID);
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Envelope.ID);
    public static final DeferredRegister<Feature<?>> WORLD_GEN_FEATURES = DeferredRegister.create(Registries.FEATURE, Envelope.ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Envelope.ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Envelope.ID);
    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(Registries.CUSTOM_STAT, Envelope.ID);
    public static final Map<ResourceLocation, StatFormatter> STATS = new HashMap<>();

    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        return BLOCKS.register(id, supplier);
    }

    public static <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> sup) {
        return BLOCK_ENTITY_TYPES.register(id, sup);
    }

    public static Supplier<PoiType> poiType(ResourceKey<PoiType> key, int ticketCount, int searchDistance, Supplier<Set<BlockState>> states) {
        return POI_TYPES.register(key.location().getPath(), () -> new PoiType(states.get(), ticketCount, searchDistance));
    }

    public static <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(Register.BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks) {
        return BlockEntityType.Builder.of(blockEntitySupplier::create, validBlocks).build(null);
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        return ITEMS.register(id, supplier);
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory, MobCategory category,
                                                                        float width, float height, int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(clientTrackingRange)
                .setShouldReceiveVelocityUpdates(velocityUpdates)
                .updateInterval(updateInterval)
                .build(id));
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory, MobCategory category, boolean receiveVelocityUpdates, Consumer<EntityType.Builder<T>> typeBuilder) {
        return ENTITY_TYPES.register(id, () -> {
            EntityType.Builder<T> builder = EntityType.Builder.of(factory, category);
            builder.setShouldReceiveVelocityUpdates(receiveVelocityUpdates);
            typeBuilder.accept(builder);
            return builder.build(id);
        });
    }

    public static void entityDataSerializer(String id, EntityDataSerializer<?> serializer) {
        ENTITY_DATA_SERIALIZERS.register(id, () -> serializer);
    }

    public static <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        return SOUND_EVENTS.register(id, supplier);
    }

    public static <T extends MenuType<E>, E extends AbstractContainerMenu> Supplier<MenuType<E>> menuType(String id, Register.MenuTypeSupplier<E> supplier) {
        return MENU_TYPES.register(id, () -> IMenuTypeExtension.create(supplier::create));
    }

    public static Supplier<RecipeType<?>> recipeType(String id, Supplier<RecipeType<?>> supplier) {
        return RECIPE_TYPES.register(id, supplier);
    }

    public static Supplier<RecipeSerializer<?>> recipeSerializer(String id, Supplier<RecipeSerializer<?>> supplier) {
        return RECIPE_SERIALIZERS.register(id, supplier);
    }

    public static <T extends CriterionTrigger<?>> Supplier<T> criterionTrigger(String name, Supplier<T> supplier) {
        return CRITERION_TRIGGERS.register(name, supplier);
    }

    public static <T extends ItemSubPredicate.Type<?>> Supplier<T> itemSubPredicate(String name, Supplier<T> supplier) {
        return ITEM_SUB_PREDICATES.register(name, supplier);
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>>
    Supplier<ArgumentTypeInfo<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        return COMMAND_ARGUMENT_TYPES.register(id,
                () -> ArgumentTypeInfos.registerByClass(infoClass, argumentTypeInfo));
    }

    public static <T extends FeatureConfiguration> Supplier<Feature<?>> worldGenFeature(String name, Supplier<Feature<T>> featureSupplier) {
        return WORLD_GEN_FEATURES.register(name, featureSupplier);
    }

    public static <T> DataComponentType<T> dataComponentType(String name, Consumer<DataComponentType.Builder<T>> builderConsumer) {
        var builder = DataComponentType.<T>builder();
        builderConsumer.accept(builder);
        var componentType = builder.build();
        DATA_COMPONENT_TYPES.register(name, () -> componentType);
        return componentType;
    }

    public static <T extends ParticleType<? extends ParticleOptions>> Supplier<ParticleType<?>> particleType(String name, Supplier<T> supplier) {
        return PARTICLE_TYPES.register(name, supplier);
    }

    public static Supplier<ResourceLocation> stat(ResourceLocation location, StatFormatter formatter) {
        STATS.put(location, formatter);
        return CUSTOM_STATS.register(location.getPath(), () -> location);
    }
}
