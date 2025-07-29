package io.github.mortuusars.envelope;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.api.mail.Address;
import io.github.mortuusars.envelope.api.mail.log.MailTravelingLog;
import io.github.mortuusars.envelope.world.block.MailboxBlock;
import io.github.mortuusars.envelope.world.block.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.item.CardboardBoxItem;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.PackageItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public class Envelope {
    public static final String ID = "envelope";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        Blocks.init();
        BlockEntityTypes.init();
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
        public static final Supplier<MailboxBlock> MAILBOX = Register.block("mailbox",
                () -> new MailboxBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.LECTERN)
                        .noOcclusion()));

        static void init() {
        }
    }

    public static class BlockEntityTypes {
        public static final Supplier<BlockEntityType<MailboxBlockEntity>> MAILBOX =
                Register.blockEntityType("mailbox", () -> Register.newBlockEntityType(MailboxBlockEntity::new, Blocks.MAILBOX.get()));

        static void init() {
        }
    }

    public static class Items {
        public static final Supplier<BlockItem> MAILBOX = Register.item("mailbox",
                () -> new BlockItem(Blocks.MAILBOX.get(), new Item.Properties()));

        public static final Supplier<LetterItem> LETTER = Register.item("letter",
                () -> new LetterItem(new Item.Properties()));
        public static final Supplier<CardboardBoxItem> CARDBOARD_BOX = Register.item("cardboard_box",
                () -> new CardboardBoxItem(new Item.Properties()));
        public static final Supplier<PackageItem> PACKAGE = Register.item("package",
                () -> new PackageItem(new Item.Properties().stacksTo(1)));

        public static final Supplier<SpawnEggItem> PIGEON_SPAWN_EGG = Register.item("pigeon_spawn_egg",
                () -> new SpawnEggItem(EntityTypes.PIGEON.get(), 0x676781, 0xB8B8CB, new Item.Properties()));

        static void init() {
        }
    }

    public static class DataComponents {
        public static final DataComponentType<UUID> MAIL_ID = Register.dataComponentType("mail_id",
                arg -> arg.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
        /**
         * 'From' address of the mail. Used to know return place if mail cannot be delivered to recipient, amongst other purposes.
         * This component is temporary and should not be depended on.
         */
        public static final DataComponentType<Address> MAIL_SENDER = Register.dataComponentType("mail_sender",
                arg -> arg.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        /**
         * 'To' address of the mail. Can change in the process of traveling (if the mail is returned or rejected, for example).
         * This component is temporary and should not be depended on.
         * If the item needs to have persistent recipient, it should use another 'recipient' component,
         * like Letters do with 'envelope:letter_recipient'.
         */
        public static final DataComponentType<Address> MAIL_RECIPIENT = Register.dataComponentType("mail_recipient",
                arg -> arg.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        /**
         * GameTime at which mail has been sent. Used to calculate mail travel, etc.
         */
        public static final DataComponentType<Long> MAIL_SENT_AT = Register.dataComponentType("mail_sent_at",
                arg -> arg.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
        /**
         * Duration of the trip from sender to recipient. Returning (or rejecting, etc.) a mail will use this for travel back duration as well.
         */
        public static final DataComponentType<Integer> MAIL_TRAVEL_DURATION = Register.dataComponentType("mail_travel_duration",
                arg -> arg.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

        /**
         * Log of all steps that mail has been through. Used to show details to the player.
         */
        public static final DataComponentType<MailTravelingLog> MAIL_TRAVELING_LOG = Register.dataComponentType("mail_traveling_log",
                arg -> arg.persistent(MailTravelingLog.CODEC).networkSynchronized(MailTravelingLog.STREAM_CODEC));

        public static final DataComponentType<Address> LETTER_RECIPIENT = Register.dataComponentType("letter_recipient",
                arg -> arg.persistent(Address.CODEC).networkSynchronized(Address.STREAM_CODEC));
        public static final DataComponentType<String> LETTER_SUBJECT = Register.dataComponentType("letter_subject",
                arg -> arg.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(512)));
        public static final DataComponentType<String> LETTER_MESSAGE = Register.dataComponentType("letter_message",
                arg -> arg.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(4096)));

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
        public static final Supplier<MenuType<MailboxMenu>> MAILBOX = Register.menuType("mailbox", MailboxMenu::fromNetwork);

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
