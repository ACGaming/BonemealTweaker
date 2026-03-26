package mod.acgaming.bonemealtweaker;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
    private static File configDir;

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
                List<ResourceLocation> blockRLs = new ArrayList<>();
                if (config.has("block"))
                {
                    blockRLs.add(new ResourceLocation(config.get("block").getAsString()));
                }
                else
                {
                    config.get("blocks").getAsJsonArray().forEach(e -> blockRLs.add(new ResourceLocation(e.getAsString())));
                }
                ResourceLocation replaceBlock = config.has("replaceBlock") ? new ResourceLocation(config.get("replaceBlock").getAsString()) : null;
                ResourceLocation adjacentBlock = config.has("adjacentBlock") ? new ResourceLocation(config.get("adjacentBlock").getAsString()) : null;
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
                int genDensity = applyMode == BlockConfig.ApplyMode.BONEMEAL ? 0 : config.get("genDensity").getAsInt();
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
                BlockConfig blockConfig = new BlockConfig(replaceBlock, adjacentBlock, iterations, applyMode, genDensity, biomes, dimensions, spawnBlocks);
                for (ResourceLocation blockRL : blockRLs)
                {
                    BLOCK_CONFIGS.computeIfAbsent(blockRL, k -> new ArrayList<>()).add(blockConfig);
                }
                BLOCK_CONFIGS.values().forEach(list -> list.sort(Comparator.comparing(c -> c.getAdjacentBlock() == null ? 1 : 0)));
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
