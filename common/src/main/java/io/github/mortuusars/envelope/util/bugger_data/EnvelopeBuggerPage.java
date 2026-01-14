package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.util.bugger.page.BuggerPage;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnvelopeBuggerPage implements BuggerPage {
    @Override
    public String getTitle() {
        return "Envelope";
    }

    @Override
    public List<String> getLeftLines() {
        return Bugger.MAIL_SERVICE.get()
              .map(tag -> {
                  int realPigeons = tag.getInt("delivering_pigeons");
                  int backgroundPigeons = tag.getInt("background_delivering_pigeons");
                  int backgroundFinishedPigeons = tag.getInt("background_finished_pigeons");

                  List<String> lines = new ArrayList<>(List.of(
                        "Mailboxes: " + tag.getInt("mailboxes"),
                        "",
                        "Couriers:",
                        "  Real: " + realPigeons,
                        "  Background: " + backgroundPigeons,
                        "  Finished: " + backgroundFinishedPigeons,
                        ""
                  ));

                  lines.add("Mail Awaiting Payback: " + tag.getInt("mail_awaiting_payback"));
                  lines.add("");

                  ListTag deliveries = tag.getList("deliveries", Tag.TAG_STRING);
                  if (!deliveries.isEmpty()) {
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
