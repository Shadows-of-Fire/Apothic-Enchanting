# Description
The Exponential Value (`apothic_enchanting:exponential`) is a level-based value which computes `base ^ exponent`.  

It may be used anywhere the vanilla enchantment system accepts a LevelBasedValue.

# Schema
```js
{
    "type": "apothic_enchanting:exponential",
    "base": float,               // [Mandatory] || The base of the exponential.
    "exponent": LevelBasedValue  // [Optional]  || The exponent of the exponential. Defaults to the enchantment level.
}
```

# Examples
An exponential value computing 2.5^level.

```json
{
    "type": "apothic_enchanting:exponential",
    "base": 2.5
}
```

An exponential value computing 2^(level - 1), using an explicit exponent.

```json
{
    "type": "apothic_enchanting:exponential",
    "base": 2.0,
    "exponent": {
        "type": "minecraft:linear",
        "base": 0.0,
        "per_level_above_first": 1.0
    }
}
```
