package mod.acgaming.bonemealtweaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = BonemealTweaker.MOD_ID)
@Mod(modid = BonemealTweaker.MOD_ID, name = BonemealTweaker.NAME, version = BonemealTweaker.VERSION, acceptedMinecraftVersions = BonemealTweaker.ACCEPTED_VERSIONS)
public class BonemealTweaker
{
    public static final String MOD_ID = Tags.MOD_ID;
    public static final String NAME = Tags.NAME;
    public static final String VERSION = Tags.VERSION;
    public static final String ACCEPTED_VERSIONS = "[1.12.2]";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, BlockConfig> BLOCK_CONFIGS = new HashMap<>();
    private static final String FLOWER_ENTRY = "flowerEntry";
    private static File configDir;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBonemeal(BonemealEvent event)
    {
        if (applyCustomBonemeal(event.getWorld(), event.getPos(), event.getBlock()))
        {
            event.setCanceled(true);
        }
    }

    public static boolean applyCustomBonemeal(World world, BlockPos pos, IBlockState state)
    {
        if (world.isRemote) return false;
        ResourceLocation blockId = state.getBlock().getRegistryName();
        BlockConfig config = BLOCK_CONFIGS.get(blockId);
        if (config == null) return false;

        Biome biome = world.getBiome(pos);
        int dimension = world.provider.getDimension();
        if ((!config.getBiomes().isEmpty() && !config.getBiomes().contains(biome.getRegistryName().toString())) || (!config.getDimensions().isEmpty() && !config.getDimensions().contains(dimension)))
        {
            return false;
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
                    if (world.isAirBlock(blockpos1))
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
                            world.notifyBlockUpdate(blockpos1, world.getBlockState(blockpos1), spawnState, 3);
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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        configDir = new File(event.getModConfigurationDirectory(), MOD_ID);
        if (!configDir.exists())
        {
            configDir.mkdirs();
        }
        loadConfigs();
    }

    private void loadConfigs()
    {
        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (configFiles == null) return;
        for (File file : configFiles)
        {
            try
            {
                String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                JsonObject config = GSON.fromJson(json, JsonObject.class);
                String blockName = config.get("block").getAsString();
                int iterations = config.get("iterations").getAsInt();
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
                BLOCK_CONFIGS.put(new ResourceLocation(blockName), new BlockConfig(iterations, biomes, dimensions, spawnBlocks));
            }
            catch (IOException | JsonParseException e)
            {
                LOGGER.error("Failed to load config: {}", file.getName(), e);
            }
        }
    }
}
