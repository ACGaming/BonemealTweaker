package mod.acgaming.bonemealtweaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import mod.acgaming.bonemealtweaker.config.json.BlockConfig;
import mod.acgaming.bonemealtweaker.config.json.SpawnBlock;
import mod.acgaming.bonemealtweaker.gen.BTWorldGenerator;

@Mod(modid = BonemealTweaker.MOD_ID, name = BonemealTweaker.NAME, version = BonemealTweaker.VERSION, acceptedMinecraftVersions = BonemealTweaker.ACCEPTED_VERSIONS)
public class BonemealTweaker
{
    public static final String MOD_ID = Tags.MOD_ID;
    public static final String NAME = Tags.NAME;
    public static final String VERSION = Tags.VERSION;
    public static final String ACCEPTED_VERSIONS = "[1.12.2]";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static final Map<ResourceLocation, List<BlockConfig>> BLOCK_CONFIGS = new Object2ObjectOpenHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FLOWER_ENTRY = "flowerEntry";
    private static File configDir;

    public static boolean applyBlockPlacement(World world, BlockPos pos, IBlockState state, EntityPlayer placer, ItemStack stack, boolean isBonemeal)
    {
        if (world.isRemote) return false;
        ResourceLocation blockRL = state.getBlock().getRegistryName();
        List<BlockConfig> configs = BLOCK_CONFIGS.get(blockRL);
        if (configs == null)
        {
            LOGGER.debug("No configs found for block: {}", blockRL);
            return false;
        }
        Biome biome = world.getBiome(pos);
        ResourceLocation biomeName = biome.getRegistryName();
        int dimension = world.provider.getDimension();
        IBlockState targetState = world.getBlockState(pos.up());
        for (BlockConfig config : configs)
        {
            if (isBonemeal && !config.getApplyMode().isBonemeal()) continue;
            if (!isBonemeal && !config.getApplyMode().isSurface()) continue;
            if (!config.getBiomes().isEmpty())
            {
                if (biomeName == null)
                {
                    LOGGER.debug("Biome at {} has no valid registry name", pos);
                    continue;
                }
                String biomeNameStr = biomeName.toString();
                if (!config.getBiomes().contains(biomeNameStr))
                {
                    LOGGER.debug("Biome '{}' at {} is not in allowed biomes list: {}", biomeNameStr, pos, config.getBiomes());
                    continue;
                }
            }
            if (!config.getDimensions().isEmpty() && !config.getDimensions().contains(dimension))
            {
                LOGGER.debug("Dimension {} is not in allowed dimensions list: {}", dimension, config.getDimensions());
                continue;
            }
            if (!(config.getReplaceBlock() == null ? world.isAirBlock(pos.up()) : targetState.getBlock().getRegistryName().equals(config.getReplaceBlock())))
            {
                LOGGER.debug("Target block {} at {} does not match replaceBlock: {}", targetState.getBlock().getRegistryName(), pos.up(), config.getReplaceBlock());
                continue;
            }
            Random rand = world.rand;
            BlockPos blockpos = pos.up();
            for (int i = 0; i < config.getIterations(); ++i)
            {
                BlockPos blockpos1 = blockpos;
                int j = 0;
                while (true)
                {
                    if (j >= i / 16)
                    {
                        IBlockState currentState = world.getBlockState(blockpos1);
                        if (config.getReplaceBlock() == null ? world.isAirBlock(blockpos1) : currentState.getBlock().getRegistryName().equals(config.getReplaceBlock()))
                        {
                            IBlockState spawnState = config.getRandomBlockState(rand);
                            if (FLOWER_ENTRY.equals(config.getSpawnBlocks().stream().filter(b -> b.getBlockState() == spawnState).map(SpawnBlock::getBlock).findFirst().orElse(null)))
                            {
                                IBlockState oldState = world.getBlockState(blockpos1);
                                biome.plantFlower(world, rand, blockpos1);
                                IBlockState newState = world.getBlockState(blockpos1);
                                if (newState != oldState)
                                {
                                    world.notifyBlockUpdate(blockpos1, oldState, newState, 3);
                                }
                            }
                            else if (spawnState != null && spawnState.getBlock().canPlaceBlockAt(world, blockpos1))
                            {
                                world.setBlockState(blockpos1, spawnState, 3);
                                spawnState.getBlock().onBlockPlacedBy(world, blockpos1, spawnState, placer, stack);
                                world.notifyBlockUpdate(blockpos1, world.getBlockState(blockpos1), spawnState, 3);
                                world.notifyBlockUpdate(blockpos1.up(), world.getBlockState(blockpos1.up()), spawnState, 3);
                            }
                        }
                        break;
                    }
                    blockpos1 = blockpos1.add(rand.nextInt(3) - 1, (rand.nextInt(3) - 1) * rand.nextInt(3) / 2, rand.nextInt(3) - 1);
                    if (world.getBlockState(blockpos1.down()).getBlock() != state.getBlock() || world.getBlockState(blockpos1).isNormalCube())
                    {
                        break;
                    }
                    ++j;
                }
            }
            return true;
        }
        LOGGER.debug("No matching config found for block: {}, biome: {}, dimension: {}, replaceBlock: {}", blockRL, biomeName, dimension, targetState.getBlock().getRegistryName());
        return false;
    }

    public static void loadConfigs()
    {
        BLOCK_CONFIGS.clear();
        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (configFiles == null) return;
        for (File file : configFiles)
        {
            try
            {
                String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                JsonObject config = GSON.fromJson(json, JsonObject.class);
                String blockName = config.get("block").getAsString();
                ResourceLocation blockRL = new ResourceLocation(blockName);
                ResourceLocation replaceBlock = config.has("replaceBlock") ? new ResourceLocation(config.get("replaceBlock").getAsString()) : null;
                int iterations = config.get("iterations").getAsInt();
                String applyModeStr = config.has("applyMode") ? config.get("applyMode").getAsString().toUpperCase() : "BONEMEAL";
                BlockConfig.ApplyMode applyMode;
                try
                {
                    applyMode = BlockConfig.ApplyMode.valueOf(applyModeStr);
                }
                catch (IllegalArgumentException e)
                {
                    LOGGER.error("Invalid applyMode '{}' in config {}, defaulting to BONEMEAL", applyModeStr, file.getName());
                    applyMode = BlockConfig.ApplyMode.BONEMEAL;
                }
                List<String> biomes = new ArrayList<>();
                config.get("biomes").getAsJsonArray().forEach(e -> biomes.add(e.getAsString()));
                List<Integer> dimensions = new ArrayList<>();
                config.get("dimensions").getAsJsonArray().forEach(e -> dimensions.add(e.getAsInt()));
                List<SpawnBlock> spawnBlocks = new ArrayList<>();
                config.get("spawnBlocks").getAsJsonArray().forEach(e -> {
                    JsonObject obj = e.getAsJsonObject();
                    String block = obj.get("block").getAsString();
                    int weight = obj.get("weight").getAsInt();
                    spawnBlocks.add(new SpawnBlock(block, weight));
                });
                BLOCK_CONFIGS.computeIfAbsent(blockRL, k -> new ArrayList<>()).add(new BlockConfig(replaceBlock, iterations, applyMode, biomes, dimensions, spawnBlocks));
            }
            catch (IOException | JsonParseException e)
            {
                LOGGER.error("Failed to load config: {}", file.getName(), e);
            }
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        configDir = new File(event.getModConfigurationDirectory(), MOD_ID);
        if (!configDir.exists())
        {
            configDir.mkdirs();
        }
        loadConfigs();
        GameRegistry.registerWorldGenerator(new BTWorldGenerator(), 100);
    }
}
