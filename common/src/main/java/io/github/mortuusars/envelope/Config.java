package io.github.mortuusars.envelope;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Using ForgeConfigApiPort on fabric allows using forge config in both environments and without extra dependencies on forge.
 */
public abstract class Config {
    public static abstract class Server {
        public static final ModConfigSpec SPEC;

        // Pigeon
        public static final ModConfigSpec.BooleanValue PIGEON_SPAWNS_NATURALLY;
        public static final ModConfigSpec.BooleanValue PIGEON_SPAWNS_IN_VILLAGE;
        public static final ModConfigSpec.IntValue PIGEON_MIN_TICKS_INSIDE_PIGEONHOLE;
        public static final ModConfigSpec.IntValue PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE;

        // Pigeonhole
        public static final ModConfigSpec.BooleanValue PIGEONHOLE_DISPENSER_WASTE_SCOOPING;
        public static final ModConfigSpec.IntValue PIGEONHOLE_ADDRESS_EXPERIENCE_LEVELS_COST;

        // Letter
        public static final ModConfigSpec.BooleanValue LETTER_PAUSE;

        // Package
        public static final ModConfigSpec.IntValue PACKAGE_PACK_LIMIT;

        // Mail
        public static final ModConfigSpec.IntValue MAIL_TRAVEL_DURATION;

        // Debug
        public static final ModConfigSpec.BooleanValue DEBUG;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            {
                builder.push("mail");
                MAIL_TRAVEL_DURATION = builder
                        //TODO: change default travel duration
                        .comment("Default travel duration in ticks. Default: 50")
                        .defineInRange("travel_duration", 50, 1, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("pigeon");
                PIGEON_SPAWNS_NATURALLY = builder
                      .comment("Pigeon can spawn naturally in '#envelope:allows_pigeon_spawns' biomes. Default: true")
                      .define("spawns_naturally", true);
                PIGEON_SPAWNS_IN_VILLAGE = builder
                      .comment("Pigeon can spawn in the village (similar to Cats). Default: true")
                      .define("spawns_in_village", true);
                PIGEON_MIN_TICKS_INSIDE_PIGEONHOLE = builder
                      .comment("Minimum time (in ticks) that a Pigeon will be sitting in the Pigeonhole after entering. Default: 600 (30 seconds)")
                      .defineInRange("min_ticks_inside_pigeonhole", 600, 1, Integer.MAX_VALUE);
                PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE = builder
                      .comment("Minimum time (in ticks) that a Pigeon will spend outside of a Pigeonhole. Default: 1200 (1 min)")
                      .defineInRange("min_ticks_outside_pigeonhole", 1200, 100, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("pigeonhole");
                PIGEONHOLE_DISPENSER_WASTE_SCOOPING = builder
                      .comment("Waste from Pigeonhole can be scooped with dispenser that has a shovel (#envelope:waste_scoopable). Default: true.")
                      .define("dispenser_waste_scooping", true);
                PIGEONHOLE_ADDRESS_EXPERIENCE_LEVELS_COST = builder
                      .comment("Levels of experience needed to set or change an address. Default: 5")
                      .defineInRange("address_experience_levels_cost", 5, 0, 128);
                builder.pop();
            }

            {
                builder.push("letter");
                LETTER_PAUSE = builder.comment("Letter screen pauses singleplayer game. Default: false")
                      .define("pause", false);
                builder.pop();
            }

            {
                builder.push("package");
                PACKAGE_PACK_LIMIT = builder.comment("Number of packs that a single package can handle.",
                            "(When reached, the item will be destroyed after all items are removed from it).", "Default: 3")
                      .defineInRange("pack_limit", 3, 1, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("debug");
                DEBUG = builder.comment("Enable debug features. Default: false.").define("debug", false);
                builder.pop();
            }

            SPEC = builder.build();
        }
    }

    /*public static class Common {
        public static final ModConfigSpec SPEC;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            SPEC = builder.build();
        }
    }*/

//    public static class Client {
//        public static final ModConfigSpec SPEC;
//
//        static {
//            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
//
//            SPEC = builder.build();
//        }
//    }
}