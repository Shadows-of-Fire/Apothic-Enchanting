# Description
Block Stats specify the enchanting stats that a block provides to the enchanting table, replacing the code-based stat lookup for the selected blocks.  

Block Stats are loaded from the `data/<namespace>/enchanting_stats/` datapack folder.  

A single file selects its blocks with a HolderSet: a single block registry name, a list of block registry names, or a `#`-prefixed block tag name.

# Dependencies
This object references the following objects:
1. [Stats](./Stats.md)

# Schema
```js
{
    "blocks": HolderSet, // [Mandatory] || The blocks receiving the stats. Accepts a single block registry name, a list of block registry names, or a #-prefixed block tag name.
    "stats": Stats       // [Mandatory] || The enchanting stats provided by the selected blocks.
}
```

# Examples
The Blazing Hellshelf, which raises the eterna cap to 65, provides 10 eterna and 10 quanta, and removes a clue.

```json
{
    "blocks": "apothic_enchanting:blazing_hellshelf",
    "stats": {
        "clues": -1,
        "eterna": 10.0,
        "maxEterna": 65.0,
        "quanta": 10.0
    }
}
```

Basic skulls, which provide 5 quanta each, selected via a block list.

```json
{
    "blocks": [
        "minecraft:skeleton_skull",
        "minecraft:skeleton_wall_skull",
        "minecraft:creeper_head",
        "minecraft:creeper_wall_head",
        "minecraft:zombie_head",
        "minecraft:zombie_wall_head",
        "minecraft:piglin_head",
        "minecraft:piglin_wall_head"
    ],
    "stats": {
        "maxEterna": 0.0,
        "quanta": 5.0
    }
}
```

A tag-based selection, applying the stats to every block in the `minecraft:wool` tag.

```json
{
    "blocks": "#minecraft:wool",
    "stats": {
        "quanta": 1.0
    }
}
```
