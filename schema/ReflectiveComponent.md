# Description
The Reflective Component (`apothic_enchanting:reflective`) is an [enchantment effect component](./EnchantmentEffects.md) which grants a chance to reflect blocked damage back at the attacker.  

When the user blocks an attack with a shield carrying this component, the proc chance is rolled, and on success the attacker takes the given fraction of the blocked damage.

# Schema
```js
"apothic_enchanting:reflective": {
    "proc_chance": LevelBasedValue,   // [Mandatory] || The chance to reflect a blocked attack.
    "reflect_ratio": LevelBasedValue  // [Mandatory] || The fraction of the blocked damage dealt back to the attacker.
}
```

# Examples
The effect of Reflective Defenses, with a 15% + 10%/level proc chance reflecting 15% + 15%/level of the damage.

```json
{
    "apothic_enchanting:reflective": {
        "proc_chance": {
            "type": "minecraft:linear",
            "base": 0.15,
            "per_level_above_first": 0.1
        },
        "reflect_ratio": {
            "type": "minecraft:linear",
            "base": 0.15,
            "per_level_above_first": 0.15
        }
    }
}
```
