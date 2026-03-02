package io.github.mortuusars.envelope.client.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientStateManager {
    //TODO: Rework for individual states. Use gameDir instead of configDir.

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE_PATH = Platform.getConfigDirectory().resolve(Envelope.ID + "/fill_recipient.json");
    private static FillRecipientState data;

    public static void load() {
        if (Files.exists(FILE_PATH)) {
            try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
                data = GSON.fromJson(reader, FillRecipientState.class);
            } catch (IOException e) {
                Envelope.LOGGER.error("Cannot load fill recipient state from '{}': {}", FILE_PATH, e.toString());
                data = new FillRecipientState();
            }
        } else {
            data = new FillRecipientState();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            Envelope.LOGGER.error("Cannot save fill recipient state to '{}': {}", FILE_PATH, e.toString());
        }
    }

    public static FillRecipientState getFillRecipientState() {
        if (data == null) {
            load();
        }
        return data;
    }
}