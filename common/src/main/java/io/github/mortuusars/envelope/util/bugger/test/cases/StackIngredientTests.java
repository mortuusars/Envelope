package io.github.mortuusars.envelope.util.bugger.test.cases;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.component.Id;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StackIngredientTests extends BuggerTests {
    private final MinecraftServer server;

    public StackIngredientTests(MinecraftServer server) {
        this.server = server;
        equality();
        itemMatching();
        tagMatching();
        componentMatching();
        matchingItemFromJson();
        matchingTagFromJson();
        matchingItemWithComponentsFromJson();
        matchingItemWithComponentsStrictFromJson();
    }

    private void equality() {
        StackIngredient ingredient = new StackIngredient(Items.FEATHER, 3, DataComponentPredicate.builder()
              .expect(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE)
              .build());

        StackIngredient ingredient2 = new StackIngredient(Items.FEATHER, 3, DataComponentPredicate.builder()
              .expect(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE)
              .build());

        add("StackIngredient_equality_passesIfSame", Test.isTrue(() -> ingredient.equals(ingredient2)));
        add("StackIngredient_equality_failsIfDifferent", Test.isFalse(() -> ingredient.equals(new StackIngredient(Items.EMERALD, 23))));
    }

    private void itemMatching() {
        StackIngredient ingredient = new StackIngredient(Items.FEATHER, 3);

        add("StackIngredient_itemMatching", Test.isTrue(() -> ingredient.test(new ItemStack(Items.FEATHER, 3))));
        add("StackIngredient_itemMatching_failsWhenItemIsDifferent", Test.isFalse(() -> ingredient.test(new ItemStack(Items.WRITTEN_BOOK, 3))));
        add("StackIngredient_itemMatching_failsWhenCountIsLesser", Test.isFalse(() -> ingredient.test(new ItemStack(Items.FEATHER, 1))));
        add("StackIngredient_itemMatching_failsWhenMoreThanCount", Test.isFalse(() -> ingredient.test(new ItemStack(Items.FEATHER, 5))));

        ItemStack stack = new ItemStack(Items.FEATHER, 3);
        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        add("StackIngredient_itemMatching_withRandomComponents", Test.isTrue(() -> ingredient.test(stack)));
    }

    private void tagMatching() {
        StackIngredient ingredient = new StackIngredient(ItemTags.LOGS, 12);

        add("StackIngredient_tagMatching", Test.isTrue(() -> ingredient.test(new ItemStack(Items.OAK_LOG, 12))));
        add("StackIngredient_tagMatching_failsWhenItemIsDifferent", Test.isFalse(() -> ingredient.test(new ItemStack(Items.WRITTEN_BOOK, 20))));
        add("StackIngredient_tagMatching_failsWhenCountIsLesser", Test.isFalse(() -> ingredient.test(new ItemStack(Items.OAK_LOG, 5))));
        add("StackIngredient_tagMatching_failsWhenMoreThanCount", Test.isFalse(() -> ingredient.test(new ItemStack(Items.OAK_LOG, 30))));

        ItemStack stack = new ItemStack(Items.OAK_LOG, 12);
        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        add("StackIngredient_tagMatching_withRandomComponents", Test.isTrue(() -> ingredient.test(stack)));
    }

    private void componentMatching() {
        StackIngredient ingredient = new StackIngredient(Items.FEATHER, 3,
              DataComponentPredicate.builder()
                    .expect(Envelope.DataComponents.MAIL_SENDER, MailService.of(server.overworld()).getAddress())
                    .expect(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE)
                    .build());

        ItemStack stack = new ItemStack(Items.FEATHER, 3);
        stack.set(Envelope.DataComponents.MAIL_SENDER, MailService.of(server.overworld()).getAddress());
        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        add("StackIngredient_componentMatching", Test.isTrue(() -> ingredient.test(stack)));

        ItemStack stack2 = stack.copy();
        stack2.remove(Envelope.DataComponents.MAIL_SENDER);
        add("StackIngredient_componentMatching_failsWhenMissing", Test.isFalse(() -> ingredient.test(stack2)));
    }

    private void matchingItemFromJson() {
        add("StackIngredient_matchingItemFromJson",
              Test.isTrue(() -> decodeFromJson("{\"item\":\"minecraft:emerald\"}").test(new ItemStack(Items.EMERALD))));
        add("StackIngredient_matchingItemFromJson_failsWhenCountIsLesser",
              Test.isFalse(() -> decodeFromJson("{\"item\":\"minecraft:emerald\",\"count\":5}").test(new ItemStack(Items.EMERALD))));
    }

    private void matchingItemWithComponentsFromJson() {
        String json = """
              {
                "item": "minecraft:emerald",
                "count": 3,
                "components": {
                  "envelope:letter_tattered": {},
                  "envelope:mail_sender": {
                      "type": "entity",
                      "entity": "envelope:mail_service"
                  },
                  "envelope:mail_recipient": {
                      "type": "block",
                      "id": "Mortuusars Laboratory"
                  }
                }
              }
              """;
        add("StackIngredient_matchingItemWithComponentsFromJson",
              Test.isTrue(() -> {
                  ItemStack stack = new ItemStack(Items.EMERALD, 3);
                  stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
                  stack.set(Envelope.DataComponents.MAIL_SENDER, MailService.of(server.overworld()).getAddress());
                  stack.set(Envelope.DataComponents.MAIL_RECIPIENT, new BlockAddress("Mortuusars Laboratory"));
                  return decodeFromJson(json).test(stack);
              }));
    }

    private void matchingItemWithComponentsStrictFromJson() {
        String json = """
              {
                "item": "minecraft:emerald",
                "count": 3,
                "components": {
                  "envelope:letter_tattered": {},
                  "envelope:mail_sender": {
                      "type": "entity",
                      "entity": "envelope:mail_service"
                  },
                  "envelope:mail_recipient": {
                      "type": "block",
                      "id": "Mortuusars Laboratory"
                  }
                },
                "strict": true
              }
              """;
        add("StackIngredient_matchingItemWithComponentsStrictFromJson",
              Test.isTrue(() -> {
                  ItemStack stack = new ItemStack(Items.EMERALD, 3);
                  stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
                  stack.set(Envelope.DataComponents.MAIL_SENDER, MailService.of(server.overworld()).getAddress());
                  stack.set(Envelope.DataComponents.MAIL_RECIPIENT, new BlockAddress("Mortuusars Laboratory"));
                  return decodeFromJson(json).test(stack);
              }));

        add("StackIngredient_matchingItemWithComponentsStrictFromJson_fails_with_less",
              Test.isFalse(() -> {
                  ItemStack stack = new ItemStack(Items.EMERALD, 3);
                  stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
                  stack.set(Envelope.DataComponents.MAIL_SENDER, MailService.of(server.overworld()).getAddress());
                  return decodeFromJson(json).test(stack);
              }));

        add("StackIngredient_matchingItemWithComponentsStrictFromJson_fails_with_additional",
              Test.isFalse(() -> {
                  ItemStack stack = new ItemStack(Items.EMERALD, 3);
                  stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
                  stack.set(Envelope.DataComponents.MAIL_SENDER, MailService.of(server.overworld()).getAddress());
                  stack.set(Envelope.DataComponents.MAIL_RECIPIENT, new BlockAddress("Mortuusars Laboratory"));
                  stack.set(Envelope.DataComponents.MAIL_ID, Id.createUnsafe(123));
                  return decodeFromJson(json).test(stack);
              }));
    }

    private void matchingTagFromJson() {
        add("StackIngredient_matchingTagFromJson",
              Test.isTrue(() -> decodeFromJson("{\"item\":\"#minecraft:planks\"}").test(new ItemStack(Items.BIRCH_PLANKS))));
        add("StackIngredient_matchingTagFromJson_failsWhenCountIsLesser",
              Test.isFalse(() -> decodeFromJson("{\"item\":\"#minecraft:planks\",\"count\":5}").test(new ItemStack(Items.BIRCH_PLANKS))));
    }

    private StackIngredient decodeFromJson(String json) {
        JsonObject obj = GsonHelper.parse(json);
        return StackIngredient.CODEC.parse(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), obj)
              .getOrThrow();
    }
}
