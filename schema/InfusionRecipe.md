# Description
An Infusion Recipe converts an item into another item when it is enchanted in the Enchanting Table, provided the current enchanting stats fall within the recipe's requirements.  

Infusion recipes are normal recipes, loaded from the `data/<namespace>/recipe/` datapack folder. Two recipe types exist:
* `apothic_enchanting:infusion` - The standard infusion recipe.
* `apothic_enchanting:keep_nbt_infusion` - Identical, but the components of the input item are copied to the output item.

# Dependencies
This object references the following objects:
1. [Stats](./Stats.md)
2. [ItemStackTemplate](../../../../Placebo/blob/26.1/schema/ItemStackTemplate.md)

# Schema
```js
{
    "type": "apothic_enchanting:infusion",
    "input": Ingredient,          // [Mandatory] || A vanilla ingredient matching the item being infused. Must not be empty.
    "result": ItemStackTemplate,  // [Mandatory] || The item produced by the infusion.
    "requirements": Stats,        // [Mandatory] || The minimum eterna, quanta, and arcana required to perform this infusion.
    "max_requirements": Stats     // [Optional]  || The maximum allowed eterna, quanta, and arcana. A value of -1 means no limit. Default value = all -1.
}
```

Note: For each of eterna, quanta, and arcana, if the maximum is not -1, the corresponding requirement must be less than or equal to it, or the recipe will fail to load.

# Examples
The Echo Shard duplication recipe, which requires 70 eterna, 50 quanta, and 50 arcana.

```json
{
    "type": "apothic_enchanting:infusion",
    "input": {
        "item": "minecraft:echo_shard"
    },
    "requirements": {
        "eterna": 70,
        "quanta": 50,
        "arcana": 50
    },
    "result": {
        "id": "minecraft:echo_shard",
        "count": 4
    }
}
```

The Ender Library recipe, which preserves the stored enchantments of the input library, and requires exactly 100 eterna and 100 arcana.

```json
{
    "type": "apothic_enchanting:keep_nbt_infusion",
    "input": {
        "item": "apothic_enchanting:library"
    },
    "requirements": {
        "eterna": 100,
        "quanta": 45,
        "arcana": 100
    },
    "max_requirements": {
        "eterna": 100,
        "quanta": 50,
        "arcana": 100
    },
    "result": {
        "id": "apothic_enchanting:ender_library",
        "count": 1
    }
}
```
