package mod.acgaming.bonemealtweaker.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import mod.acgaming.bonemealtweaker.BonemealTweaker;

@Config(modid = BonemealTweaker.MOD_ID)
public class BTConfig
{
    @Config.Name("Exclusive Mode")
    @Config.Comment("Disables all other bonemeal events except those specified via JSON config files")
    public static boolean exclusiveMode = false;

    @Config.Name("Reload Configs")
    @Config.Comment("Reloads all JSON config files from the 'bonemealtweaker' subfolder")
    public static boolean reloadConfigs = false;

    @Mod.EventBusSubscriber(modid = BonemealTweaker.MOD_ID)
    public static class EventHandler
    {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
        {
            if (event.getModID().equals(BonemealTweaker.MOD_ID))
            {
                ConfigManager.sync(BonemealTweaker.MOD_ID, Config.Type.INSTANCE);
                if (reloadConfigs)
                {
                    BonemealTweaker.loadConfigs();
                    reloadConfigs = false;
                    ConfigManager.sync(BonemealTweaker.MOD_ID, Config.Type.INSTANCE);
                    BonemealTweaker.LOGGER.info("Reloaded JSON config files!");
                }
            }
        }
    }
}
