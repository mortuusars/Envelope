package io.github.mortuusars.envelope;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.command.argument.AddressArgument;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.item.*;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.item.crafting.LetterCloningRecipe;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.util.DeferredSoundType;
import io.github.mortuusars.envelope.world.block.*;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PackageMenu;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Envelope {
    public static final String ID = "envelope";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        Bugger.enabler = () -> Config.Server.SPEC.isLoaded() && Config.Server.DEBUG.get();

        Blocks.init();
        BlockEntityTypes.init();
        PoiTypes.init();
        EntityTypes.init();
        Items.init();
        DataComponents.init();
        Stats.init();
        CriteriaTriggers.init();
        ItemSubPredicates.init();
        MenuTypes.init();
        RecipeSerializers.init();
        SoundEvents.init();
        ArgumentTypes.init();
    }

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static boolean debug() {
        return Config.Server.DEBUG.get();
    }

    public static class Blocks {
        public static final Map<ResourceLocation, Supplier<PigeonholeBlock>> PIGEONHOLES = new HashMap<>();

        public static final Supplier<PigeonholeBlock> OAK_PIGEONHOLE = pigeonhole("oak", MapColor.WOOD);

        public static final Supplier<PaperBoxBlock> PAPER_BOX = Register.block("paper_box",
              () -> new PaperBoxBlock(BlockBehaviour.Properties.of()
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava()
                    .strength(0.3f)
                    .sound(SoundTypes.PAPER)
                    .mapColor(MapColor.SAND)
              ));

        public static final Supplier<PackageBlock> PACKAGE = Register.block("package",
              () -> new PackageBlock(BlockBehaviour.Properties.of()
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava()
                    .strength(0.4f)
                    .sound(SoundTypes.PAPER)
                    .mapColor(MapColor.SAND)
                    .noOcclusion()));

        public static final Supplier<PackageBlock> SEALED_PACKAGE = Register.block("sealed_package",
              () -> new PackageBlock(BlockBehaviour.Properties.of()
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava()
                    .strength(0.4f)
                    .sound(SoundTypes.PAPER)
                    .mapColor(MapColor.SAND)
                    .noOcclusion()));

        private static Supplier<PigeonholeBlock> pigeonhole(String type, MapColor color) {
            String id = type + "_pigeonhole";
            Supplier<PigeonholeBlock> block = Register.block(id,
                  () -> new PigeonholeBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BEEHIVE)
                        .strength(2f)
                        .mapColor(color)));
            PIGEONHOLES.put(Envelope.resource(id), block);
            return block;
        }

        static void init() {
        }
    }

    public static class BlockEntityTypes {
        public static final Supplier<BlockEntityType<PigeonholeBlockEntity>> PIGEONHOLE =
              Register.blockEntityType("pigeonhole", () -> Register.newBlockEntityType(
                    PigeonholeBlockEntity::new, getPigeonholeBlocks()));

        public static final Supplier<BlockEntityType<PackageBlockEntity>> PACKAGE =
              Register.blockEntityType("package", () -> Register.newBlockEntityType(
                    PackageBlockEntity::new, Blocks.PACKAGE.get(), Blocks.SEALED_PACKAGE.get()));

        private static PigeonholeBlock[] getPigeonholeBlocks() {
            return Blocks.PIGEONHOLES.values().stream().map(Supplier::get).toArray(PigeonholeBlock[]::new);
        }

        static void init() {
        }
    }

    public static class PoiTypes {
        public static final ResourceKey<PoiType> PIGEONHOLE =
              ResourceKey.create(net.minecraft.core.registries.Registries.POINT_OF_INTEREST_TYPE, resource("pigeonhole"));

        static void init() {
            Register.poiType(PIGEONHOLE, 0, 1, PoiTypes::getPigeonholePoiBlockStates);
        }

        private static Set<BlockState> getPigeonholePoiBlockStates() {
            return Blocks.PIGEONHOLES.values().stream()
                  .map(Supplier::get)
                  .map(b -> b.getStateDefinition().getPossibleStates())
                  .flatMap(Collection::stream)
                  .collect(Collectors.toSet());
        }
    }

    public static class Items {
        public static final List<Supplier<BlockItem>> PIGEONHOLES = new ArrayList<>();

        public static final Supplier<BlockItem> OAK_PIGEONHOLE = pigeonhole("oak", Blocks.OAK_PIGEONHOLE);

        public static final Supplier<AddressTagItem> ADDRESS_TAG = Register.item("address_tag",
              () -> new AddressTagItem(new Item.Properties()));
        public static final Supplier<SealStampItem> SEAL_STAMP = Register.item("seal_stamp",
              () -> new SealStampItem(new Item.Properties().stacksTo(1)));

        public static final Supplier<LetterAndQuillItem> LETTER_AND_QUILL = Register.item("letter_and_quill",
              () -> new LetterAndQuillItem(new Item.Properties().stacksTo(1)));
        public static final Supplier<LetterItem> LETTER = Register.item("letter",
              () -> new LetterItem(new Item.Properties()));
        public static final Supplier<SealedLetterItem> SEALED_LETTER = Register.item("sealed_letter",
              () -> new SealedLetterItem(new Item.Properties()));

        public static final Supplier<BlockItem> PAPER_BOX = Register.item("paper_box",
              () -> new BlockItem(Blocks.PAPER_BOX.get(), new Item.Properties().stacksTo(16)));
        public static final Supplier<PackageItem> PACKAGE = Register.item("package",
              () -> new PackageItem(Blocks.PACKAGE.get(), new Item.Properties().stacksTo(1)));
        public static final Supplier<SealedPackageItem> SEALED_PACKAGE = Register.item("sealed_package",
              () -> new SealedPackageItem(Blocks.SEALED_PACKAGE.get(), new Item.Properties().stacksTo(1)));

        public static final Supplier<SpawnEggItem> PIGEON_SPAWN_EGG = Register.item("pigeon_spawn_egg",
              () -> new SpawnEggItem(EntityTypes.PIGEON.get(), 0x676781, 0xB8B8CB, new Item.Properties()));

        private static @NotNull Supplier<BlockItem> pigeonhole(String type, Supplier<PigeonholeBlock> block) {
            Supplier<BlockItem> item = Register.item(type + "_pigeonhole", () -> new BlockItem(block.get(), new Item.Properties()));
            PIGEONHOLES.add(item);
            return item;
        }

        static void init() {
        }
    }

    public static class DataComponents {
        public static final DataComponentType<Address> ADDRESS = Register.dataComponentType("address", b ->
              b.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));

        public static final DataComponentType<Address> MAIL_SENDER = Register.dataComponentType("mail_sender", b ->
              b.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        public static final DataComponentType<Address> MAIL_RECIPIENT = Register.dataComponentType("mail_recipient", b ->
              b.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));

        public static final DataComponentType<LetterAndQuillContent> LETTER_AND_QUILL_CONTENT =
              Register.dataComponentType("letter_and_quill_content", b ->
                    b.persistent(LetterAndQuillContent.CODEC).networkSynchronized(LetterAndQuillContent.STREAM_CODEC));
        public static final DataComponentType<LetterContent> LETTER_CONTENT =
              Register.dataComponentType("letter_content", b ->
                    b.persistent(LetterContent.CODEC).networkSynchronized(LetterContent.STREAM_CODEC));
        public static final DataComponentType<Unit> LETTER_TATTERED =
              Register.dataComponentType("letter_tattered", b ->
                    b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

        public static final DataComponentType<PackageContents> PACKAGE_CONTENTS = Register.dataComponentType("package_contents",
              b -> b.persistent(PackageContents.CODEC).networkSynchronized(PackageContents.STREAM_CODEC));
        public static final DataComponentType<Integer> PACKAGE_TIMES_PACKED = Register.dataComponentType("package_times_packed",
              b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        public static final DataComponentType<Seal> SEAL = Register.dataComponentType("seal",
              b -> b.persistent(Seal.CODEC).networkSynchronized(Seal.STREAM_CODEC));
        public static final DataComponentType<SealImpression> SEAL_IMPRESSION = Register.dataComponentType("seal_impression",
              b -> b.persistent(SealImpression.CODEC).networkSynchronized(SealImpression.STREAM_CODEC));

        public static final DataComponentType<List<Occupant>> PIGEONS = Register.dataComponentType("pigeons", b ->
              b.persistent(Occupant.LIST_CODEC).networkSynchronized(Occupant.STREAM_CODEC.apply(ByteBufCodecs.list())).cacheEncoding());

        static void init() {
        }
    }

    public static class EntityTypes {
        public static final Supplier<EntityType<Pigeon>> PIGEON = Register.entityType("pigeon",
              Pigeon::new, MobCategory.CREATURE, true, builder -> builder
                    .sized(0.65F, 0.85F)
                    .eyeHeight(0.59375F)
                    .passengerAttachments(0.4625F)
                    .clientTrackingRange(8));

        static void init() {
        }
    }

    public static class MenuTypes {
        public static final Supplier<MenuType<PigeonholeMenu>> PIGEONHOLE =
              Register.menuType("pigeonhole", PigeonholeMenu::fromNetwork);

        public static final Supplier<MenuType<PackageMenu>> PACKAGE =
              Register.menuType("package", PackageMenu::fromNetwork);

        static void init() {
        }
    }

    public static class RecipeSerializers {
        public static final Supplier<RecipeSerializer<?>> LETTER_CLONING = Register.recipeSerializer(
              "crafting_special_letter_cloning", () -> new SimpleCraftingRecipeSerializer<>(LetterCloningRecipe::new));

        static void init() {
        }
    }

    public static class SoundEvents {
        public static final Supplier<SoundEvent> PAPER_TEAR = register("item", "paper.tear");
        public static final Supplier<SoundEvent> PAPER_CRACKLE = register("item", "paper.crackle");
        public static final Supplier<SoundEvent> PAPER_PLACE = register("block", "paper.place");
        public static final Supplier<SoundEvent> PAPER_BREAK = register("block", "paper.break");
        public static final Supplier<SoundEvent> PAPER_HIT = register("block", "paper.hit");
        public static final Supplier<SoundEvent> PAPER_FALL = register("block", "paper.fall");
        public static final Supplier<SoundEvent> PAPER_STEP = register("block", "paper.step");
        public static final Supplier<SoundEvent> PAPER_USE = register("block", "paper.use");

        public static final Supplier<SoundEvent> PIGEON_AMBIENT = register("entity", "pigeon.ambient");
        public static final Supplier<SoundEvent> PIGEON_DEATH = register("entity", "pigeon.death");
        public static final Supplier<SoundEvent> PIGEON_EAT = register("entity", "pigeon.eat");
        public static final Supplier<SoundEvent> PIGEON_FLY = register("entity", "pigeon.fly");
        public static final Supplier<SoundEvent> PIGEON_HURT = register("entity", "pigeon.hurt");
        public static final Supplier<SoundEvent> PIGEON_STEP = register("entity", "pigeon.step");

        private static Supplier<SoundEvent> register(String category, String key) {
            Preconditions.checkState(category != null && !category.isEmpty(), "'category' should not be empty.");
            Preconditions.checkState(key != null && !key.isEmpty(), "'key' should not be empty.");
            String path = category + "." + key;
            return Register.soundEvent(path, () -> SoundEvent.createVariableRangeEvent(Envelope.resource(path)));
        }

        static void init() {
        }
    }

    public static class SoundTypes {
        public static final SoundType PAPER = new DeferredSoundType(1, 1,
              SoundEvents.PAPER_BREAK,
              SoundEvents.PAPER_STEP,
              SoundEvents.PAPER_PLACE,
              SoundEvents.PAPER_HIT,
              SoundEvents.PAPER_FALL);
    }

    public static class Stats {
        public static void init() {
        }
    }

    public static class CriteriaTriggers {
        public static void init() {
        }
    }

    public static class ItemSubPredicates {
        public static void init() {
        }
    }

    public static class LootTables {
    }

    public static class Tags {
        public static class Blocks {
            public static final TagKey<Block> PIGEON_SPAWNABLE_ON =
                  TagKey.create(net.minecraft.core.registries.Registries.BLOCK, resource("pigeon_spawnable_on"));
            public static final TagKey<Block> PIGEONHOLES =
                  TagKey.create(net.minecraft.core.registries.Registries.BLOCK, resource("pigeonholes"));
        }

        public static class Items {
            public static final TagKey<Item> PIGEONHOLES =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("pigeonholes"));
            public static final TagKey<Item> PIGEON_FOOD =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("pigeon_food"));
            public static final TagKey<Item> WASTE_SCOOPABLE =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("waste_scoopable"));
            public static final TagKey<Item> MAILABLE =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("mailable"));
            public static final TagKey<Item> CANNOT_BE_PACKAGED =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("cannot_be_packaged"));
            public static final TagKey<Item> LETTERS =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("letters"));
            public static final TagKey<Item> PACKAGES =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("packages"));
        }

        public static class EntityTypes {
            public static final TagKey<EntityType<?>> PIGEONHOLE_INHABITORS =
                  TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, resource("pigeonhole_inhabitors"));
        }

        public static class Biomes {
            public static final TagKey<Biome> ALLOWS_PIGEON_SPAWNS =
                  TagKey.create(net.minecraft.core.registries.Registries.BIOME, resource("allows_pigeon_spawns"));

        }

        public static class Structures {
            public static final TagKey<Structure> PIGEONS_SPAWN_IN =
                  TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, resource("pigeons_spawn_in"));
        }
    }

    public static class ArgumentTypes {
        public static final Supplier<ArgumentTypeInfo<AddressArgument, SingletonArgumentInfo<AddressArgument>.Template>> ADDRESS =
              Register.commandArgumentType("address", AddressArgument.class, SingletonArgumentInfo.contextFree(AddressArgument::all));

        public static void init() {
        }
    }

    public static class Registries {
        public static final ResourceKey<Registry<SealMaterial>> SEAL_MATERIALS =
              ResourceKey.createRegistryKey(resource("seal_materials"));
    }
}
