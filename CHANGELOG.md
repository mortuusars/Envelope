# Changelog

## 0.2.0.2
- Improvements to Bugger (Debug utility enabled with `debug.debug` server config option. Adds more info to F3 screen, more log messages, etc).

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