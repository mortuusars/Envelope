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
        public static final ModConfigSpec.IntValue PIGEON_TIRED_AFTER_DELIVERY_TICKS;
        public static final ModConfigSpec.BooleanValue PIGEON_HUNTED_BY_CAT;
        public static final ModConfigSpec.BooleanValue PIGEON_HUNTED_BY_OCELOT;
        public static final ModConfigSpec.BooleanValue PIGEON_HUNTED_BY_FOX;
        public static final ModConfigSpec.DoubleValue PIGEON_DAMAGE_EVASION_CHANCE_WHILE_DELIVERING;

        // Pigeonhole
        public static final ModConfigSpec.DoubleValue PIGEONHOLE_WASTE_INCREASE_CHANCE;
        public static final ModConfigSpec.DoubleValue PIGEONHOLE_WASTE_INCREASE_CHANCE_AFTER_DELIVERY;
        public static final ModConfigSpec.BooleanValue PIGEONHOLE_DISPENSER_WASTE_SCOOPING;

        // Mailbox
        public static final ModConfigSpec.IntValue MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST;

        // Letter
        public static final ModConfigSpec.BooleanValue LETTER_PAUSE;

        // Package
        public static final ModConfigSpec.BooleanValue PACKAGE_SNEAK_QUICK_UNPACK;
        public static final ModConfigSpec.DoubleValue PACKAGE_PAPER_BOX_RETURN_CHANCE;
        public static final ModConfigSpec.DoubleValue PAYBACK_PACKAGE_BOX_RETURN_CHANCE;

        // Delivery
        public static final ModConfigSpec.IntValue DELIVERY_DEFAULT_DISTANCE;
        public static final ModConfigSpec.DoubleValue DELIVERY_COURIER_TRAVEL_SPEED;
        public static final ModConfigSpec.IntValue DELIVERY_TRAVEL_DURATION_DISTANCE_CAP;
        public static final ModConfigSpec.IntValue DELIVERY_PAYBACK_TIMEOUT_MINUTES;
        public static final ModConfigSpec.BooleanValue DELIVERY_SPAWNING_RESPECTS_DOMOBSPAWNING_RULE;

        // PAYBACK
        public static final ModConfigSpec.IntValue PAYBACK_REQUEST_DURATION_SHORT;
        public static final ModConfigSpec.IntValue PAYBACK_REQUEST_DURATION_MEDIUM;
        public static final ModConfigSpec.IntValue PAYBACK_REQUEST_DURATION_LONG;

        // Debug
        public static final ModConfigSpec.BooleanValue DEBUG;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            {
                builder.push("pigeon");
                PIGEON_SPAWNS_NATURALLY = builder
                      .comment("Pigeons can spawn naturally in '#envelope:allows_pigeon_spawns' biomes.",
                            " Default: true")
                      .define("spawns_naturally", true);
                PIGEON_SPAWNS_IN_VILLAGE = builder
                      .comment("Pigeons can spawn in the village (similar to Cats).",
                            " Default: true")
                      .define("spawns_in_village", true);
                PIGEON_MIN_TICKS_INSIDE_PIGEONHOLE = builder
                      .comment("Minimum time (in ticks) that Pigeon will be sitting in a Pigeonhole after entering.")
                      .defineInRange("min_ticks_inside_pigeonhole", 1200, 1, Integer.MAX_VALUE);
                PIGEON_MIN_TICKS_OUTSIDE_PIGEONHOLE = builder
                      .comment("Minimum time (in ticks) that Pigeon will spend outside of a Pigeonhole.")
                      .defineInRange("min_ticks_outside_pigeonhole", 2400, 100, Integer.MAX_VALUE);
                PIGEON_TIRED_AFTER_DELIVERY_TICKS = builder
                      .comment("Time (in ticks) for which Pigeon will be tired after finishing a delivery.")
                      .defineInRange("ticks_tired_after_delivery", 6000, 0, Integer.MAX_VALUE);

                PIGEON_HUNTED_BY_CAT = builder
                      .comment("Cat will hunt and kill pigeons.", "Default: true")
                      .define("hunted_by_cat", true);
                PIGEON_HUNTED_BY_OCELOT = builder
                      .comment("Ocelot will hunt and kill pigeons.", "Default: true")
                      .define("hunted_by_ocelot", true);
                PIGEON_HUNTED_BY_FOX = builder
                      .comment("Fox will hunt and kill pigeons.", "Default: true")
                      .define("hunted_by_fox", true);

                PIGEON_DAMAGE_EVASION_CHANCE_WHILE_DELIVERING = builder
                      .comment("Chance to evade damage while delivering. `#envelope:bypasses_pigeon_delivery_evasion` tag can be used to control which damage types will not be affected.")
                      .defineInRange("damage_evasion_chance_while_delivering", 0.0, 0.0, 1.0);

                builder.pop();
            }

            {
                builder.push("pigeonhole");
                PIGEONHOLE_WASTE_INCREASE_CHANCE = builder
                      .comment("Chance of waste level increasing when pigeon exits the block.")
                      .defineInRange("waste_level_increase_chance", 0.2, 0.0, 1.0);
                PIGEONHOLE_WASTE_INCREASE_CHANCE_AFTER_DELIVERY = builder
                      .comment("Chance of waste level increasing when pigeon exits the block, if it was tired (finished a delivery before resting in a Pigeonhole).")
                      .defineInRange("waste_level_increase_chance_after_delivery", 1.0, 0.0, 1.0);
                PIGEONHOLE_DISPENSER_WASTE_SCOOPING = builder
                      .comment("Waste from Pigeonhole can be scooped with dispenser that has a shovel (#envelope:waste_scoopable).",
                            " Default: true.")
                      .define("dispenser_waste_scooping", true);
                builder.pop();
            }

            {
                builder.push("mailbox");
                MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST = builder
                      .comment("Levels of experience needed to set or change the address.")
                      .defineInRange("address_experience_levels_cost", 3, 0, 128);
                builder.pop();
            }

            {
                builder.push("letter");
                LETTER_PAUSE = builder
                      .comment("Letter screen pauses singleplayer game.",
                            " Default: false")
                      .define("pause", false);
                builder.pop();
            }

            {
                builder.push("package");
                PACKAGE_SNEAK_QUICK_UNPACK = builder
                      .comment("Holding Sneak while using the Package will unpack and destroy it immediately, instead of opening the menu.",
                            "Default: true")
                      .define("sneak_quick_unpack", true);
                PACKAGE_PAPER_BOX_RETURN_CHANCE = builder
                      .comment("Chance of a Package \"recycling\" back into Paper Box after opening.")
                      .defineInRange("paper_box_return_chance", 0.5, 0.0, 1.0);
                PAYBACK_PACKAGE_BOX_RETURN_CHANCE = builder
                      .comment("Chance of Payback Package \"recycling\" back into Payback Box after opening, allowing another try to pack the payment.")
                      .defineInRange("payback_box_return_chance", 0.75, 0.0, 1.0);
                builder.pop();
            }

            {
                builder.push("delivery");
                DELIVERY_DEFAULT_DISTANCE = builder
                      .comment("Default distance (in blocks) that will be used if distance between two addresses cannot be determined (recipient does not exist, for example).")
                      .defineInRange("default_distance", 1500, 1, Integer.MAX_VALUE);
                DELIVERY_COURIER_TRAVEL_SPEED = builder
                      .comment("Courier speed (in blocks per second) while in traveling (background) phases.")
                      .defineInRange("courier_travel_speed", 20.0, 0.01, 9999.0);
                DELIVERY_TRAVEL_DURATION_DISTANCE_CAP = builder
                      .comment("Distance (in blocks) after which travel duration stops increasing and stays at maximum value.")
                      .defineInRange("travel_duration_distance_cap", 5000, 1, Integer.MAX_VALUE);
                DELIVERY_PAYBACK_TIMEOUT_MINUTES = builder
                      .comment("How long (in minutes) Mail Service will wait for payment before returning the mail back to sender.")
                      .defineInRange("payback_timeout_minutes", 60, 1, Integer.MAX_VALUE);
                DELIVERY_SPAWNING_RESPECTS_DOMOBSPAWNING_RULE = builder
                      .comment("Delivering pigeons will not spawn when 'doMobSpawning' rule is set to 'false'.",
                            "Default: false (spawn anyway)")
                      .define("spawning_respects_domobspawning_rule", false);
                builder.pop();
            }

            {
                builder.push("payback");
                PAYBACK_REQUEST_DURATION_SHORT = builder
                      .comment("'Short' payback request duration (in minutes).")
                      .defineInRange("request_duration_short", 30, 1, Integer.MAX_VALUE);
                PAYBACK_REQUEST_DURATION_MEDIUM = builder
                      .comment("'Medium' payback request duration (in minutes). Default.")
                      .defineInRange("request_duration_medium", 180, 1, Integer.MAX_VALUE);
                PAYBACK_REQUEST_DURATION_LONG = builder
                      .comment("'Long' payback request duration (in minutes).")
                      .defineInRange("request_duration_long", 4320, 1, Integer.MAX_VALUE);
                builder.pop();
            }

            {
                builder.push("debug");
                DEBUG = builder
                      .comment("Enable debug features. Will affect performance negatively. Don't enable unless it's needed.",
                            " Default: false.")
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