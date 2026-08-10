# Changelog

## 0.7.4 - 2026-08-10
- Fixed item dupe related to packages. Containers are hard.

## 0.7.3 - 2026-08-08
- Fixed dupe bug with Payback Tag and inventory sorter mods.

## 0.7.2 - 2026-07-27
- Fixed pigeon head not tilting up/down to look at stuff when flying
- Fixed KubeJS handleMailDropOff event not working correctly.
- Removed duplicated amethyst shard entry in `envelope:packages/lost_mail/valuables` loot table
- Reduced chance of recipe packages dropping from Charred Pigeon.

## 0.7.1 - 2026-07-23
- Fixed service crafting returning the input package instead of a package with leftover items when not all items were used in the craft.
- Fixed transferring all mail from inbox with Ctrl+Shift+Click hanging the client indefinitely. 

## 0.7.0 - Integration Hell - 2026-07-21

### Devlog / Showcase: 
https://youtu.be/cYj1cDlv14w

### New Features
- Added Collapsed Mail Hub structure
  - Piece of once great infrastructure now in disrepair. Generates high in the sky.
- Added Plains, Taiga, Savanna and Desert Dovecotes
- Added Charred Pigeon
  - Spawns in the nether in all biomes except Basalt Deltas
  - 1 in 5 pigeons will spawn carrying a Package
  - When brought into Overworld, will turn into a regular pigeon after some time.
    - Converted pigeons will have special "charred" variant
  - Regular pigeon that stays in the nether for some time will be converted into Charred Pigeon
- Added Pigeonhole variants for all wood types
- Added unique sounds to Pigeonhole waste scooping and pigeon entering, leaving and working
- Added ability to place Letters on the wall (by holding sneak)
- Added Letter burning: using the item on a fire or campfire will consume the letter
  - Doesn't apply to Sealed Letters
- Added ability to add a 5th box to Paper Box block, which will make it a full block
- Added fall absorption property to Paper Box block
  - The block will be broken, dropping boxes on the ground
- Added Nitwit villager behavior: throwing seeds for nearby pigeons
- Added new Pigeon behaviors:
  - Eating seeds dropped on the ground
  - Following Nitwits to potentially get some seeds
- Added 1% chance of receiving a Diamond when scooping waste from a Pigeonhole  
  - Grants hidden advancement
- Added 'hanging' property to Mailbox block
  - When placed on the side of a block, the model will have slightly different style
- Added Payback Request Canceling mailing recipe to Mail Service address
  - Last request will be canceled
- Added special "Archimedes" Pigeon variant
- Added Paper Boxes and Letters to Abandoned Mineshaft and Pillager Outpost chests
- Added advancements:
  - Dirt to Diamonds
  - Just Don't Smoke It
  - And You Did It Anyway
  - Foxtile Environment
  - Overworld Wide Web
  - What Is My Purpose?
  - It's Filthy in There
  - Soft Landing
  - System Failure
- Added stats:
  - Interactions with Mailbox
  - Mail Deliveries
  - Seals Applied
  - Seals Broken
  - Letters Folded
  - Packages Created
  - Packages Opened

### Changes and Fixes
Pigeon
- Improved wandering:
  - Pigeons now perch occasionally on leaves and wooden blocks, similar to parrots
  - Reduced wandering movement speed slightly
  - Other minor changes and fixes
- Improved and fixed pigeon's "home" mechanic:
  - When a Pigeon exits Pigeonhole - it becomes its home position
  - Pigeon will not wander outside 24 block radius of it
  - Pigeon will pathfind back to the home radius if it happens to be outside of it
  - Pigeon will forget its home after 3 in-game days of not resting inside a Pigeonhole, and will be free to wander off anywhere
  - Pigeon can still enter any Pigeonhole when it already has a home, which will make it its new home
- Small adjustment to pigeon spawning in village to reduce spawn counts in some cases
- Increased Pigeonhole location distance
- Minor adjustments to predator avoidance behavior
- Tired pigeons fly slower now
- Pigeons named "Drumstick" will have a slightly chonkier model
- Minor tweaks of Pigeon textures:
  - Slightly darkened underside of wings and tail.
  - Fixed texture of the underside of a Pigeon's wing being flipped.
- Fixed wings of pigeons sitting in boats or seats having wrong position
- Fixed position of Create's Conductor and Logistics hat on a Pigeon
- Fixed pigeons exiting (and re-entering immediately) the Pigeonhole when it's raining without thunderstorm

Pigeonhole
- Improved pigeon behavior when the block is smoked
- Waste scooping now uses loot table for drops
- Fixed waste generation of tired pigeons being less than intended

Mailbox
- Adjusted Mailbox collision shape to better reflect the model
  - Fixes some cases of wrong culling

Services
- Equine Assurance Bureau notice letter sending is now triggered by taming an animal instead of at random to all players at once
  - Every sent letter decreases the chance of the next. Sending them Gold Block also decreases the chance
  - Taming animals other than horses have 1 in 5 chance to trigger sending (which then also stacks with reduced chance of sending)
- Reduced virtual distance to service addresses:
  - Mail Service (1000 -> 0)
  - Automated Supply Service (2000 -> 1000)

Integration
- Added Paper Box recipe using Cardboard from Create
- Packages from Create can be delivered now
  - Address Tag is still required, regardless of the address on the package. Address Tag can be applied automatically with crafters
- When moving mail recipe ingredients into a Package with JEI - address of the recipe will be remembered and applied when items are packed, consuming an Address Tag
- Fixed mailing recipe category not displaying correctly with _EMI_ and _TooManyRecipeViewers_.

Misc
- Fixed a crash occurring when a service address returned unprocessed items in a craft (sending back remaining items)

### Technical Changes
- Pigeon Variants are now data-driven
  - You can create and spawn in world a custom variant using datapack + resourcepack
- Added Pigeon to `minecraft:fall_damage_immune` entity tag
- Renamed tag `has_passenger_pigeons` to `spawns_passenger_pigeons`
- Moved pigeon hat and backpack textures into `misc` subfolder
- Added 'has_mail' property to Mailbox block
  - Will be set to true if inbox is not empty
  - Unused in the mod, but can be useful for resourcepacks
- Seal data:
  - Renamed `envelope:seal_stamp_impression` to `envelope:seal_stamp_die`
    - Old component is still available, to not break existing stamps and it will be replaced with a new one on use
  - Renamed `envelope:seal_impression` registry to `envelope:seal_symbol`
  - Renamed `assets/envelope/textures/seal/impression` folder to `assets/envelope/textures/seal/symbol`  
    - Existing seals will remain intact
- Renamed **Equine Assurance Bureau** internal name from `equine_insurance_bureau` to `equine_assurance_bureau`
- Restructured lost mail loot tables
  - Moved `envelope:lost_mail` to `envelope:packages/lost_mail`
  - Moved sub-tables into `envelope:packages/lost_mail/` folder and removed `lost_mail_` prefix from them
  - Open your existing Lost Mail packages before updating to this version
- Added config option for fox letter tattering

## 0.6.2 - 2026-06-11
- Potentially fixed an issue with C2ME.
- Fixed crash with Flashback mod when recorded replay is viewed.
- [Fabric] Fixed crash at startup.

## 0.6.1 - 2026-06-07
- Added Sable compatibility (thanks Flooweur).
- Updated localization files.

_Pigeon wandering is still not fixed in this release. Will be fixed in 0.7._

## 0.6 - 2026-04-16
#### Mail Recipes
Added a new way to craft items by sending a Package with ingredients to the service address.
- Works similarly to the shapeless crafting.
- Results are returned using the same courier, or with a service courier, if some of the ingredients were unprocessed.
- Data-driven. 
- Can give experience. Applied to the resulting package.
- Shown in **JEI**
  - Clicking on an arrow in the Mailbox menu, or on a box in the Paper Box menu will show available recipes.

Included recipes:
- Payback Tag 
  - no longer craftable in the Crafting Table
- Seal Stamps with custom impressions
- Rotten Flesh -> Leather
- Saddle
- Name Tag
- Tuff, Dripstone, Calcite
- Glow Ink Sac
- Book and Quill and Letter and Quill 
- Lost Mail
  - Has random loot inside
- Letter Broadcasting
  - Sends copies of the letter to all existing player-default mailboxes.

Package
- Added `envelope:package_experience` item component. Awarded when package is opened.
- Using the package while holding [**Sneak**] will unpack it without opening the menu.
- Fixed overlays not rendering over package slots.
- Fixed being able to insert items into opened package, if items were the same.

Payback Tag
- Added drag and drop from JEI support.
- Added request duration selector.
  - Controls how much time buyer will have to pay for the mail.
  - Item texture changes slightly based on duration defined.
- Using the tag while holding [**Sneak**] will clear the data from it.
- Menu UI/UX improvements.
- Changed `envelope:payback_tag_contents` item component: its structure is now the same as `envelope:mail_payback_request`.
- `delivery.payback_timeout_minutes` config option was replaced by three `payback.request_duration_<duration>` options.

Address
- Player address type definition has been changed. Field `id` is now `name`: `{ type:"player", name:"mortuusars" }`
- Service addresses are now data-driven
  - Address component now references registered definition by its key: `{ type:"service", definition:"envelope:mail_service" }`
- Added "custom" address type: `{ type:"custom", name:{translate:"display.name"} }`
  - Can be used for display purposes (commands, scripts, etc.). Cannot receive mail.
- Unknown address is now a separate type: `{ type:"unknown" }`

Service Addresses
- Definition is data-driven.
- For use in recipes or custom handlers.
- `#envelope:hidden` service address tag can be used to hide the address from suggestions and its recipes from JEI.
- Addresses that have recipes will show up in JEI as an ingredient.
  - Querying their uses (R-Click or [U]) will show available recipes associated with that address.
- Added **Equine Assurance Bureau** sending "spam" letters.

Delivery
- Added support for Service address "drop-off" handlers.
  - They can be defined in code (for addons) or through KubeJS.
  - _Drop-off handler is responsible for actually deciding what happens with delivered mail (consume/return/reply)._

KubeJS
- Added `EnvelopeEvents.registerServiceDropOffHandlers` event.  
- Added `EnvelopeEvents.handleMailDropOff` event.
  - Called before most of the regular logic is processed.
  - Can be used to modify drop-off of any address type.
- No documentation yet.

Misc:
- Slightly improved Pigeon pathfinding around blocks, should get stuck a bit less now .
- Added **Address Tag** and **Payback Tag** application recipes.
- Applying **Address Tag** and **Payback Tag** in GUI now consumes 1 tag per mail, instead of 1 per stack.
- **Payback Box** and **Payback Package** now show full time remaining when [**Shift**] is held.
- Updated some address type icons (little flags)
- Moved seal textures from `textures/gui/seal` to `textures/seal` folder.
- Moved letters and numbers seal impressions into their respective subfolders. Example: `envelope:c` -> `envelope:letter/c`
- Fixed total delivery distance calculation in debug-mode overlay.
- Rewrote some parts of Delivery Log, mostly internal work, but there are some minor user-facing changes as well.
- Small updates of **Address Tag**, **Payback Box** and **Payback Package** menu textures.
- Small change to how `envelope:payback_subject` component is defined.
- Renamed `mail_service_payback_department.dat` to `envelope_mail_service_payback_department.dat` in _level/data_.
- Fixed couriers spawned after delivery is finished (when ended in unloaded chunk) not being tired.
- Fixed leashed pigeons being able to enter a Pigeonhole.
- Fixed error in Pigeon saved NBT data.

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