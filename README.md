## Bonemeal Tweaker

###### Not bad to the bone!

A simple mod that modifies the way plants are spawned when bonemeal is applied on blocks.

Configuration is handled by JSON-based config files per block in the `config/bonemealtweaker` directory. Example for the vanilla grass block (`vanilla_grass.json`):

```json
{
  "block": "minecraft:grass",
  "replaceBlock": "minecraft:air",
  "iterations": 128,
  "applyMode": "BONEMEAL",
  "biomes": [
    "minecraft:plains",
    "minecraft:forest"
  ],
  "dimensions": [
    0
  ],
  "spawnBlocks": [
    {
      "block": "minecraft:tallgrass[type=tall_grass]",
      "weight": 60
    },
    {
      "block": "flowerEntry",
      "weight": 20
    }
  ]
}
```

- `block`: The **IGrowable** block to apply custom bonemeal logic on
- `replaceBlock`: The block to be replaced with foliage (above `block`), can be omitted to replace air
- `iterations`: The density of blocks/plants to spawn
- `applyMode`: The logic to apply, can be `BONEMEAL` (on bonemealing, default), `SURFACE` (natural surface world generation) or `BOTH` (bonemealing + world generation)
- `biomes`: An optional list of whitelisted biomes, can be left empty to allow any
- `dimensions`: An optional list of whitelisted dimensions, can be left empty to allow any
- `spawnBlocks`: An array of blocks/plants by resource location to spawn on the specified `block`, **flowerEntry** picks a random flower
- `weight`: The relative chance to spawn across all `spawnBlocks` entries

---

This mod was commissioned for Minecraft 1.12.2.