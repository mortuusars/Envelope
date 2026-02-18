# Changelog

## UNRELEASED
Payback Tag
- Added drag and drop from JEI support.
- Added payback request duration selector.
  - Controls how much time buyer will have to pay for the mail. 
  - Item texture changes slightly based on duration defined.
- Using the tag while holding [**Sneak**] will clear the data from it.
- Menu UI/UX improvements.
- Removed crafting table recipes.
- Changed `envelope:payback_tag_contents` item component: its structure is now the same as `envelope:mail_payback_request`.

Package
- Added `envelope:package_experience` item component. Awarded when Package is opened.
- Using the **Package** while holding [**Sneak**] will unpack it without opening the menu.
- Fixed overlays not rendering over package slots.

Crafting
- Added `experience` field to mail crafting recipes. Applied to resulting package and awarded when player opens it.
  - Works the same as in smelting recipes.
- Seal Stamp recipes @[Mail Service] now give 1.5 experience per craft. 

Misc
- Small change to how `envelope:payback_subject` component is defined.

## 0.6.0-Snapshot 1
#### Added Mailing recipes
Send a Package with ingredients to a mail entity - receive crafted result back.
  - Similar to shapeless recipe, but can accept ingredients larger than single item
  - Can do multiple crafts at a time, if more ingredients are provided. As long as they are in same stacks.
  - Results are returned using the same courier, or with service courier, if some of the ingredients were unprocessed 
  - Recipes are data-driven, of course
  - JEI shows available recipes

Included recipes:
  - Payback Tag (no longer craftable in Crafting Table)
  - Seal Stamps of several custom impressions
  - Rotten Flesh -> Leather
  - Saddle

#### Address
- Player address type definition has been changed. Field `id` is now `name`: `{ type:"player", name:"mortuusars" }`
- Entity addresses are now data-driven
  - Address component now references registered entity by its key: `{ type:"entity", entity:"envelope:mail_service" }`
- Added "custom" address type: `{ type:"custom", name:{translate:"display.name"} }`
  - Can be used as a virtual address for display purposes (commands, scripts, etc.). Cannot receive mail.
- Unknown address is now a separate type: `{ type:"unknown" }`

Misc:
- Updated some address type icons (little flags)
- Moved seal textures from `textures/gui/seal` to `textures/seal` folder.
- Moved letters and numbers seal impressions into their respective subfolders. Example: `envelope:c` -> `envelope:letter/c`
  - Sealed Letters and Packages will lose their sealer data when updating to this version.
- Fixed total delivery distance calculation in debug-mode overlay.

## 0.5.2 - 2026-02-06
#### Reworked Packages:
- `Paper Box` is now used to create a package; `Packing Box` has been removed
- Recipe of `Paper Box` has been changed to `4 paper + 1 honeycomb -> 3 boxes`  
- `Package` can no longer be repacked several times. If any item inside the opened `Package` changes - it will break and drop remaining items
- When the `Package` is broken - it has a chance to drop a `Paper Box`
- `Payback Packages` also follow this new system  
- Behavior of `Package` block breaking is now inverted:
  - Just breaking will drop the intact `Package `
  - Sneaking will destroy the `Package` and drop its contents
- Smaller improvements and fixes

#### Misc
- _doMobSpawning_ game rule no longer prevents delivering pigeons from spawning. _Configurable._ 
- Fixed crash in Letter view screen  
- Fixed error with `C2ME`

## 0.5.1 - 2026-01-31
- Removed `Requires debug mode` message in chat on world load.

## 0.5.0 - 2026-01-30
[The mod is not fully finished. Use in forever world is not recommended.]
- Release

## 0.4.0.5
- Config translation
- Added mod icon

## 0.4.0.4
Pigeon:
- Added Passenger Pigeon variant.
  - Spawns only in old growth biomes, or taiga village.
- Offspring variant is now dependent on parents, with a 50/50 chance, instead of random from all variants.
- Pigeons are now hunted by cats, ocelots and foxes.
- Pigeons are now sitting occasionally when tired.
- Service pigeons now wear a mailman hat.
- Small changes to pigeon movement and AI.
- Small updates and changes to pigeon textures, model and animations.
- Added `pigeon.damage_evasion_chance_when_delivering` config option.

Package:
- Added `minecraft:container_loot` support for packages.
- Package that will be destroyed after opening now displays a warning sign in the item tooltip.
- Opening a Package that will be destroyed after opening now requires holding the use button, to not unpack accidentally.

## 0.4.0.3
- Added Courier Death Notice. 
- Reworked how delivery route and delivery duration is calculated. 
  - It's somewhat involved, maybe I'll explain it the future in a wiki or something. 
- Reduced `courier_travel_speed` config value from 25 to 20.

## 0.4.0.2 - Reworks, reworks, reworks
- Pigeonhole has been split into two blocks:
  - Added Mailbox block. This is now a main interaction point with the mail system.
    - Upon placing a Mailbox, player will be asked to provide an address for it. (Address Tag can still be used to change existing address)
    - When mailbox has mail to send and food for pigeons, its front hatch will open - allowing nearby pigeons to pick it up for delivery.  
  - Pigeonhole no longer acts like a mailbox. Now it's just a place for pigeons to chill.
- Reduced xp requirement for setting/changing mailbox address from 5 to 3 levels.
- Mail items:
  - Tooltips now have separate lines for sender/recipient addresses. 
  - Delivered mail no longer keeps sending data (recipient and payback).
  - Delivery Log can now be seen in the mail item tooltip by pressing [Shift], if mail has a sender component.
  - Using Shears on the mail item in inventory (clicking the item with them) removes delivery data.
- Added recipe for Payback Tag
- Payback timeout config option is now in minutes instead of seconds. Config will update automatically as it's name changed as well.
- Changed Address Tag recipe: it's now shapeless, requires only one sign and gives one tag.
- Heavy changes to internal systems. Renamed several item components.
  - Some existing stuff will disappear from the world again.
- Many changes and improvements to delivery system to make it more robust.
- Many smaller changes and tweaks.

## 0.4.0.1
[Some existing envelope world data might disappear when updating to this version]

- Added Payback system
  - Added Payback Tag, Payback Packing Box and Payback Package
  - Mail with Payback will be handled by Mail Service for safe transfer of goods and payment between addresses
- Added Packing Box - separate item type that represents an empty package; Package usage experience remains basically the same
- Improved visual style of sender/recipient addresses on mailable items 
- Changed Mail Service address icon
- Renamed address type `pigeonhole` to `block` 
- Fixed relocating Pigeonhole block with CarryOn not removing background Pigeonhole data
  - This system will most likely receive proper rework in the future updates  
- Smaller changes and improvements

## 0.3.0.2
- Package
  - Added **Sealed Package**
  - Updated textures
  - Small changes to Package block behavior and other mechanics


- Seal materials and impressions are now data-driven

## 0.3.0.1
- Letter
  - Added **Sealed Letter** 
  - Added letter copying recipe
  - Added `envelope:letter_tattered` data component for Letter and Sealed Letter 
    - Letters become tattered when a Fox spits it out from its mouth
    - Tattered letters differ only in appearance, functionality is the same
  - Updated how existing letters work and look


- Seal
  - Added **Seal Stamp**
    - Right click with a stamp on a sealable item in inventory to add a Seal
  - Seals can have impressions of letters, numbers or various other symbols on them
  - Using a regular seal stamp will produce an impression with the first character of sealer's signature (name)
  - Seals are shown in tooltips of sealed items along with the sealer signature

## 0.2.0.2
[Existing blocks and other stuff might (or will) break/reset when updating to this version]

Delivery:
- Rewritten delivery system to fix some bugs and introduce new ones
- Added 'ability to reach' check for `approaching_recipient` phase, in addition to `departing_sender`
- Delivery travel duration is now based on distance between addresses
  - Added config options for delivery duration
  - Removed `envelope:mail_travel_duration` item data component
- Improved transition points (where pigeon appears/disappears)


- Changed address tag GUI
  - Added proper autocomplete suggestions popup
- Increased address length limit from 22 to 40 characters
- Removed `envelope:mail_id`, `envelope:mail_delivery_log`, `envelope:mail_status` item data components
  - These things are now handled internally. Not much has changed for the player, they were for internal use anyway
- Pigeon movement should be smoother in some cases
- Added `/envelope pigeonhole` commands
- Changed command `/mail send [item]` to `/envelope send [item]`
- Pigeons released from Pigeonhole for emergency reasons will no longer start delivering mail as usual 
- Restricted all mail operations to overworld only
- Improvements to Bugger (Debug utility enabled with `debug.debug_mode` server config option. Adds more info to F3 screen, more log messages, etc).

## 0.2.0.1
- Added restrictions for items that can be placed inside a Package:
  - Added Bundle to `envelope:cannot_be_packaged` tag
  - Package now also checks `item.canFitInsideContainerItems()`. This will restrict items like Shulker Box, Supplementaries Sack, etc.
- Improvements to pigeon delivery
  - Pigeons that return home when the chunk is unloaded will properly spawn when it's loaded again.
  - Pigeons are now properly unloaded at the edges of active chunks (and in other places as well).
  - Pigeons now attempt to spawn continuously when delivering, instead of just at the beginning of new phase.
  - Many other changes and cleanups of delivery system.
- Added simple mail command `/mail send [item]`
  - `sender` and `recipient` must be defined (sender can be fake)
- Added test NPC (mail entity) implementation: Villager
  - When a Letter is sent to this address, returns a reply (a primitive one)
- Pigeon Spawn Egg could be used to conveniently place pigeons inside Pigeonhole.
- Many small improvements.
- Fixed Pigeonhole comparator output not updating if mail was received when the block was unloaded.
- Fixed crash when delivering items with enchantments.

## 0.1.0
- init