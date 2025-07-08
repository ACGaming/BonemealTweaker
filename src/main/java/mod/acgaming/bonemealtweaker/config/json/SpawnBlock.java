package mod.acgaming.bonemealtweaker.config.json;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import mod.acgaming.bonemealtweaker.BonemealTweaker;

public class SpawnBlock
{
    private static final String FLOWER_ENTRY = "flowerEntry";
    private final String block;
    private final int weight;
    private IBlockState state;

    public SpawnBlock(String block, int weight)
    {
        this.block = block;
        this.weight = weight;
    }

    public IBlockState getBlockState()
    {
        if (FLOWER_ENTRY.equals(block)) return null;
        if (state == null)
        {
            String[] parts = block.split("\\[");
            Block b = Block.getBlockFromName(parts[0]);
            if (b == null)
            {
                BonemealTweaker.LOGGER.error("Invalid block: {}", block);
                return null;
            }
            state = b.getDefaultState();
            if (parts.length > 1)
            {
                String[] properties = parts[1].replace("]", "").split(",");
                for (String prop : properties)
                {
                    String[] kv = prop.split("=");
                    IProperty<?> property = b.getBlockState().getProperty(kv[0]);
                    if (property != null)
                    {
                        try
                        {
                            Object value = property.parseValue(kv[1]).orNull();
                            if (value != null)
                            {
                                state = state.withProperty((IProperty) property, (Comparable) value);
                            }
                            else
                            {
                                BonemealTweaker.LOGGER.error("Invalid value for property {} in block state: {}", kv[0], block);
                            }
                        }
                        catch (Exception e)
                        {
                            BonemealTweaker.LOGGER.error("Failed to parse block state property: {}", block, e);
                        }
                    }
                    else
                    {
                        BonemealTweaker.LOGGER.error("Unknown property {} for block state: {}", kv[0], block);
                    }
                }
            }
        }
        return state;
    }

    public String getBlock()
    {
        return block;
    }

    public int getWeight()
    {
        return weight;
    }
}
