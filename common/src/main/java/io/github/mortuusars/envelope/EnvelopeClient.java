package io.github.mortuusars.envelope;

import io.github.mortuusars.envelope.util.bugger.BuggerGui;
import io.github.mortuusars.envelope.util.bugger.BuggerEntityOverhead;
import io.github.mortuusars.envelope.util.bugger_data.EnvelopeStatsBuggerPage;
import io.github.mortuusars.envelope.util.bugger_data.PigeonEntityOverheadData;

public class EnvelopeClient {
    public static void init() {
        BuggerGui.addPage(new EnvelopeStatsBuggerPage());
        BuggerEntityOverhead.addData(new PigeonEntityOverheadData());
    }
}
