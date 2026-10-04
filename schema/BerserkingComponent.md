# Description
The Berserking Component (`apothic_enchanting:berserking`) is an [enchantment effect component](./EnchantmentEffects.md) which sends the user into a fury when they take damage.  

When triggered while off cooldown, the user pays the health cost and receives all of the listed mob effects.

# Schema
```js
"apothic_enchanting:berserking": {
    "hp_cost": [                               // [Mandatory] || Value effects computing the health cost paid when triggered.
        {
            "effect": EnchantmentValueEffect,  // [Mandatory] || The value effect.
            "requirements": LootCondition      // [Optional]  || A condition gating this entry.
        }
    ],
    "mob_effects": [                           // [Mandatory] || The effects granted when triggered.
        {
            "effect": {
                "mob_effect": "string",        // [Mandatory] || Registry name of the effect to apply.
                "duration": [                  // [Mandatory] || Value effects computing the duration, in ticks.
                    EnchantmentValueEffect
                ],
                "amplifier": [                 // [Mandatory] || Value effects computing the amplifier. Zero-indexed, meaning 0 is level I.
                    EnchantmentValueEffect
                ],
                "ambient": boolean,            // [Optional]  || If the effect is marked as ambient. Default value = false.
                "visible": boolean,            // [Optional]  || If the effect particles are visible. Default value = true.
                "show_icon": boolean           // [Optional]  || If the effect icon is shown. Defaults to the value of "visible".
            },
            "requirements": LootCondition      // [Optional]  || A condition gating this entry.
        }
    ],
    "cooldown": [                              // [Mandatory] || Value effects computing the cooldown between triggers, in ticks.
        {
            "effect": EnchantmentValueEffect,  // [Mandatory] || The value effect.
            "requirements": LootCondition      // [Optional]  || A condition gating this entry.
        }
    ]
}
```

# Examples
A trimmed version of Berserker's Fury, which costs 2.5^level health, grants 25 seconds of Resistance (scaling with level), and has a 45 second cooldown.

```json
{
    "apothic_enchanting:berserking": {
        "cooldown": [
            {
                "effect": {
                    "type": "minecraft:add",
                    "value": 900.0
                }
            }
        ],
        "hp_cost": [
            {
                "effect": {
                    "type": "minecraft:add",
                    "value": {
                        "type": "apothic_enchanting:exponential",
                        "base": 2.5
                    }
                }
            }
        ],
        "mob_effects": [
            {
                "effect": {
                    "amplifier": [
                        {
                            "type": "minecraft:add",
                            "value": {
                                "type": "minecraft:linear",
                                "base": 0.0,
                                "per_level_above_first": 1.0
                            }
                        }
                    ],
                    "duration": [
                        {
                            "type": "minecraft:add",
                            "value": 500.0
                        }
                    ],
                    "mob_effect": "minecraft:resistance"
                }
            }
        ]
    }
}
```
