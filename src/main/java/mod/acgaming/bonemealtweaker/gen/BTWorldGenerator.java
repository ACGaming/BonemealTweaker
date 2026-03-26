package mod.acgaming.bonemealtweaker.gen;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import mod.acgaming.bonemealtweaker.BonemealTweaker;
import mod.acgaming.bonemealtweaker.config.json.BlockConfig;

public class BTWorldGenerator implements IWorldGenerator
{
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider)
    {
        if (world.getWorldInfo().getTerrainType() == WorldType.FLAT) return;
        int chunkXPos = chunkX * 16;
        int chunkZPos = chunkZ * 16;
        String biome = world.getBiome(new BlockPos(chunkXPos + 8, 0, chunkZPos + 8)).getRegistryName().toString();
        int dimension = world.provider.getDimension();
        for (Map.Entry<ResourceLocation, List<BlockConfig>> entry : BonemealTweaker.BLOCK_CONFIGS.entrySet())
        {
            ResourceLocation blockRL = entry.getKey();
            List<BlockConfig> configs = entry.getValue();
            for (BlockConfig config : configs)
            {
                if (!config.getApplyMode().isSurface()) continue;
                if (!config.getBiomes().isEmpty() && !config.getBiomes().contains(biome)) continue;
                if (!config.getDimensions().isEmpty() && !config.getDimensions().contains(dimension)) continue;
                for (int i = 0; i < config.getGenDensity(); i++)
                {
                    int x = chunkXPos + random.nextInt(16) + 8;
                    int z = chunkZPos + random.nextInt(16) + 8;
                    BlockPos topPos = world.getHeight(new BlockPos(x, 0, z));
                    BlockPos belowPos = topPos.down();
                    IBlockState belowState = world.getBlockState(belowPos);
                    if (belowState.getBlock().getRegistryName().equals(blockRL))
                    {
                        IBlockState targetState = world.getBlockState(topPos);
                        if (config.getReplaceBlock() == null ? world.isAirBlock(topPos) : targetState.getBlock().getRegistryName().equals(config.getReplaceBlock()))
                        {
                            BTPlacementLogic.applyBlockPlacement(world, belowPos, belowState, null, ItemStack.EMPTY, false);
                        }
                    }
                }
            }
        }
    }
}
