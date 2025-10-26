package io.github.mortuusars.envelope;

import io.github.mortuusars.envelope.util.bugger.BuggerGui;
import io.github.mortuusars.envelope.util.bugger_data.EnvelopeStatsBuggerPage;

public class EnvelopeClient {
    public static void init() {
        BuggerGui.addPage(new EnvelopeStatsBuggerPage());
    }
}
