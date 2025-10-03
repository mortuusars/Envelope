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

        public static class Pigeon {
            public static final ModConfigSpec.BooleanValue SPAWNS_NATURALLY;
            public static final ModConfigSpec.BooleanValue SPAWNS_IN_VILLAGE;

            static {
                BUILDER.push("pigeon");
                SPAWNS_NATURALLY = BUILDER
                        .comment("Pigeon can spawn naturally in '#envelope:allows_pigeon_spawns' biomes. Default: true")
                        .define("spawns_naturally", true);
                SPAWNS_IN_VILLAGE = BUILDER
                        .comment("Pigeon can spawn in the village (similar to Cats). Default: true")
                        .define("spawns_in_village", true);
                BUILDER.pop();
            }

            public static void init() { }
        }

        public static class Letter {
            public static final ModConfigSpec.BooleanValue PAUSE;

            static {
                BUILDER.push("letter");
                PAUSE = BUILDER.comment("Letter screen pauses singleplayer game. Default: false")
                        .define("letter_pause", false);
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

            Letter.init();

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