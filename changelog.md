## 1.6.2
* Fixed an issue that caused star-prefixed enchantments to display the level twice.
* Satherov: Added a JEI transfer handler for infusion recipes in the Enchanting Table of the Raven.

## 1.6.1
* Updated how Enchantability works. It no longer gives Arcana, and instead gives a % chance to boost an enchantment's level by 1 when enchanting.
  * This can allow enchantments to go over the apoth configured max level.
  * Enchantments that are over the configured max level are now marked with a star.
* The enchanting table will now directly call out World Tiers as the source of max eterna limits if Apotheosis is installed.
* Fixed a bug with Miner's Fervor and instabreak blocks like Sugarcane being unbreakable.
* Removed a _very_ stale shears dispenser behavior override.
* Quarkrus: Updated Russian translation.
* t0piy & PrincessStellar: Updated Brazilian translation.

## 1.6.0
* Backported the following changes from 2.0.0 (Minecraft 26.1.2)
  * Added the Apothic Enchanting Table, a cosmetic variant of the Enchanting Table.
  * Added the Enchanting Table of the Raven, an end-game table that allows manually setting eterna, quanta, and arcana.
    * The crafting recipe is provided by Apotheosis.
  * Added the Max Eterna attribute (`apothic_enchanting:max_eterna`), which limits the eterna a player may use at any table.
  * Added the Basic Bookshelf, and gave every shelf randomized model variants.
  * All shelf textures have been updated, and shelves are now mined with an axe (no tool required to drop).
  * Infusion enchanting now clamps the displayed level cost to the recipe's eterna requirement.

## 1.5.3
* PrincessStellar: Updated Brazilian translation.
* PODOB: Updated Korean translation.
* Implemented Clearable on `EnchantingTableBlockEntity`.

## 1.5.2
* Fixed an issue that prevented connecting to Vanilla servers.
* Updated the Enchantment Library's GUI texture.
* Mgazul: Fixed an incorrect signature in `ItemMixin#apoth_ignoreDamageForEnchantable`.

## 1.5.1
* Fixed enchantments with `"show_in_tooltip": false` still showing in tooltips.
* Fixed enchantment hard caps being ignored.
* mc-kaishixiaxue: Updated Chinese translation.

## 1.5.0
* Added three new Music Discs (Eterna, Quanta, and Arcana) by Firel.
* PrincessStellar: Updated Brazilian Translation
* MaybeMedic: Updated Russian Translation

## 1.4.4
* Updated to Placebo 9.9.0

## 1.4.3
* Fixed the Occult Ender Lead not respecting `apothic_spawners:blacklisted_from_spawners`.
* Fixed `ApothEnchantmentScreen#clickMenuButton` crashing when given an invalid index.
* Made the "View all available enchantments" button play the button click sound.
* Made Boon of the Earth respect Fortune and Silk Touch, adjusting its effects appropriately.

## 1.4.2
* ZzThanhBaozZ: Added Vietnamese translation.
* Added a new config option to add inline enchantment description tooltips.
  * Apoth's custom handling of item tooltips breaks the stock implementation used by Enchantment Descriptions.
  * If you want to re-enable inline tooltips, flip the config option. You'll still need to install enchdesc for the translations.

## 1.4.1
* Fixed a crash when an ender lead holds an entity with a custom mob category.
* Fixed an issue where the filtering shelf could attempt to send packets on the client.
* mc-kaishixiaxue: Updated Chinese translation.

## 1.4.0
### Features
* Boon of the Earth now uses Loot Tables! This means Boon is actually a viable enchantment, instead of a spam-producing mess.
  * Boon was previously relying on an implementation from ~1.16 where it dropped ore blocks. Since Raw Ores were introduced, this approach became stale.
  * Now, Boon of the Earth works by specifying a set of input blocks and a corresponding loot table for what can drop from those blocks.
  * By default, Boon has different loot tables for stones, deepslate, and nether blocks.
* Added the Ender Lead! This is an item from Apotheosis's Garden Module in 1.20 that didn't have a home before, but will live here for now.
  * The ender lead has three variants, the Flimsy Ender Lead, the Ender Lead, and the Occult Ender Lead.
  * The Occult Ender Lead has the ability to change the target mob in Mob Spawners (similar to a spawn egg).
  * It will respect the blacklist set by the entity type tag `apothic_spawners:blacklisted_from_spawners`.
* Added the item tag `apothic_enchanting:cannot_be_converted_to_xp`.
  * This tag allows exempting items from being converted to experience via Knowledge of the Ages.

### Bugfixes
* Fixed a hang when placing certain invalid items in the anvil.
* Fixed Icy Thorns having a duration 20x higher than intended.
* Fixed an issue where colors in the Enchantment Library were incorrect.
* Fixed the Tome of the Others not working.
* Fixed Seashelves of Aquatic Filtration not working with modded automation.
* Fixed Chainsaw not working with Occultism trees.
* Fixed an issue that was causing Lure to be instant at all levels.

### Misc
* Zakoz777: Added Japanese Translation.
* ZHAY10086: Updated Chinese Translation.
* Removed remaining mentions of rectification and updated them to stability.
* Added Enchantment Level Cap Indicator to coremod targets so it reads the Apoth max levels.

## 1.3.2
* Fixed log spam when loading an unset level cap (value of -1).

## 1.3.1
* Added the "Forced Level Cap" config option to each element in the enchantments.cfg file.
  * This option allows modpack developers to cap the gameplay level, regardless of how high players may be able to get the enchantment otherwise.
* RuyaSavascisi: Added Turkish translation.

## 1.3.0
* Fixed the Tome of Extraction using gameplay enchantments instead of NBT enchantments.
* Fixed Library tooltips going offscreen when advanced tooltips (f3+h) are enabled.
* Fixed higher level books not showing any uses in JEI.
* Fixed all the advancements.
* A stock datapack artifact will now be published to CurseForge with each release of the mod.

## 1.2.5
* Crescendo of Bolts now works with Ars' Spell Crossbow.
* MelnCat: The Seashelf of Aquatic Filtration can now accept most books for decorative purposes.
  * Only Enchanted Books with a single enchantment will provide functional bonuses.

## 1.2.4
* Fixed Icy Thorns applying for much longer than intended.

## 1.2.3
* Added back the enchanted book tooltips present in Apotheosis, these got lost in the split-off somehow.
  * This includes adding a description even if Enchantment Descriptions isn't installed, and showing various metadata information.

## 1.2.2
* Fixed Boon of the Earth being unlocalized.
* Translated Infusion JEI tooltips up by 100 on the Z axis (should fix some overlap issues).

## 1.2.1
* Updated to Placebo 9.5.1.

## 1.2.0
* Updated to Minecraft 1.21.1.
* Made Tridents and Shears able to accept all enchantments that were possible in 1.20.
  * This was not possible earlier as it relied on Neo's `Item#supportsEnchantment` hook.
* Fixed sheep-specific shear enchantments crashing when used.
* Tightened tooltip level access when resolving enchanting stat tooltips. Falls back to the default block state when unavailable.

## 1.1.2
* Fixed crossbows crashing on fire.
* Fixed the warden loot modifier failing when the TOOL parameter was not provided.
* Fixed enchantment redirect coremods triggering a sided crash on dedicated servers.
* Renamed `apothic_enchanting:earths_boon` to `apothic_enchanting:boon_of_the_earth`.
* Set Endless Quiver's max level to 1 (should always have been 1).
* Fixed all shear enchantments (pending merge of a recent Neo PR).

## 1.1.1
* Fixed various invalid tag paths and broken recipes. This should fix a wide variety of behaviors that were caused by tags not being loaded.
* Fixed Enchantment Libraries not displaying the number of stored enchantments in item form.
* Fixed Quantic Stability causing all enchantments to be rolled at max enchanting power (200).
* Fixed enchantment name colors to match the original values for corrupted / twisted / masterwork enchantments.

## 1.1.0
* Alpha update to Minecraft 1.21. Various things may be incomplete or missing!

## 1.0.0
* Initial Release