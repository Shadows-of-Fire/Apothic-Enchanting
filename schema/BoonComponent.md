# Description
The Boon Component (`apothic_enchanting:earths_boon`) is an [enchantment effect component](./EnchantmentEffects.md) which grants a chance at bonus loot when mining certain blocks.  

When a block matching an entry's target is broken, the entry's drop chance is rolled, and on success the loot table is rolled and its contents are dropped.

# Schema
```js
"apothic_enchanting:earths_boon": {
    "entries": [                                   // [Mandatory] || The list of boon entries.
        {
            "target": HolderSet,                   // [Mandatory] || The blocks that trigger this entry. Accepts a list of block registry names, or a single #-prefixed tag name.
            "loot_table": "string",                // [Mandatory] || Registry name of the loot table rolled when this entry triggers.
            "drop_chance": [                       // [Mandatory] || Value effects computing the chance to roll the loot table.
                {
                    "effect": EnchantmentValueEffect,  // [Mandatory] || The value effect.
                    "requirements": LootCondition      // [Optional]  || A condition gating this entry.
                }
            ]
        }
    ]
}
```

# Examples
A trimmed version of Boon of the Earth's effect, granting a 1.5% chance per level to roll the boon loot table when mining deepslate.

```json
{
    "apothic_enchanting:earths_boon": {
        "entries": [
            {
                "drop_chance": [
                    {
                        "effect": {
                            "type": "minecraft:add",
                            "value": {
                                "type": "minecraft:linear",
                                "base": 0.015,
                                "per_level_above_first": 0.015
                            }
                        }
                    }
                ],
                "loot_table": "apothic_enchanting:boon_deepslate_drops",
                "target": "#minecraft:deepslate_ore_replaceables"
            }
        ]
    }
}
```
