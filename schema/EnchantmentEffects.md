# Description
Apothic Enchanting adds a number of enchantment effect components, which are used as keys in the `effects` map of a vanilla enchantment definition (`data/<namespace>/enchantment/`).  

Like all enchantment effect components, they are only active while the enchantment's `slots` requirement is met. The value shapes below reuse the vanilla enchantment constructs (LevelBasedValue, ConditionalEffect, and EnchantmentValueEffect).

# Schema
The components are grouped below by the shape of their values.

## Marker Components
The following components have no parameters, and are enabled by providing an empty object:

1. `apothic_enchanting:chainsaw` - Breaking a log fells the entire tree.
2. `apothic_enchanting:chromatic` - Sheared wool is changed to a random color.
3. `apothic_enchanting:exploitation` - Sheared wool is doubled, but the sheep takes two damage.
4. `apothic_enchanting:stable_footing` - Negates the mining speed penalty for flying.
5. `apothic_enchanting:tempting` - Nearby farm animals follow the holder, as if the item were their food.

```js
"apothic_enchanting:chainsaw": {}
```

## Level-Based Value Components
The following components hold a single vanilla LevelBasedValue:

1. `apothic_enchanting:bonemeal_crops` - Right-clicking a crop bonemeals it. The value is the durability cost per use.
2. `apothic_enchanting:miners_fervor` - Scales mining speed like a stronger Efficiency, but can never reach instant-break. The value is the added break speed; the cap is fixed in code.

```js
"apothic_enchanting:bonemeal_crops": LevelBasedValue
```

## Value Effect List Components
The following components hold a list of vanilla ConditionalEffects, each wrapping an EnchantmentValueEffect that computes the final value:

1. `apothic_enchanting:crescendo` - The value is the number of additional crossbow shots available per consumed ammunition, without reloading.
2. `apothic_enchanting:drops_to_xp` - Items dropped by slain mobs are converted into experience. The value is the amount of experience granted per item.
3. `apothic_enchanting:extra_loot_roll` - The value is the chance that a slain mob's loot table is rolled and dropped an additional time.
4. `apothic_enchanting:repair_with_hp` - Incoming healing is consumed to repair the item. The value is the durability restored per point of healing. When the value is above one, fractional healing may be consumed to restore whole durability points.

```js
"apothic_enchanting:crescendo": [
    {
        "effect": EnchantmentValueEffect,  // [Mandatory] || The value effect computing the final value.
        "requirements": LootCondition      // [Optional]  || A condition gating this entry.
    }
]
```

## Float Components
1. `apothic_enchanting:growth_serum` - The value is the chance that a sheared sheep immediately regrows its wool. Range: [0.001, 1].

```js
"apothic_enchanting:growth_serum": float
```

## Complex Components
The following components have their own schema files:

1. [BerserkingComponent](./BerserkingComponent.md) (`apothic_enchanting:berserking`) - Grants effects at a health cost when the user takes damage.
2. [BoonComponent](./BoonComponent.md) (`apothic_enchanting:earths_boon`) - Grants a chance at bonus loot when mining certain blocks.
3. [ReflectiveComponent](./ReflectiveComponent.md) (`apothic_enchanting:reflective`) - Grants a chance to reflect blocked damage back at the attacker.

Apothic Enchanting also adds an entity effect and a level-based value type usable anywhere the vanilla enchantment system accepts them:

1. [ReboundingEffect](./ReboundingEffect.md) (`apothic_enchanting:rebounding`) - Launches an entity away from the effect origin.
2. [ExponentialValue](./ExponentialValue.md) (`apothic_enchanting:exponential`) - A level-based value computing base^exponent.

# Examples
The Chromatic Aberration enchantment, showing a marker component in the context of a full enchantment definition.

```json
{
    "anvil_cost": 2,
    "description": {
        "translate": "enchantment.apothic_enchanting.chromatic"
    },
    "effects": {
        "apothic_enchanting:chromatic": {}
    },
    "max_cost": {
        "base": 200,
        "per_level_above_first": 0
    },
    "max_level": 1,
    "min_cost": {
        "base": 25,
        "per_level_above_first": 0
    },
    "slots": [
        "hand"
    ],
    "supported_items": "#c:tools/shear",
    "weight": 5
}
```

The effects of Knowledge of the Ages, granting 25 experience per converted item at level one, and 25 more per level.

```json
{
    "apothic_enchanting:drops_to_xp": [
        {
            "effect": {
                "type": "minecraft:add",
                "value": {
                    "type": "minecraft:linear",
                    "base": 25.0,
                    "per_level_above_first": 25.0
                }
            }
        }
    ]
}
```
