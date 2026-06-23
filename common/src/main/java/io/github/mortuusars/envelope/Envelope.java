package io.github.mortuusars.envelope;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.envelope.command.argument.AddressArgument;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlock;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.CharredPigeon;
import io.github.mortuusars.envelope.world.entity.PigeonVariant;
import io.github.mortuusars.envelope.world.inventory.*;
import io.github.mortuusars.envelope.world.item.*;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryLog;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealImpression;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.item.crafting.*;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailCraftingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailLetterBroadcastingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailPaybackRequestCancelingRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.MailRecipe;
import io.github.mortuusars.envelope.world.item.crafting.mail.serializer.MailRecipeSerializer;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.util.DeferredSoundType;
import io.github.mortuusars.envelope.world.block.*;
import io.github.mortuusars.envelope.world.block.occupiable.Occupant;
import io.github.mortuusars.envelope.world.item.component.*;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.item.component.PaybackSubject;
import io.github.mortuusars.envelope.world.mail.service.ServiceAddressDefinition;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
import net.minecraft.world.level.storage.loot.LootTable;
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
        EntityDataSerializers.init();
        Items.init();
        DataComponents.init();
        Stats.init();
        CriteriaTriggers.init();
        ItemSubPredicates.init();
        MenuTypes.init();
        RecipeTypes.init();
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

        public static final Supplier<PigeonholeBlock> OAK_PIGEONHOLE = pigeonhole("oak", net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> SPRUCE_PIGEONHOLE = pigeonhole("spruce", net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> BIRCH_PIGEONHOLE = pigeonhole("birch", net.minecraft.world.level.block.Blocks.BIRCH_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> JUNGLE_PIGEONHOLE = pigeonhole("jungle", net.minecraft.world.level.block.Blocks.JUNGLE_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> ACACIA_PIGEONHOLE = pigeonhole("acacia", net.minecraft.world.level.block.Blocks.ACACIA_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> DARK_OAK_PIGEONHOLE = pigeonhole("dark_oak", net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> MANGROVE_PIGEONHOLE = pigeonhole("mangrove", net.minecraft.world.level.block.Blocks.MANGROVE_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> CHERRY_PIGEONHOLE = pigeonhole("cherry", net.minecraft.world.level.block.Blocks.CHERRY_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> BAMBOO_PIGEONHOLE = pigeonhole("bamboo", net.minecraft.world.level.block.Blocks.BAMBOO_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> CRIMSON_PIGEONHOLE = pigeonhole("crimson", net.minecraft.world.level.block.Blocks.CRIMSON_PLANKS.defaultMapColor());
        public static final Supplier<PigeonholeBlock> WARPED_PIGEONHOLE = pigeonhole("warped", net.minecraft.world.level.block.Blocks.WARPED_PLANKS.defaultMapColor());

        public static final Supplier<MailboxBlock> MAILBOX = Register.block("mailbox",
              () -> new MailboxBlock(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .sound(SoundType.WOOD)
                    .mapColor(MapColor.WOOD)));

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

        public static final Supplier<LetterBlock> LETTER = Register.block("letter",
              () -> new LetterBlock(BlockBehaviour.Properties.of()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundTypes.PAPER)
                    .ignitedByLava()
                    .instabreak()
                    .noCollission()
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
        public static final Supplier<BlockEntityType<MailboxBlockEntity>> MAILBOX =
              Register.blockEntityType("mailbox", () -> Register.newBlockEntityType(
                    MailboxBlockEntity::new, Blocks.MAILBOX.get()));

        public static final Supplier<BlockEntityType<PigeonholeBlockEntity>> PIGEONHOLE =
              Register.blockEntityType("pigeonhole", () -> Register.newBlockEntityType(
                    PigeonholeBlockEntity::new, getPigeonholeBlocks()));

        public static final Supplier<BlockEntityType<PackageBlockEntity>> PACKAGE =
              Register.blockEntityType("package", () -> Register.newBlockEntityType(
                    PackageBlockEntity::new, Blocks.PACKAGE.get(), Blocks.SEALED_PACKAGE.get()));

        public static final Supplier<BlockEntityType<LetterBlockEntity>> LETTER =
              Register.blockEntityType("letter", () -> Register.newBlockEntityType(
                    LetterBlockEntity::new, Blocks.LETTER.get()));

        private static PigeonholeBlock[] getPigeonholeBlocks() {
            return Blocks.PIGEONHOLES.values().stream().map(Supplier::get).toArray(PigeonholeBlock[]::new);
        }

        static void init() {
        }
    }

    public static class PoiTypes {
        public static final ResourceKey<PoiType> PIGEONHOLE =
              ResourceKey.create(net.minecraft.core.registries.Registries.POINT_OF_INTEREST_TYPE, resource("pigeonhole"));

        public static final ResourceKey<PoiType> MAILBOX =
              ResourceKey.create(net.minecraft.core.registries.Registries.POINT_OF_INTEREST_TYPE, resource("mailbox"));

        static void init() {
            Register.poiType(PIGEONHOLE, 0, 1, PoiTypes::getPigeonholePoiBlockStates);
            Register.poiType(MAILBOX, 0, 1, PoiTypes::getMailboxPoiBlockStates);
        }

        private static Set<BlockState> getPigeonholePoiBlockStates() {
            return Blocks.PIGEONHOLES.values().stream()
                  .map(Supplier::get)
                  .map(b -> b.getStateDefinition().getPossibleStates())
                  .flatMap(Collection::stream)
                  .collect(Collectors.toSet());
        }

        private static Set<BlockState> getMailboxPoiBlockStates() {
            return Set.copyOf(Blocks.MAILBOX.get().getStateDefinition().getPossibleStates());
        }
    }

    public static class Items {
        public static final List<Supplier<BlockItem>> PIGEONHOLES = new ArrayList<>();

        public static final Supplier<BlockItem> OAK_PIGEONHOLE = pigeonhole("oak", Blocks.OAK_PIGEONHOLE);
        public static final Supplier<BlockItem> SPRUCE_PIGEONHOLE = pigeonhole("spruce", Blocks.SPRUCE_PIGEONHOLE);
        public static final Supplier<BlockItem> BIRCH_PIGEONHOLE = pigeonhole("birch", Blocks.BIRCH_PIGEONHOLE);
        public static final Supplier<BlockItem> JUNGLE_PIGEONHOLE = pigeonhole("jungle", Blocks.JUNGLE_PIGEONHOLE);
        public static final Supplier<BlockItem> ACACIA_PIGEONHOLE = pigeonhole("acacia", Blocks.ACACIA_PIGEONHOLE);
        public static final Supplier<BlockItem> DARK_OAK_PIGEONHOLE = pigeonhole("dark_oak", Blocks.DARK_OAK_PIGEONHOLE);
        public static final Supplier<BlockItem> MANGROVE_PIGEONHOLE = pigeonhole("mangrove", Blocks.MANGROVE_PIGEONHOLE);
        public static final Supplier<BlockItem> CHERRY_PIGEONHOLE = pigeonhole("cherry", Blocks.CHERRY_PIGEONHOLE);
        public static final Supplier<BlockItem> BAMBOO_PIGEONHOLE = pigeonhole("bamboo", Blocks.BAMBOO_PIGEONHOLE);
        public static final Supplier<BlockItem> CRIMSON_PIGEONHOLE = pigeonhole("crimson", Blocks.CRIMSON_PIGEONHOLE);
        public static final Supplier<BlockItem> WARPED_PIGEONHOLE = pigeonhole("warped", Blocks.WARPED_PIGEONHOLE);

        public static final Supplier<MailboxBlockItem> MAILBOX = Register.item("mailbox",
              () -> new MailboxBlockItem(Blocks.MAILBOX.get(), new Item.Properties()));

        public static final Supplier<LetterAndQuillItem> LETTER_AND_QUILL = Register.item("letter_and_quill",
              () -> new LetterAndQuillItem(new Item.Properties().stacksTo(1)));
        public static final Supplier<LetterItem> LETTER = Register.item("letter",
              () -> new LetterItem(Blocks.LETTER.get(), new Item.Properties()));
        public static final Supplier<SealedLetterItem> SEALED_LETTER = Register.item("sealed_letter",
              () -> new SealedLetterItem(new Item.Properties()));

        public static final Supplier<PaperBoxItem> PAPER_BOX = Register.item("paper_box",
              () -> new PaperBoxItem(Blocks.PAPER_BOX.get(), new Item.Properties().stacksTo(16)));
        public static final Supplier<PackageItem> PACKAGE = Register.item("package",
              () -> new PackageItem(Blocks.PACKAGE.get(), new Item.Properties().stacksTo(1)));
        public static final Supplier<SealedPackageItem> SEALED_PACKAGE = Register.item("sealed_package",
              () -> new SealedPackageItem(Blocks.SEALED_PACKAGE.get(), new Item.Properties().stacksTo(1)));

        public static final Supplier<PaybackTagItem> PAYBACK_TAG = Register.item("payback_tag",
              () -> new PaybackTagItem(new Item.Properties()));
        public static final Supplier<PaybackBoxItem> PAYBACK_BOX = Register.item("payback_box",
              () -> new PaybackBoxItem(new Item.Properties().stacksTo(1)));
        public static final Supplier<PaybackPackageItem> PAYBACK_PACKAGE = Register.item("payback_package",
              () -> new PaybackPackageItem(new Item.Properties().stacksTo(1)));

        public static final Supplier<AddressTagItem> ADDRESS_TAG = Register.item("address_tag",
              () -> new AddressTagItem(new Item.Properties()));
        public static final Supplier<SealStampItem> SEAL_STAMP = Register.item("seal_stamp",
              () -> new SealStampItem(new Item.Properties().stacksTo(1)));

        public static final Supplier<SpawnEggItem> PIGEON_SPAWN_EGG = Register.item("pigeon_spawn_egg",
              () -> new SpawnEggItem(EntityTypes.PIGEON.get(), 0x676781, 0xB8B8CB, new Item.Properties()));
        public static final Supplier<SpawnEggItem> CHARRED_PIGEON_SPAWN_EGG = Register.item("charred_pigeon_spawn_egg",
              () -> new SpawnEggItem(EntityTypes.CHARRED_PIGEON.get(), 0x2B2223, 0xE85F00, new Item.Properties()));

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

        // -- Mail

        public static final DataComponentType<Id> MAIL_ID = Register.dataComponentType("mail_id", b ->
              b.persistent(Id.CODEC).networkSynchronized(Id.STREAM_CODEC));
        /**
         * Only for display purposes. Shouldn't be used delivery in logic.
         */
        public static final DataComponentType<Address> MAIL_SENDER = Register.dataComponentType("mail_sender", b ->
              b.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        public static final DataComponentType<Address> MAIL_RECIPIENT = Register.dataComponentType("mail_recipient", b ->
              b.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        public static final DataComponentType<PaybackRequest> MAIL_PAYBACK_REQUEST = Register.dataComponentType("mail_payback_request",
              b -> b.persistent(PaybackRequest.CODEC).networkSynchronized(PaybackRequest.STREAM_CODEC).cacheEncoding());
        public static final DataComponentType<DeliveryLog> MAIL_DELIVERY_LOG = Register.dataComponentType("mail_delivery_log", b ->
              b.persistent(DeliveryLog.CODEC).networkSynchronized(DeliveryLog.STREAM_CODEC));
        public static final DataComponentType<Unit> MAIL_RETURNED = Register.dataComponentType("mail_returned", b ->
              b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

        // -- Letter

        public static final DataComponentType<LetterAndQuillContent> LETTER_AND_QUILL_CONTENT =
              Register.dataComponentType("letter_and_quill_content", b ->
                    b.persistent(LetterAndQuillContent.CODEC).networkSynchronized(LetterAndQuillContent.STREAM_CODEC));
        public static final DataComponentType<LetterContent> LETTER_CONTENT =
              Register.dataComponentType("letter_content", b ->
                    b.persistent(LetterContent.CODEC).networkSynchronized(LetterContent.STREAM_CODEC).cacheEncoding());
        public static final DataComponentType<Unit> LETTER_TATTERED =
              Register.dataComponentType("letter_tattered", b ->
                    b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

        // -- Package

        public static final DataComponentType<PackageContents> PACKAGE_CONTENTS = Register.dataComponentType("package_contents",
              b -> b.persistent(PackageContents.CODEC).networkSynchronized(PackageContents.STREAM_CODEC));
        public static final DataComponentType<Integer> PACKAGE_EXPERIENCE = Register.dataComponentType("package_experience",
              b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.INT));

        // -- Seal

        public static final DataComponentType<Seal> SEAL = Register.dataComponentType("seal",
              b -> b.persistent(Seal.CODEC).networkSynchronized(Seal.STREAM_CODEC).cacheEncoding());
        public static final DataComponentType<Holder<SealImpression>> SEAL_STAMP_IMPRESSION = Register.dataComponentType("seal_stamp_impression",
              b -> b.persistent(SealImpression.CODEC).networkSynchronized(SealImpression.STREAM_CODEC).cacheEncoding());

        // -- Payback

        public static final DataComponentType<PaybackRequest> PAYBACK_TAG_CONTENTS = Register.dataComponentType("payback_tag_contents",
              b -> b.persistent(PaybackRequest.CODEC).networkSynchronized(PaybackRequest.STREAM_CODEC).cacheEncoding());
        public static final DataComponentType<PaybackSubject> PAYBACK_SUBJECT = Register.dataComponentType("payback_subject",
              b -> b.persistent(PaybackSubject.CODEC).networkSynchronized(PaybackSubject.STREAM_CODEC).cacheEncoding());

        // -- Misc

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

        public static final Supplier<EntityType<CharredPigeon>> CHARRED_PIGEON = Register.entityType("charred_pigeon",
              CharredPigeon::new, MobCategory.MONSTER, true, builder -> builder
                    .sized(0.65F, 0.85F)
                    .eyeHeight(0.59375F)
                    .passengerAttachments(0.4625F)
                    .clientTrackingRange(8));


        static void init() {
        }
    }

    public static class EntityDataSerializers {
        public static final Supplier<EntityDataSerializer<Holder<PigeonVariant>>> PIGEON_VARIANT =
              Register.entityDataSerializer("pigeon_variant", EntityDataSerializer.forValueType(PigeonVariant.STREAM_CODEC));

        static void init() {
        }
    }

    public static class MenuTypes {
        public static final Supplier<MenuType<MailboxMenu>> MAILBOX =
              Register.menuType("mailbox", MailboxMenu::fromNetwork);

        public static final Supplier<MenuType<PackingMenu>> PACKING =
              Register.menuType("packing", PackingMenu::fromNetwork);
        public static final Supplier<MenuType<PackageMenu>> PACKAGE =
              Register.menuType("package", PackageMenu::fromNetwork);

        public static final Supplier<MenuType<PaybackPackingMenu>> PAYBACK_PACKING =
              Register.menuType("payback_packing", PaybackPackingMenu::fromNetwork);
        public static final Supplier<MenuType<PaybackPackageMenu>> PAYBACK_PACKAGE =
              Register.menuType("payback_package", PaybackPackageMenu::fromNetwork);

        public static final Supplier<MenuType<PaybackTagMenu>> PAYBACK_TAG =
              Register.menuType("payback_tag", PaybackTagMenu::fromNetwork);

        static void init() {
        }
    }

    public static class RecipeTypes {
        public static final Supplier<RecipeType<MailRecipe>> MAILING = Register.recipeType("mailing",
              () -> new RecipeType<>() {
                  @Override
                  public String toString() {
                      return ID + ":mailing";
                  }
              });

        static void init() {
        }
    }

    public static class RecipeSerializers {
        public static final Supplier<RecipeSerializer<LetterCloningRecipe>> LETTER_CLONING = Register.recipeSerializer(
              "crafting_special_letter_cloning", () -> new SimpleCraftingRecipeSerializer<>(LetterCloningRecipe::new));
        public static final Supplier<RecipeSerializer<AddressTagApplicationRecipe>> ADDRESS_TAG_APPLICATION = Register.recipeSerializer(
              "crafting_special_address_tag_application", () -> new SimpleCraftingRecipeSerializer<>(AddressTagApplicationRecipe::new));
        public static final Supplier<RecipeSerializer<PaybackTagApplicationRecipe>> PAYBACK_TAG_APPLICATION = Register.recipeSerializer(
              "crafting_special_payback_tag_application", () -> new SimpleCraftingRecipeSerializer<>(PaybackTagApplicationRecipe::new));

        public static final Supplier<RecipeSerializer<MailCraftingRecipe>> MAIL_CRAFTING = Register.recipeSerializer(
              "mail_crafting", () -> new MailRecipeSerializer<>(MailCraftingRecipe::new));
        public static final Supplier<RecipeSerializer<MailLetterBroadcastingRecipe>> MAIL_LETTER_BROADCASTING = Register.recipeSerializer(
              "mail_letter_broadcasting", MailLetterBroadcastingRecipe.Serializer::new);
        public static final Supplier<RecipeSerializer<MailPaybackRequestCancelingRecipe>> MAIL_PAYBACK_REQUEST_CANCELING = Register.recipeSerializer(
              "mail_payback_request_canceling", MailPaybackRequestCancelingRecipe.Serializer::new);

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

        public static final Supplier<SoundEvent> PIGEONHOLE_ENTER = register("block", "pigeonhole.enter");
        public static final Supplier<SoundEvent> PIGEONHOLE_EXIT = register("block", "pigeonhole.exit");
        public static final Supplier<SoundEvent> PIGEONHOLE_WORK = register("block", "pigeonhole.work");
        public static final Supplier<SoundEvent> PIGEONHOLE_WORK_MULTIPLE = register("block", "pigeonhole.work_multiple");
        public static final Supplier<SoundEvent> PIGEONHOLE_SCOOP = register("block", "pigeonhole.scoop");

        public static final Supplier<SoundEvent> VILLAGER_THROW_PIGEON_FOOD = register("entity", "villager.throw_pigeon_food");

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
        public static Supplier<PlayerTrigger> SCOOP_DIAMOND = Register.criterionTrigger("scoop_diamond", PlayerTrigger::new);

        public static void init() {
        }
    }

    public static class ItemSubPredicates {
        public static void init() {
        }
    }

    public static class LootTables {
        public static final ResourceKey<LootTable> PIGEONHOLE_WASTE =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("gameplay/pigeonhole_waste"));
        public static final ResourceKey<LootTable> CHARRED_PIGEON_MAIL =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("gameplay/charred_pigeon_mail"));

        public static final ResourceKey<LootTable> LOST_MAIL =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail"));
        public static final ResourceKey<LootTable> LOST_MAIL_JUNK =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/junk"));
        public static final ResourceKey<LootTable> LOST_MAIL_PLANTS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/plants"));
        public static final ResourceKey<LootTable> LOST_MAIL_BLOCKS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/blocks"));
        public static final ResourceKey<LootTable> LOST_MAIL_METALS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/metals"));
        public static final ResourceKey<LootTable> LOST_MAIL_TOOLS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/tools"));
        public static final ResourceKey<LootTable> LOST_MAIL_WEAPONS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/weapons"));
        public static final ResourceKey<LootTable> LOST_MAIL_VALUABLES =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/lost_mail/valuables"));

        public static final ResourceKey<LootTable> NETHER_LOST_MAIL =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("packages/nether_lost_mail"));

        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_STORAGE =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/storage"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_STORAGE_MAIL =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/storage_mail"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_STORAGE_MATERIALS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/storage_materials"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_STORAGE_STAMPS =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/storage_stamps"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_MANUFACTORY =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/manufactory"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_DISPATCHERY =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/dispatchery"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_CONSTRUCTION =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("chests/collapsed_mail_hub/construction"));
        public static final ResourceKey<LootTable> COLLAPSED_MAIL_HUB_DOCK_NOTE =
              ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Envelope.resource("letters/collapsed_mail_hub/dock_note"));
    }

    public static class Tags {
        public static class Blocks {
            public static final TagKey<Block> PIGEONS_SPAWNABLE_ON =
                  TagKey.create(net.minecraft.core.registries.Registries.BLOCK, resource("pigeons_spawnable_on"));
            public static final TagKey<Block> PIGEONS_PERCHABLE_ON =
                  TagKey.create(net.minecraft.core.registries.Registries.BLOCK, resource("pigeons_perchable_on"));
            public static final TagKey<Block> PIGEONHOLES =
                  TagKey.create(net.minecraft.core.registries.Registries.BLOCK, resource("pigeonholes"));
            public static final TagKey<Block> PIGEONHOLES_THAT_BURN =
                  TagKey.create(net.minecraft.core.registries.Registries.BLOCK, resource("pigeonholes_that_burn"));
        }

        public static class Items {
            public static final TagKey<Item> PIGEON_FOOD =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("pigeon_food"));

            public static final TagKey<Item> VILLAGER_FEEDING_PIGEON_FOOD_COMMON =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("villager_feeding_pigeon_food_common"));
            public static final TagKey<Item> VILLAGER_FEEDING_PIGEON_FOOD_UNCOMMON =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("villager_feeding_pigeon_food_uncommon"));
            public static final TagKey<Item> VILLAGER_FEEDING_PIGEON_FOOD_RARE =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("villager_feeding_pigeon_food_rare"));

            public static final TagKey<Item> PIGEONHOLES =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("pigeonholes"));
            public static final TagKey<Item> WASTE_SCOOPABLE =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("waste_scoopable"));

            public static final TagKey<Item> LETTERS =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("letters"));
            public static final TagKey<Item> PACKAGES =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("packages"));
            public static final TagKey<Item> MAILABLE =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("mailable"));
            public static final TagKey<Item> CANNOT_BE_PACKAGED =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("cannot_be_packaged"));
            public static final TagKey<Item> CANNOT_BE_USED_AS_PAYBACK =
                  TagKey.create(net.minecraft.core.registries.Registries.ITEM, resource("cannot_be_used_as_payback"));
        }

        public static class EntityTypes {
            public static final TagKey<EntityType<?>> PIGEONHOLE_INHABITORS =
                  TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, resource("pigeonhole_inhabitors"));
        }

        public static class DamageTypes {
            public static final TagKey<DamageType> BYPASSES_PIGEON_DELIVERY_EVASION =
                  TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, resource("bypasses_pigeon_delivery_evasion"));
        }

        public static class Biomes {
            public static final TagKey<Biome> ALLOWS_PIGEON_SPAWNS =
                  TagKey.create(net.minecraft.core.registries.Registries.BIOME, resource("allows_pigeon_spawns"));
            public static final TagKey<Biome> ALLOWS_CHARRED_PIGEON_SPAWNS =
                  TagKey.create(net.minecraft.core.registries.Registries.BIOME, resource("allows_charred_pigeon_spawns"));

            public static final TagKey<Biome> SPAWNS_PASSENGER_PIGEONS =
                  TagKey.create(net.minecraft.core.registries.Registries.BIOME, resource("spawns_passenger_pigeons"));
        }

        public static class Structures {
            public static final TagKey<Structure> PIGEONS_SPAWN_IN =
                  TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE, resource("pigeons_spawn_in"));
        }

        public static class SealImpressions {
            public static final TagKey<SealImpression> SPECIAL =
                  TagKey.create(Registries.SEAL_IMPRESSION, resource("special"));
            public static final TagKey<SealImpression> TOOLS =
                  TagKey.create(Registries.SEAL_IMPRESSION, resource("tools"));
        }

        public static class ServiceAddresses {
            public static final TagKey<ServiceAddressDefinition> HIDDEN =
                  TagKey.create(Registries.SERVICE_ADDRESS_DEFINITION, resource("hidden"));
        }
    }

    public static class ArgumentTypes {
        public static final Supplier<ArgumentTypeInfo<AddressArgument, SingletonArgumentInfo<AddressArgument>.Template>> ADDRESS =
              Register.commandArgumentType("address", AddressArgument.class, SingletonArgumentInfo.contextFree(AddressArgument::all));

        public static void init() {
        }
    }

    public static class Registries {
        public static final ResourceKey<Registry<PigeonVariant>> PIGEON_VARIANT =
              ResourceKey.createRegistryKey(resource("pigeon_variant"));

        public static final ResourceKey<Registry<ServiceAddressDefinition>> SERVICE_ADDRESS_DEFINITION =
              ResourceKey.createRegistryKey(resource("service_address"));

        public static final ResourceKey<Registry<SealMaterial>> SEAL_MATERIAL =
              ResourceKey.createRegistryKey(resource("seal_material"));
        public static final ResourceKey<Registry<SealImpression>> SEAL_IMPRESSION =
              ResourceKey.createRegistryKey(resource("seal_impression"));
    }
}
