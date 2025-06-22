package io.github.mortuusars.envelope;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.mortuusars.envelope.api.mail.Recipient;
import io.github.mortuusars.envelope.world.block.MailboxBlock;
import io.github.mortuusars.envelope.world.inventory.MailboxMenu;
import io.github.mortuusars.envelope.world.item.CardboardBoxItem;
import io.github.mortuusars.envelope.world.item.LetterItem;
import io.github.mortuusars.envelope.world.item.PackageItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;

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

        static void init() {
        }
    }

    public static class DataComponents {
        public static final DataComponentType<Recipient> RECIPIENT = Register.dataComponentType("recipient",
                arg -> arg.persistent(Recipient.CODEC).networkSynchronized(Recipient.STREAM_CODEC));

        public static final DataComponentType<String> LETTER_SUBJECT = Register.dataComponentType("letter_subject",
                arg -> arg.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(512)));
        public static final DataComponentType<String> LETTER_MESSAGE = Register.dataComponentType("letter_message",
                arg -> arg.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(4096)));

        static void init() {
        }
    }

    public static class EntityTypes {
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
    }

    public static class ArgumentTypes {
        public static void init() {
        }
    }

    public static class Registries {
    }
}
