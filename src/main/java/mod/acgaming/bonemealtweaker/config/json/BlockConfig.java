package mod.acgaming.bonemealtweaker.config.json;

import net.minecraft.block.state.IBlockState;

import java.util.List;
import java.util.Random;

public class BlockConfig
{
    private final int iterations;
    private final List<String> biomes;
    private final List<Integer> dimensions;
    private final List<SpawnBlock> spawnBlocks;
    private final int totalWeight;

    public BlockConfig(int iterations, List<String> biomes, List<Integer> dimensions, List<SpawnBlock> spawnBlocks)
    {
        this.iterations = iterations;
        this.biomes = biomes;
        this.dimensions = dimensions;
        this.spawnBlocks = spawnBlocks;
        this.totalWeight = spawnBlocks.stream().mapToInt(SpawnBlock::getWeight).sum();
    }

    public IBlockState getRandomBlockState(Random rand)
    {
        int randWeight = rand.nextInt(totalWeight);
        int currentWeight = 0;
        for (SpawnBlock spawnBlock : spawnBlocks)
        {
            currentWeight += spawnBlock.getWeight();
            if (randWeight < currentWeight)
            {
                return spawnBlock.getBlockState();
            }
        }
        return null;
    }

    public int getIterations()
    {
        return iterations;
    }

    public List<String> getBiomes()
    {
        return biomes;
    }

    public List<Integer> getDimensions()
    {
        return dimensions;
    }

    public List<SpawnBlock> getSpawnBlocks()
    {
        return spawnBlocks;
    }
}
