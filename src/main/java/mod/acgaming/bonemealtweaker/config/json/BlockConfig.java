package mod.acgaming.bonemealtweaker.config.json;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Random;

public class BlockConfig
{
    private final ResourceLocation replaceBlock;
    private final int iterations;
    private final List<String> biomes;
    private final List<Integer> dimensions;
    private final List<SpawnBlock> spawnBlocks;
    private final int totalWeight;

    public BlockConfig(ResourceLocation replaceBlock, int iterations, List<String> biomes, List<Integer> dimensions, List<SpawnBlock> spawnBlocks)
    {
        this.replaceBlock = replaceBlock;
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

    public ResourceLocation getReplaceBlock()
    {
        return replaceBlock;
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
