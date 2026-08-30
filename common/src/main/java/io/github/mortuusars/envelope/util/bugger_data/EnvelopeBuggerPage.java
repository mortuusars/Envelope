package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.mortaar.bugger.screen.page.BuggerScreenPage;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnvelopeBuggerPage implements BuggerScreenPage {
    @Override
    public String getTitle() {
        return "Envelope";
    }

    @Override
    public List<String> getLeftLines() {
        return EnvelopeBuggerData.MAIL_SERVICE.get()
              .map(tag -> {

                  List<String> lines = new ArrayList<>(List.of(
                        "Mailboxes: " + tag.getInt("mailboxes"),
                        "",
                        "Mail:",
                        "  Dropped: " + tag.getInt("dropped_mail_count"),
                        "  Awaiting Payback: " + tag.getInt("payback_pending_mail_count"),
                        "",
                        "Couriers:",
                        "  Real: " + tag.getInt("delivering_pigeons"),
                        "  Background: " + tag.getInt("background_delivering_pigeons"),
                        "  Finished: " + tag.getInt("background_finished_pigeons")
                  ));

                  ListTag deliveries = tag.getList("deliveries", Tag.TAG_STRING);
                  if (!deliveries.isEmpty()) {
                      lines.add("");
                      lines.add("Deliveries:");
                      for (Tag delivery : deliveries) {
                          lines.add("  " + delivery.getAsString());
                      }
                  }

                  return lines;
              })
              .orElse(Collections.emptyList());
    }
}
