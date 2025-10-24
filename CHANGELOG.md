# Changelog

## 0.2.0.1
- Added restrictions for items that can be placed inside a Package:
  - Added Bundle to `envelope:cannot_be_packaged` tag
  - Package now also checks `item.canFitInsideContainerItems()`. This will restrict items like Shulker Box, Supplementaries Sack, etc.
- Many small improvements.
- Fixed crash when delivering items with enchantments.