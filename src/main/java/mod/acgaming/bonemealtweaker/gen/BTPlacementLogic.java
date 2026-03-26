package mod.acgaming.bonemealtweaker.gen;

import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import mod.acgaming.bonemealtweaker.BonemealTweaker;
import mod.acgaming.bonemealtweaker.config.json.BlockConfig;
import mod.acgaming.bonemealtweaker.config.json.SpawnBlock;

public class BTPlacementLogic
{
    private static final String FLOWER_ENTRY = "flowerEntry";

    public static boolean applyBlockPlacement(World world, BlockPos pos, IBlockState state, EntityPlayer placer, ItemStack stack, boolean isBonemeal)
    {
        if (world.isRemote) return false;
        ResourceLocation blockRL = state.getBlock().getRegistryName();
        List<BlockConfig> configs = BonemealTweaker.BLOCK_CONFIGS.get(blockRL);
        if (configs == null)
        {
            BonemealTweaker.LOGGER.debug("No configs found for block: {}", blockRL);
            return false;
        }
        Biome biome = world.getBiome(pos);
        String biomeName = biome.getRegistryName().toString();
        int dimension = world.provider.getDimension();
        IBlockState targetState = world.getBlockState(pos.up());
        boolean matched = false;
        for (BlockConfig config : configs)
        {
            if (!checkPreconditions(world, dimension, biomeName, pos, targetState.getBlock().getRegistryName(), config, isBonemeal))
            {
                continue;
            }
            Random rand = world.rand;
            BlockPos blockPos = pos.up();
            for (int i = 0; i < config.getIterations(); ++i)
            {
                BlockPos blockPos1 = blockPos;
                int maxSteps = i / 16;
                for (int j = 0; j < maxSteps; ++j)
                {
                    blockPos1 = blockPos1.add(rand.nextInt(3) - 1, (rand.nextInt(3) - 1) * rand.nextInt(3) / 2, rand.nextInt(3) - 1);
                    if (world.getBlockState(blockPos1.down()).getBlock() != state.getBlock() || world.getBlockState(blockPos1).isNormalCube())
                    {
                        blockPos1 = null;
                        break;
                    }
                }
                if (blockPos1 == null)
                {
                    continue;
                }
                IBlockState currentState = world.getBlockState(blockPos1);
                if (config.getReplaceBlock() == null ? world.isAirBlock(blockPos1) : currentState.getBlock().getRegistryName().equals(config.getReplaceBlock()))
                {
                    if (config.getAdjacentBlock() == null || checkAdjacentBlock(world, blockPos1.down(), config.getAdjacentBlock()))
                    {
                        IBlockState spawnState = config.getRandomBlockState(rand);
                        if (FLOWER_ENTRY.equals(config.getSpawnBlocks().stream().filter(b -> b.getBlockState() == spawnState).map(SpawnBlock::getBlock).findFirst().orElse(null)))
                        {
                            IBlockState oldState = world.getBlockState(blockPos1);
                            biome.plantFlower(world, rand, blockPos1);
                            IBlockState newState = world.getBlockState(blockPos1);
                            if (newState != oldState)
                            {
                                world.notifyBlockUpdate(blockPos1, oldState, newState, 3);
                            }
                        }
                        else if (spawnState != null && spawnState.getBlock().canPlaceBlockAt(world, blockPos1))
                        {
                            world.setBlockState(blockPos1, spawnState, 3);
                            spawnState.getBlock().onBlockPlacedBy(world, blockPos1, spawnState, placer, stack);
                            world.notifyBlockUpdate(blockPos1, world.getBlockState(blockPos1), spawnState, 3);
                            world.notifyBlockUpdate(blockPos1.up(), world.getBlockState(blockPos1.up()), spawnState, 3);
                        }
                    }
                }
            }
            matched = true;
        }
        if (!matched)
        {
            BonemealTweaker.LOGGER.debug("No matching config found for block: {}, biome: {}, dimension: {}, replaceBlock: {}", blockRL, biomeName, dimension, targetState.getBlock().getRegistryName());
        }
        return matched;
    }

    private static boolean checkPreconditions(World world, int dimension, String biomeName, BlockPos pos, ResourceLocation targetRL, BlockConfig config, boolean isBonemeal)
    {
        if (isBonemeal && !config.getApplyMode().isBonemeal()) return false;
        if (!isBonemeal && !config.getApplyMode().isSurface()) return false;
        if (!config.getBiomes().isEmpty() && !config.getBiomes().contains(biomeName))
        {
            BonemealTweaker.LOGGER.debug("Biome {} at {} is not in allowed biomes list: {}", biomeName, pos, config.getBiomes());
            return false;
        }
        if (!config.getDimensions().isEmpty() && !config.getDimensions().contains(dimension))
        {
            BonemealTweaker.LOGGER.debug("Dimension {} is not in allowed dimensions list: {}", dimension, config.getDimensions());
            return false;
        }
        if (!(config.getReplaceBlock() == null ? world.isAirBlock(pos.up()) : targetRL.equals(config.getReplaceBlock())))
        {
            BonemealTweaker.LOGGER.debug("Target block {} at {} does not match replaceBlock: {}", targetRL, pos.up(), config.getReplaceBlock());
            return false;
        }
        return true;
    }

    private static boolean checkAdjacentBlock(World world, BlockPos pos, ResourceLocation adjacentBlock)
    {
        for (EnumFacing facing : EnumFacing.HORIZONTALS)
        {
            if (world.getBlockState(pos.offset(facing)).getBlock().getRegistryName().equals(adjacentBlock))
            {
                return true;
            }
        }
        return false;
    }
}
