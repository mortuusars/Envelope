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

        // Delivery
        public static final ModConfigSpec.DoubleValue DELIVERY_COURIER_TRAVEL_SPEED;
        public static final ModConfigSpec.IntValue DELIVERY_TRAVEL_DURATION_DISTANCE_CAP;

        // Debug
        public static final ModConfigSpec.BooleanValue DEBUG;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            {
                builder.push("pigeon");
                PIGEON_SPAWNS_NATURALLY = builder
                      .comment(" Pigeon can spawn naturally in '#envelope:allows_pigeon_spawns' biomes. Default: true")
                      .define("spawns_naturally", true);
                PIGEON_SPAWNS_IN_VILLAGE = builder
                      .comment(" Pigeon can spawn in the village (similar to Cats). Default: true")
                      .define("spawns_in_village", true);
                PIGEON_MIN_TICKS_INSIDE_PIGEONHOLE = builder
                      .comment(" Minimum time (in ticks) that a Pigeon will be sitting in the Pigeonhole after entering. Default: 600 (30 seconds)")
                      .defineInRange("min_ticks_inside_pigeonhole", 600, 1, Integer.MAX_VALUE);
                PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE = builder
                      .comment(" Minimum time (in ticks) that a Pigeon will spend outside of a Pigeonhole. Default: 1200 (1 min)")
                      .defineInRange("min_ticks_outside_pigeonhole", 1200, 100, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("pigeonhole");
                PIGEONHOLE_DISPENSER_WASTE_SCOOPING = builder
                      .comment(" Waste from Pigeonhole can be scooped with dispenser that has a shovel (#envelope:waste_scoopable). Default: true.")
                      .define("dispenser_waste_scooping", true);
                PIGEONHOLE_ADDRESS_EXPERIENCE_LEVELS_COST = builder
                      .comment(" Levels of experience needed to set or change an address. Default: 5")
                      .defineInRange("address_experience_levels_cost", 5, 0, 128);
                builder.pop();
            }

            {
                builder.push("letter");
                LETTER_PAUSE = builder.comment(" Letter screen pauses singleplayer game. Default: false")
                      .define("pause", false);
                builder.pop();
            }

            {
                builder.push("package");
                PACKAGE_PACK_LIMIT = builder.comment(" Number of packs that a single package can handle.",
                            " (When reached, the item will be destroyed after all items are removed from it).", "Default: 3")
                      .defineInRange("pack_limit", 3, 1, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("delivery");
                DELIVERY_COURIER_TRAVEL_SPEED = builder
                      .comment(" Courier speed (in blocks per second) while in traveling (background) phases. Default: 25.0")
                      .defineInRange("courier_travel_speed", 25.0, 0.0001, 9999.0);
                DELIVERY_TRAVEL_DURATION_DISTANCE_CAP = builder
                      .comment(" Distance (in blocks) after which travel duration stops increasing and stays at maximum value. Default: 5000")
                      .defineInRange("travel_duration_distance_cap", 5000, 1, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("debug");
                DEBUG = builder
                      .comment(" Enable debug features. Will affect performance negatively. Don't enable unless it's needed. Default: false.")
                      .define("debug_mode", false);
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