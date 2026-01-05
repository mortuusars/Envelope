package io.github.mortuusars.envelope.util.bugger.test.cases;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.bugger.test.BuggerTests;
import io.github.mortuusars.envelope.util.bugger.test.Test;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RequestedItemTests extends BuggerTests {
    private final MinecraftServer server;

    public RequestedItemTests(MinecraftServer server) {
        this.server = server;
        itemMatching();
        tagMatching();
        componentMatching();
        matchingItemFromJson();
        matchingTagFromJson();
        matchingItemWithComponentsFromJson();
    }

    private void itemMatching() {
        RequestedItem requestedItem = new RequestedItem(Items.FEATHER, 3);

        add("RequestedItem_simpleMatching", Test.isTrue(() -> requestedItem.matches(new ItemStack(Items.FEATHER, 3))));
        add("RequestedItem_simpleMatching_failsWhenItemIsDifferent", Test.isFalse(() -> requestedItem.matches(new ItemStack(Items.WRITTEN_BOOK, 3))));
        add("RequestedItem_simpleMatching_failsWhenCountIsLesser", Test.isFalse(() -> requestedItem.matches(new ItemStack(Items.FEATHER, 1))));
        add("RequestedItem_simpleMatching_failsWhenMoreThanCount", Test.isFalse(() -> requestedItem.matches(new ItemStack(Items.FEATHER, 5))));

        ItemStack stack = new ItemStack(Items.FEATHER, 3);
        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        stack.set(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 5);
        add("RequestedItem_simpleMatching_withRandomComponents", Test.isTrue(() -> requestedItem.matches(stack)));
    }

    private void tagMatching() {
        RequestedItem requestedItem = new RequestedItem(ItemTags.LOGS, 12);

        add("RequestedItem_simpleMatching", Test.isTrue(() -> requestedItem.matches(new ItemStack(Items.OAK_LOG, 12))));
        add("RequestedItem_simpleMatching_failsWhenItemIsDifferent", Test.isFalse(() -> requestedItem.matches(new ItemStack(Items.WRITTEN_BOOK, 20))));
        add("RequestedItem_simpleMatching_failsWhenCountIsLesser", Test.isFalse(() -> requestedItem.matches(new ItemStack(Items.OAK_LOG, 5))));
        add("RequestedItem_simpleMatching_failsWhenMoreThanCount", Test.isFalse(() -> requestedItem.matches(new ItemStack(Items.OAK_LOG, 30))));

        ItemStack stack = new ItemStack(Items.OAK_LOG, 12);
        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        stack.set(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 5);
        add("RequestedItem_simpleMatching_withRandomComponents", Test.isTrue(() -> requestedItem.matches(stack)));
    }

    private void componentMatching() {
        RequestedItem requestedItem = new RequestedItem(Items.FEATHER, 3, DataComponentPredicate.builder()
              .expect(Envelope.DataComponents.SENDER_ADDRESS, Address.MAIL_SERVICE)
              .expect(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE)
              .expect(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 5)
              .build());

        ItemStack stack = new ItemStack(Items.FEATHER, 3);
        stack.set(Envelope.DataComponents.SENDER_ADDRESS, Address.MAIL_SERVICE);
        stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
        stack.set(Envelope.DataComponents.PACKAGE_TIMES_PACKED, 5);
        add("RequestedItem_componentMatching", Test.isTrue(() -> requestedItem.matches(stack)));

        ItemStack stack2 = stack.copy();
        stack2.remove(Envelope.DataComponents.SENDER_ADDRESS);
        add("RequestedItem_componentMatching_failsWhenMissing", Test.isFalse(() -> requestedItem.matches(stack2)));
    }

    private void matchingItemFromJson() {
        add("RequestedItem_matchingItemFromJson_simple",
              Test.isTrue(() -> decodeFromJson("{\"item\":\"minecraft:emerald\"}").matches(new ItemStack(Items.EMERALD))));
        add("RequestedItem_matchingItemFromJson_failsWhenCountIsLesser",
              Test.isFalse(() -> decodeFromJson("{\"item\":\"minecraft:emerald\",\"count\":5}").matches(new ItemStack(Items.EMERALD))));
    }

    private void matchingItemWithComponentsFromJson() {
        String json = """
              {
                "item": "minecraft:emerald",
                "count": 3,
                "components": {
                  "envelope:letter_tattered": {},
                  "envelope:sender_address": {
                      "type": "entity",
                      "id": "Mail Service"
                  },
                  "envelope:address_tag": {
                      "type": "block",
                      "id": "Mortuusars Laboratory"
                  }
                }
              }
              """;
        add("RequestedItem_matchingItemWithComponentsFromJson_simple",
              Test.isTrue(() -> {
                  ItemStack stack = new ItemStack(Items.EMERALD, 3);
                  stack.set(Envelope.DataComponents.LETTER_TATTERED, Unit.INSTANCE);
                  stack.set(Envelope.DataComponents.SENDER_ADDRESS, Address.MAIL_SERVICE);
                  stack.set(Envelope.DataComponents.RECIPIENT_ADDRESS, new Address.Block("Mortuusars Laboratory"));
                  return decodeFromJson(json).matches(stack);
              }));
    }

    private void matchingTagFromJson() {
        add("RequestedItem_matchingTagFromJson_simple",
              Test.isTrue(() -> decodeFromJson("{\"item\":\"#minecraft:planks\"}").matches(new ItemStack(Items.BIRCH_PLANKS))));
        add("RequestedItem_matchingTagFromJson_failsWhenCountIsLesser",
              Test.isFalse(() -> decodeFromJson("{\"item\":\"#minecraft:planks\",\"count\":5}").matches(new ItemStack(Items.BIRCH_PLANKS))));
    }

    private static RequestedItem decodeFromJson(String json) {
        JsonObject obj = GsonHelper.parse(json);
        return RequestedItem.CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow();
    }
}
