package io.github.mortuusars.envelope.util.bugger.page;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogPage implements BuggerPage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ArrayList<String> log = new ArrayList<>();

    @Nullable
    private LogFileTailer tailer;
    private final Path logFile = Paths.get(".").resolve("logs").resolve("latest.log");

    @Override
    public String getTitle() {
        return "Log";
    }

    @Override
    public void activated() {
        if (tailer == null) {
            tailer = new LogFileTailer(logFile, line -> {
                line = process(line);
                if (!line.isEmpty()) {
                    log.add(line);
                }
            });
            tailer.start();
        }
    }

    @Override
    public void deactivated() {
        if (tailer != null) {
            tailer.stop();
            tailer = null;
        }
    }

    private String process(String line) {
        while (log.size() > 1000) {
            log.removeFirst();
        }

        if (line.contains("Saving chunks for level")) {
            return "";
        }

        try {
            List<String> parts = extractParts(line);

            String date = parts.getFirst();
            date = date.substring(date.indexOf(' ') + 1, date.length() - 4);
            String threadAndLevel = parts.get(1);
            threadAndLevel = threadAndLevel.replace(" thread", "");
            String thread = threadAndLevel.substring(0, threadAndLevel.indexOf("/"));
            String level = threadAndLevel.substring(threadAndLevel.indexOf("/") + 1);
            String callingClass = parts.get(2);
            callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1, callingClass.length() - 1);
            String message = line.substring(line.indexOf("]: ") + 3);

            String color = switch (level) {
                case "WARN" -> ChatFormatting.YELLOW.toString();
                case "ERROR" -> ChatFormatting.RED.toString();
                default -> "";
            };

            return String.format("[%s] [%s/%s%s§r] [%s]: %s", date, thread, color, level, callingClass, message);
        } catch (Exception e) {
            return line;
        }
    }

    List<String> extractParts(String input) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\[(.*?)]").matcher(input);
        while (matcher.find()) {
            parts.add(matcher.group(1)); // content inside []
        }
        return parts;
    }

    @Override
    public List<String> getLeftLines() {
        if (log.isEmpty()) {
            return List.of("[Ctrl+O]: Open latest.log");
        }
        return log;
    }

    @Override
    public boolean onKeyPress(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_O && Screen.hasControlDown()) {
            Util.getPlatform().openFile(logFile.toFile());
            return true;
        }
        return false;
    }

    // Written by ChatGPT. Not that important to dig deep into it.
    public static class LogFileTailer {
        private final Path logPath;
        private final Consumer<String> lineConsumer;

        // make these non-final so we can recreate
        private ScheduledExecutorService scheduledExecutor;
        private Thread watchThread;

        private volatile boolean running = false;

        // tail state
        private RandomAccessFile raf = null;
        private long lastKnownLength = 0L;

        public LogFileTailer(Path logPath, Consumer<String> lineConsumer) {
            this.logPath = logPath;
            this.lineConsumer = lineConsumer;
        }

        public synchronized void start() {
            if (running) return;
            running = true;

            // scheduled executor used ONLY for poll()
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "log-tailer-poller");
                t.setDaemon(true);
                return t;
            });

            // schedule poll repeatedly
            scheduledExecutor.scheduleWithFixedDelay(() -> {
                try {
                    poll();
                } catch (Throwable t) {
                    // NEVER let exceptions escape — they will cancel the scheduled task
                }
            }, 0, 300, TimeUnit.MILLISECONDS);

            // watchLoop runs on its own thread so it can't block the scheduler
            watchThread = new Thread(this::watchLoop, "log-tailer-watcher");
            watchThread.setDaemon(true);
            watchThread.start();
        }

        public synchronized void stop() {
            running = false;

            if (scheduledExecutor != null) {
                scheduledExecutor.shutdownNow();
                scheduledExecutor = null;
            }

            if (watchThread != null) {
                watchThread.interrupt();
                watchThread = null;
            }

            if (raf != null) {
                try { raf.close(); } catch (IOException ignored) {}
                raf = null;
            }
        }

        private void poll() {
            // protect whole method — an exception would stop future executions
            try {
                tailFile();
            } catch (Throwable t) {
                LOGGER.error("poll error: ", t);
            }
        }

        private synchronized void tailFile() throws IOException {
            if (raf == null) openForTail();

            if (raf == null) return; // file didn't exist yet

            long fileLength = raf.length();
            if (fileLength < lastKnownLength) {
                // truncated / rotated
                reopenForTail();
                fileLength = raf.length();
            }

            if (fileLength > lastKnownLength) {
                raf.seek(lastKnownLength);
                String line;
                while ((line = raf.readLine()) != null) {
                    // readLine() uses ISO-8859-1; convert if needed
                    try {
                        lineConsumer.accept(new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        lineConsumer.accept(line);
                    }
                }
                lastKnownLength = raf.getFilePointer();
            }
        }

        private void openForTail() throws IOException {
            if (!Files.exists(logPath)) return;
            raf = new RandomAccessFile(logPath.toFile(), "r");
            // choose behaviour: 0 -> read entire file at start; raf.length() -> start only new lines
            lastKnownLength = raf.length(); // skip existing lines by default
            raf.seek(lastKnownLength);
        }

        private void reopenForTail() throws IOException {
            if (raf != null) {
                try { raf.close(); } catch (IOException ignored) {}
                raf = null;
            }
            openForTail();
        }

        private void watchLoop() {
            // WatchService blocks; run it in its own thread and handle interrupts gracefully
            try (WatchService ws = FileSystems.getDefault().newWatchService()) {
                Path parent = logPath.getParent();
                if (parent == null) return;
                parent.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);

                while (running && !Thread.currentThread().isInterrupted()) {
                    WatchKey key;
                    try {
                        key = ws.poll(500, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (key == null) continue;

                    for (WatchEvent<?> ev : key.pollEvents()) {
                        Path changed = (Path) ev.context();
                        if (changed != null && changed.endsWith(logPath.getFileName())) {
                            // try to reopen quickly; poll() will process actual content
                            try { reopenForTail(); } catch (IOException ignored) {}
                        }
                    }
                    key.reset();
                }
            } catch (IOException e) {
                LOGGER.error("watchLoop error: ", e);
            }
        }
    }
}
