package io.github.mortuusars.envelope.world.mail.address;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class SimpleBlockAddressGenerator {
    private final AllAddresses knownAddresses;
    private final int tries;

    public SimpleBlockAddressGenerator(AllAddresses knownAddresses, int tries) {
        Preconditions.checkArgument(tries > 0, "Should have at least 1 try.");
        this.knownAddresses = knownAddresses;
        this.tries = tries;
    }

    public @NotNull BlockAddress generate(RandomSource random) {
        String[] adjectives = getAdjectives();
        String[] nouns = getNouns();

        int count = 0;
        String id = "";

        do {
            if (count > tries) {
                return new BlockAddress(id);
            }
            count++;

            String firstAdjective = Util.getRandom(adjectives, random);
            String secondAdjective = "";

            if (random.nextFloat() > 0.5) {
                do {
                    secondAdjective = Util.getRandom(adjectives, random);
                }
                while (secondAdjective.equals(firstAdjective)); // To not repeat
            }

            String noun = Util.getRandom(nouns, random);

            id = secondAdjective.isEmpty()
                  ? firstAdjective + " " + noun
                  : firstAdjective + " " + secondAdjective + " " + noun;
        } while (knownAddresses.isKnown(id));

        return new BlockAddress(id);
    }

    private String[] getAdjectives() {
        return new String[]{
              "Silent",
              "Old",
              "Rugged",
              "Tattered",
              "Rusty",
              "Battered",
              "Tired",
              "Lost",
              "Decayed",
              "Faded",
              "Fogged",
              "Missed",
              "Time-lost",
              "Marked",
              "Hidden",
              "Open",
              "Dusty",
              "Lush",
              "Wet",
              "Calm",
              "Wide",
              "Broad",
              "Bright",
              "Light",
              "Dark",
              "Majestic",
              "Wonderful",
              "Great",
              "Enchanting",
              "Mesmerizing",
              "Dim",
              "Pale",
              "Gray",
              "Wild",
              "Secluded",
              "Narrow",
              "Empty",
              "Forgotten",
              "Waiting",
              "Prime",
              "Secondary",
              "Static",
              "Brisk",
              "Agile"
        };
    }

    private String[] getNouns() {
        return new String[]{
              "Wood",
              "Stone",
              "Brick",
              "Oak",
              "Ash",
              "Clay",
              "Crossroad",
              "Corner",
              "Gate",
              "Yard",
              "Path",
              "Field",
              "Dock",
              "Lane",
              "Square",
              "Post",
              "Crow",
              "Bird",
              "Sparrow",
              "Fox",
              "Wolf",
              "Rabbit",
              "Pigeon",
              "Cat",
              "Frog",
              "Raven",
              "Bat",
              "Sheep",
              "Stag",
              "Pine",
              "Reed",
              "Moss",
              "Ember",
              "Frost",
              "Grass",
              "Ice",
              "Snow",
              "Air",
              "Gust",
              "Storm",
              "Peace",
              "Wind",
              "Brook",
              "Hill",
              "Place",
              "Nook",
              "Retreat",
              "Outpost"
        };
    }
}
