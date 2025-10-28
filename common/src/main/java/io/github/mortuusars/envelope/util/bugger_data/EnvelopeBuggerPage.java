package io.github.mortuusars.envelope.util.bugger_data;

import io.github.mortuusars.envelope.util.bugger.Bugger;
import io.github.mortuusars.envelope.util.bugger.page.BuggerPage;

import java.util.Collections;
import java.util.List;

public class EnvelopeBuggerPage implements BuggerPage {
    @Override
    public String getTitle() {
        return "Envelope";
    }

    @Override
    public List<String> getLeftLines() {
        return Bugger.ENVELOPE.get()
              .map(tag -> List.of(
                    "Pigeonholes: " + tag.getInt("pigeonholes"),
                    "",
                    "Delivering Pigeons: " + tag.getInt("delivering_pigeons"),
                    "Background Delivering Pigeons: " + tag.getInt("background_delivering_pigeons"),
                    "Background Pigeons (Waiting For Spawn): " + tag.getInt("background_finished_pigeons")
              ))
              .orElse(Collections.emptyList());
    }
}
