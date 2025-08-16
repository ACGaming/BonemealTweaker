package mod.acgaming.bonemealtweaker.gen;

import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import mod.acgaming.bonemealtweaker.BonemealTweaker;
import mod.acgaming.bonemealtweaker.config.BTConfig;

@Mod.EventBusSubscriber(modid = BonemealTweaker.MOD_ID)
public class BTBonemealEvent
{
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBonemeal(BonemealEvent event)
    {
        if (BonemealTweaker.applyBlockPlacement(event.getWorld(), event.getPos(), event.getBlock(), event.getEntityPlayer(), event.getStack(), true))
        {
            event.setResult(Event.Result.ALLOW);
        }
        else if (BTConfig.exclusiveMode && BonemealTweaker.BLOCK_CONFIGS.containsKey(event.getBlock().getBlock().getRegistryName()))
        {
            event.setCanceled(true);
        }
    }
}
