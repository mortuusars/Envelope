package io.github.mortuusars.envelope;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Using ForgeConfigApiPort on fabric allows using forge config in both environments and without extra dependencies on forge.
 */
public abstract class Config {
    public static abstract class Server {
        private static ModConfigSpec.Builder BUILDER;

        public static final ModConfigSpec SPEC;

        public static final ModConfigSpec.IntValue MAIL_TRAVEL_DURATION;

        public static class Pigeonhole {
            public static final ModConfigSpec.BooleanValue DISPENSER_WASTE_SCOOPING;
            public static final ModConfigSpec.IntValue ADDRESS_EXPERIENCE_LEVELS_COST;

            static {
                BUILDER.push("pigeonhole");
                DISPENSER_WASTE_SCOOPING = BUILDER
                      .comment("Waste from Pigeonhole can be scooped with dispenser that has a shovel (#envelope:waste_scoopable). Default: true.")
                      .define("dispenser_waste_scooping", true);
                ADDRESS_EXPERIENCE_LEVELS_COST = BUILDER
                        .comment("Levels of experience needed to set or change an address. Default: 5")
                        .defineInRange("address_experience_levels_cost", 5, 0, 128);
                BUILDER.pop();
            }

            public static void init() { }
        }

        public static class Pigeon {
            public static final ModConfigSpec.BooleanValue SPAWNS_NATURALLY;
            public static final ModConfigSpec.BooleanValue SPAWNS_IN_VILLAGE;
            public static final ModConfigSpec.IntValue MIN_TICKS_INSIDE_PIGEONHOLE;
            public static final ModConfigSpec.IntValue MIN_TICKS_OUTSIDE_PIGEONHOLE;

            static {
                BUILDER.push("pigeon");
                SPAWNS_NATURALLY = BUILDER
                        .comment("Pigeon can spawn naturally in '#envelope:allows_pigeon_spawns' biomes. Default: true")
                        .define("spawns_naturally", true);
                SPAWNS_IN_VILLAGE = BUILDER
                        .comment("Pigeon can spawn in the village (similar to Cats). Default: true")
                        .define("spawns_in_village", true);
                MIN_TICKS_INSIDE_PIGEONHOLE = BUILDER
                      .comment("Minimum time (in ticks) that a Pigeon will be sitting in the Pigeonhole after entering. Default: 600 (30 seconds)")
                      .defineInRange("min_ticks_inside_pigeonhole", 600, 1, Integer.MAX_VALUE);
                MIN_TICKS_OUTSIDE_PIGEONHOLE = BUILDER
                      .comment("Minimum time (in ticks) that a Pigeon will spend outside of a Pigeonhole. Default: 1200 (1 min)")
                      .defineInRange("min_ticks_outside_pigeonhole", 1200, 100, Integer.MAX_VALUE);
                BUILDER.pop();
            }

            public static void init() { }
        }

        public static class Letter {
            public static final ModConfigSpec.BooleanValue PAUSE;

            static {
                BUILDER.push("letter");
                PAUSE = BUILDER.comment("Letter screen pauses singleplayer game. Default: false")
                        .define("pause", false);
                BUILDER.pop();
            }

            public static void init() { }
        }

        public static class Package {
            public static final ModConfigSpec.IntValue PACK_LIMIT;

            static {
                BUILDER.push("package");
                PACK_LIMIT = BUILDER.comment("Number of packs that a single package can handle.",
                                "(When reached, the item will be destroyed after all items are removed from it).", "Default: 3")
                        .defineInRange("pack_limit", 3, 1, Integer.MAX_VALUE);
                BUILDER.pop();
            }

            public static void init() { }
        }

        static {
            BUILDER = new ModConfigSpec.Builder();

            {
                BUILDER.push("mail");
                MAIL_TRAVEL_DURATION = BUILDER
                        //TODO: change default travel duration
                        .comment("Default travel duration in ticks. Default: 50")
                        .defineInRange("travel_duration", 50, 1, Integer.MAX_VALUE);
                BUILDER.pop();
            }

            Pigeonhole.init();
            Pigeon.init();
            Letter.init();
            Package.init();

            SPEC = BUILDER.build();
            BUILDER = null;
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