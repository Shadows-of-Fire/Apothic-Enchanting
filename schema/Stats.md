# Description
Stats are a collection of enchanting table stats: the eterna cap, eterna, quanta, arcana, and clues.  

They are used by [Block Stats](./BlockStats.md) to declare what a block provides, and by [Infusion Recipes](./InfusionRecipe.md) to declare requirements.

# Schema
```js
{
    "maxEterna": float, // [Optional] || The highest eterna value this block may raise the table to. Default value = 30.
    "eterna": float,    // [Optional] || The eterna provided. Default value = 0.
    "quanta": float,    // [Optional] || The quanta provided. May be negative. Default value = 0.
    "arcana": float,    // [Optional] || The arcana provided. Default value = 0.
    "clues": integer    // [Optional] || The number of additional enchantment clues provided. Default value = 0.
}
```

Note: The `maxEterna` key is camelCase, unlike most keys in this mod.  

When used in the `max_requirements` of an infusion recipe, a value of `-1` means "no limit". The `maxEterna` and `clues` values are accepted but have no meaning in recipe contexts.

# Examples
Stats for a basic bookshelf, providing 2 eterna and leaving everything else at the defaults.

```json
{
    "eterna": 2
}
```
