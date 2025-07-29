package io.github.mortuusars.envelope;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Using ForgeConfigApiPort on fabric allows using forge config in both environments and without extra dependencies on forge.
 */
public class Config {
    public static class Server {
        public static final ModConfigSpec SPEC;

        public static final ModConfigSpec.IntValue TRAVEL_DURATION;

        // Pigeon
        public static final ModConfigSpec.BooleanValue PIGEON_SPAWNS_NATURALLY;
        public static final ModConfigSpec.BooleanValue PIGEON_SPAWNS_IN_VILLAGE;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            {
                builder.push("mail");
                TRAVEL_DURATION = builder
                        //TODO: change default travel duration
                        .comment("Default travel duration in ticks. Default: 50")
                        .defineInRange("travel_duration", 50, 1, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("pigeon");
                PIGEON_SPAWNS_NATURALLY = builder
                        .comment("Pigeon can spawn naturally in #envelope:allows_pigeon_spawns biomes. Default: true")
                        .define("spawns_naturally", true);
                PIGEON_SPAWNS_IN_VILLAGE = builder
                        .comment("Pigeon can spawn in the village (similar to how Cats do). Default: true")
                        .define("spawns_in_village", true);
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