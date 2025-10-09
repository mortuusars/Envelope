package io.github.mortuusars.envelope;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.world.block.PackageBlock;
import io.github.mortuusars.envelope.world.block.PackageBlockEntity;
import io.github.mortuusars.envelope.world.block.PigeonholeBlock;
import io.github.mortuusars.envelope.world.block.PigeonholeBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.PackageMenu;
import io.github.mortuusars.envelope.world.inventory.PigeonholeAddressTagMenu;
import io.github.mortuusars.envelope.world.inventory.PigeonholeMenu;
import io.github.mortuusars.envelope.world.item.CardboardBoxItem;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.PackageItem;
import io.github.mortuusars.envelope.world.item.component.MailId;
import io.github.mortuusars.envelope.world.item.AddressTagItem;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import io.github.mortuusars.envelope.world.item.component.StoredItemStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
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

    public static class Blocks {
        public static final Map<ResourceLocation, Supplier<PigeonholeBlock>> PIGEONHOLES = new HashMap<>();

        public static final Supplier<PigeonholeBlock> OAK_PIGEONHOLE = pigeonhole("oak");
        public static final Supplier<PigeonholeBlock> SPRUCE_PIGEONHOLE = pigeonhole("spruce");
        public static final Supplier<PigeonholeBlock> BIRCH_PIGEONHOLE = pigeonhole("birch");

        public static final Supplier<PackageBlock> PACKAGE = Register.block("package",
                () -> new PackageBlock(BlockBehaviour.Properties.of()
                        .pushReaction(PushReaction.DESTROY)
                        .ignitedByLava()
                        .strength(0.5f)
                        .mapColor(MapColor.SAND)
                        .noOcclusion()));

        private static Supplier<PigeonholeBlock> pigeonhole(String type) {
            String id = type + "_pigeonhole";
            Supplier<PigeonholeBlock> block = Register.block(id,
                    () -> new PigeonholeBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BEEHIVE)
                            .noOcclusion()));
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
                        PackageBlockEntity::new, Blocks.PACKAGE.get()));

        private static PigeonholeBlock[] getPigeonholeBlocks() {
            return Blocks.PIGEONHOLES.values().stream().map(Supplier::get).toArray(PigeonholeBlock[]::new);
        }

        static void init() {
        }
    }

    public static class PoiTypes {
        public static final ResourceKey<PoiType> PIGEONHOLE =
                ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, resource("pigeonhole"));

        static void init() {
            Register.poiType(PIGEONHOLE, 0, 1, () -> getPigeonholePoiBlockStates());
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
        public static final Supplier<BlockItem> SPRUCE_PIGEONHOLE = pigeonhole("spruce", Blocks.SPRUCE_PIGEONHOLE);
        public static final Supplier<BlockItem> BIRCH_PIGEONHOLE = pigeonhole("birch", Blocks.BIRCH_PIGEONHOLE);

        public static final Supplier<LetterItem> LETTER = Register.item("letter",
                () -> new LetterItem(new Item.Properties().stacksTo(1)));

        public static final Supplier<CardboardBoxItem> CARDBOARD_BOX = Register.item("cardboard_box",
                () -> new CardboardBoxItem(new Item.Properties().stacksTo(16)));

        public static final Supplier<PackageItem> PACKAGE = Register.item("package",
                () -> new PackageItem(Blocks.PACKAGE.get(), new Item.Properties().stacksTo(1)));

        public static final Supplier<AddressTagItem> ADDRESS_TAG = Register.item("address_tag",
                () -> new AddressTagItem(new Item.Properties()));

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
        public static final DataComponentType<Address> ADDRESS = Register.dataComponentType("address",
                arg -> arg.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));

        /**
         * Used in Pigeonhole inbox to differentiate between mail items.
         */
        public static final DataComponentType<MailId> MAIL_ID = Register.dataComponentType("mail_id",
                arg -> arg.persistent(MailId.CODEC).networkSynchronized(MailId.STREAM_CODEC));

        public static final DataComponentType<Address> MAIL_SENDER = Register.dataComponentType("mail_sender",
                arg -> arg.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        public static final DataComponentType<Address> MAIL_RECIPIENT = Register.dataComponentType("mail_recipient",
                arg -> arg.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        public static final DataComponentType<Integer> MAIL_TRAVEL_DURATION = Register.dataComponentType("mail_travel_duration",
                arg -> arg.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
        public static final DataComponentType<MailDeliveryLog> MAIL_DELIVERY_LOG = Register.dataComponentType("mail_delivery_log",
                arg -> arg.persistent(MailDeliveryLog.CODEC).networkSynchronized(MailDeliveryLog.STREAM_CODEC));

        public static final DataComponentType<String> LETTER_SUBJECT = Register.dataComponentType("letter_subject",
                arg -> arg.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(512)));
        public static final DataComponentType<String> LETTER_MESSAGE = Register.dataComponentType("letter_message",
                arg -> arg.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(4096)));

        public static final DataComponentType<PackageContents> PACKAGE_CONTENTS = Register.dataComponentType("package_contents",
                arg -> arg.persistent(PackageContents.CODEC).networkSynchronized(PackageContents.STREAM_CODEC));
        public static final DataComponentType<StoredItemStack> PACKAGE_LETTER = Register.dataComponentType("package_letter",
                arg -> arg.persistent(StoredItemStack.CODEC).networkSynchronized(StoredItemStack.STREAM_CODEC));
        public static final DataComponentType<Integer> PACKAGE_TIMES_PACKED = Register.dataComponentType("package_times_packed",
                arg -> arg.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

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
        public static final Supplier<MenuType<PigeonholeAddressTagMenu>> PIGEONHOLE_ADDRESS =
                Register.menuType("pigeonhole_address", PigeonholeAddressTagMenu::fromNetwork);
        public static final Supplier<MenuType<PigeonholeMenu>> PIGEONHOLE =
                Register.menuType("pigeonhole", PigeonholeMenu::fromNetwork);

        public static final Supplier<MenuType<PackageMenu>> PACKAGE =
                Register.menuType("package", PackageMenu::fromNetwork);

        static void init() {
        }
    }

    public static class RecipeSerializers {
        static void init() {
        }
    }

    public static class SoundEvents {
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
                    TagKey.create(Registries.BLOCK, resource("pigeon_spawnable_on"));
            public static final TagKey<Block> PIGEONHOLES =
                    TagKey.create(Registries.BLOCK, resource("pigeonholes"));
        }

        public static class Items {
            public static final TagKey<Item> PIGEONHOLES =
                    TagKey.create(Registries.ITEM, resource("pigeonholes"));
            public static final TagKey<Item> PIGEON_FOOD =
                    TagKey.create(Registries.ITEM, resource("pigeon_food"));
            public static final TagKey<Item> WASTE_SCOOPABLE =
                    TagKey.create(Registries.ITEM, resource("waste_scoopable"));
            public static final TagKey<Item> MAILABLE =
                    TagKey.create(Registries.ITEM, resource("mailable"));
            public static final TagKey<Item> CANNOT_BE_PACKAGED =
                    TagKey.create(Registries.ITEM, resource("cannot_be_packaged"));
            public static final TagKey<Item> LETTERS =
                    TagKey.create(Registries.ITEM, resource("letters"));
            public static final TagKey<Item> PACKAGES =
                    TagKey.create(Registries.ITEM, resource("packages"));
        }

        public static class EntityTypes {
            public static final TagKey<EntityType<?>> PIGEONHOLE_INHABITORS =
                    TagKey.create(Registries.ENTITY_TYPE, resource("pigeonhole_inhabitors"));
        }

        public static class Biomes {
            public static final TagKey<Biome> ALLOWS_PIGEON_SPAWNS =
                    TagKey.create(Registries.BIOME, resource("allows_pigeon_spawns"));

        }
        public static class Structures {
            public static final TagKey<Structure> PIGEONS_SPAWN_IN =
                    TagKey.create(Registries.STRUCTURE, resource("pigeons_spawn_in"));
        }
    }

    public static class ArgumentTypes {
        public static void init() {
        }
    }
}
