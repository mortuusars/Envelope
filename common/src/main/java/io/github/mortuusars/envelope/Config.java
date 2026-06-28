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
        public static final ModConfigSpec.BooleanValue PIGEON_EATS_SEEDS;
        public static final ModConfigSpec.BooleanValue PIGEON_CONVERT_INTO_CHARRED;
        public static final ModConfigSpec.IntValue PIGEON_CONVERT_INTO_CHARRED_TICKS;

        // Charred Pigeon
        public static final ModConfigSpec.BooleanValue CHARRED_PIGEON_SPAWNS_NATURALLY;
        public static final ModConfigSpec.DoubleValue CHARRED_PIGEON_MAIL_CHANCE;
        public static final ModConfigSpec.BooleanValue CHARRED_PIGEON_CONVERT_INTO_REGULAR;
        public static final ModConfigSpec.IntValue CHARRED_PIGEON_CONVERT_INTO_REGULAR_TICKS;

        // Pigeonhole
        public static final ModConfigSpec.DoubleValue PIGEONHOLE_WASTE_INCREASE_CHANCE;
        public static final ModConfigSpec.DoubleValue PIGEONHOLE_WASTE_INCREASE_CHANCE_AFTER_DELIVERY;
        public static final ModConfigSpec.BooleanValue PIGEONHOLE_DISPENSER_WASTE_SCOOPING;

        // Mailbox
        public static final ModConfigSpec.IntValue MAILBOX_ADDRESS_EXPERIENCE_LEVELS_COST;

        // Letter
        public static final ModConfigSpec.BooleanValue LETTER_PAUSE;
        public static final ModConfigSpec.BooleanValue LETTER_BURNING;
        public static final ModConfigSpec.BooleanValue FOX_LETTER_TATTERING;

        // Package
        public static final ModConfigSpec.BooleanValue PACKAGE_SNEAK_QUICK_UNPACK;
        public static final ModConfigSpec.DoubleValue PACKAGE_PAPER_BOX_RETURN_CHANCE;
        public static final ModConfigSpec.DoubleValue PAYBACK_PACKAGE_BOX_RETURN_CHANCE;

        // Delivery
        public static final ModConfigSpec.IntValue DELIVERY_DEFAULT_DISTANCE;
        public static final ModConfigSpec.DoubleValue DELIVERY_COURIER_TRAVEL_SPEED;
        public static final ModConfigSpec.IntValue DELIVERY_TRAVEL_DURATION_DISTANCE_CAP;
        public static final ModConfigSpec.BooleanValue DELIVERY_SPAWNING_RESPECTS_DOMOBSPAWNING_RULE;

        // Payback
        public static final ModConfigSpec.IntValue PAYBACK_REQUEST_DURATION_SHORT;
        public static final ModConfigSpec.IntValue PAYBACK_REQUEST_DURATION_MEDIUM;
        public static final ModConfigSpec.IntValue PAYBACK_REQUEST_DURATION_LONG;

        // Service Addresses
        // Equine Assurance Bureau
        public static final ModConfigSpec.BooleanValue SERVICE_EQUINE_BUREAU_NOTICE_SENDING_ENABLED;

        // Misc
        public static final ModConfigSpec.BooleanValue VILLAGER_FEEDING_PIGEONS;
        public static final ModConfigSpec.BooleanValue VILLAGER_FEEDING_PIGEONS_NITWIT_ONLY;
        public static final ModConfigSpec.DoubleValue ARCHIMEDES_CHANCE;

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

                PIGEON_EATS_SEEDS = builder
                      .comment("Pigeon searches for nearby dropped seeds (envelope:pigeon_food) and eats them.")
                      .define("eats_seeds", true);

                PIGEON_CONVERT_INTO_CHARRED = builder
                      .comment("Pigeon will convert into a Charred Pigeon when it stays in the ultrawarm dimension for some time.", "Default: true")
                      .define("convert_into_charred", true);

                PIGEON_CONVERT_INTO_CHARRED_TICKS = builder
                      .comment("Time (in ticks) Pigeon needs to stay in the ultrawarm dimension to convert into Charred Pigeon.")
                      .defineInRange("convert_into_charred_ticks", 300, 0, Integer.MAX_VALUE);

                builder.pop();
            }

            {
                builder.push("charred_pigeon");
                CHARRED_PIGEON_SPAWNS_NATURALLY = builder
                      .comment("Charred Pigeons can spawn naturally in '#envelope:allows_charred_pigeon_spawns' biomes.",
                            " Default: true")
                      .define("spawns_naturally", true);
                CHARRED_PIGEON_MAIL_CHANCE = builder
                      .comment("Chance of a Charred Pigeon carrying mail when spawned.")
                      .defineInRange("mail_chance", 0.2, 0.0, 1.0);

                CHARRED_PIGEON_CONVERT_INTO_REGULAR = builder
                      .comment("Charred Pigeon will convert into a regular Pigeon when it stays outside of the ultrawarm dimension for some time.", "Default: true")
                      .define("convert_into_regular", true);
                CHARRED_PIGEON_CONVERT_INTO_REGULAR_TICKS = builder
                      .comment("Time (in ticks) Charred Pigeon needs to stay outside of the ultrawarm dimension to convert into a regular Pigeon.")
                      .defineInRange("convert_into_regular_ticks", 300, 0, Integer.MAX_VALUE);
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
                LETTER_BURNING = builder
                      .comment("Letter will burn and disappear when used on `envelope:burning` blocks. Doesn't apply to Sealed Letters.",
                            " Default: true")
                      .define("burning", true);
                FOX_LETTER_TATTERING = builder
                      .comment("Letter will become tattered if a Fox picks it up.")
                      .define("fox_tattering", true);
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
                builder.push("service_addresses");
                {
                    builder.push("equine_assurance_bureau");
                    SERVICE_EQUINE_BUREAU_NOTICE_SENDING_ENABLED = builder
                          .comment("A notice letter will be occasionally sent to the player.", "Default: true")
                          .define("notice_sending_enabled", true);
                    builder.pop();
                }
                builder.pop();
            }

            {
                builder.push("misc");
                VILLAGER_FEEDING_PIGEONS = builder
                      .comment("Villagers will feed nearby pigeons by throwing them seeds.",
                            "Requires 'pigeon.eats_seeds' config option to be enabled.")
                      .define("villager_feeding_pigeons", true);
                VILLAGER_FEEDING_PIGEONS_NITWIT_ONLY = builder
                      .comment("Only Nitwits can feed pigeons.")
                      .define("villager_feeding_pigeons_only_nitwits", true);
                ARCHIMEDES_CHANCE = builder
                      .comment("Chance of an Archimedes spawning when 'envelope:spawns_archimedes' mob is killed by 'envelope:spawns_archimedes' damage type (player explosion by default).")
                      .defineInRange("archimedes_chance", 0.05, 0, 1);
                builder.pop();
            }

            {
                builder.push("debug");
                DEBUG = builder
                      .comment("Enable debug features. Will affect performance negatively. Don't enable unless it's needed.")
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

    public static class Client {
        public static final ModConfigSpec SPEC;

        // JEI
        public static final ModConfigSpec.BooleanValue JEI_SERVICE_ADDRESS_INGREDIENT;

        static {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            {
                builder.push("integration");
                {
                    builder.push("jei");
                    JEI_SERVICE_ADDRESS_INGREDIENT = builder
                          .comment("Service Addresses that have recipes associated with them will be shown in JEI as ingredients.",
                                "Changing the value requires relogging into the world or /reload to take effect.",
                                " Default: true.")
                          .define("service_address_ingredient", true);
                    builder.pop();
                }
                builder.pop();
            }

            SPEC = builder.build();
        }
    }
}