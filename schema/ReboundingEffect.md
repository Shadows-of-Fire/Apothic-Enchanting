# Description
The Rebounding Effect (`apothic_enchanting:rebounding`) is an enchantment entity effect which launches the affected entity away from the effect origin.  

It may be used anywhere the vanilla enchantment system accepts an entity effect, such as `minecraft:post_attack`.

# Schema
```js
{
    "type": "apothic_enchanting:rebounding",
    "range": LevelBasedValue,                // [Mandatory] || The maximum squared distance between the affected entity and the effect origin. Note that this is a squared distance, so a value of 4 covers 2 blocks.
    "horizontal_strength": LevelBasedValue,  // [Mandatory] || The multiplier applied to the horizontal components of the launch vector.
    "vertical_strength": LevelBasedValue     // [Mandatory] || The multiplier applied to the vertical component of the launch vector.
}
```

# Examples
The effects of the Rebounding enchantment, launching melee attackers away from the wearer.

```json
{
    "effects": {
        "minecraft:post_attack": [
            {
                "affected": "attacker",
                "effect": {
                    "type": "apothic_enchanting:rebounding",
                    "horizontal_strength": {
                        "type": "minecraft:linear",
                        "base": 2.0,
                        "per_level_above_first": 2.0
                    },
                    "range": 4.0,
                    "vertical_strength": {
                        "type": "minecraft:linear",
                        "base": 3.0,
                        "per_level_above_first": 3.0
                    }
                },
                "enchanted": "victim"
            }
        ]
    }
}
```
