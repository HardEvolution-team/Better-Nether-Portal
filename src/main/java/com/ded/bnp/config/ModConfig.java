package com.ded.bnp.config;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ded.bnp.Tags;

/**
 * Класс для управления конфигурацией мода
 */
@Config(modid = Tags.MODID)
public class ModConfig {
    private static final Logger LOGGER = LogManager.getLogger("BNP-Config");
    
    @Config.Comment({"Item used to activate the portal", "Default: minecraft:flint_and_steel"})
    @Config.Name("Portal Activator Item")
    @Config.LangKey("bnp.config.portal_activator")
    @Config.RequiresMcRestart
    public static String portalActivatorItem = "minecraft:flint_and_steel";
    
    private static Item cachedActivatorItem = null;
    
    /**
     * Получить предмет-активатор портала
     * @return Предмет для активации портала
     */
    public static Item getPortalActivatorItem() {
        if (cachedActivatorItem == null) {
            try {
                Item item = Item.getByNameOrId(portalActivatorItem);
                if (item != null) {
                    cachedActivatorItem = item;
                } else {
                    LOGGER.warn("Item not found: " + portalActivatorItem + ", using default");
                    cachedActivatorItem = Items.FLINT_AND_STEEL;
                }
            } catch (Exception e) {
                LOGGER.error("Error getting portal activator item: " + e.getMessage());
                cachedActivatorItem = Items.FLINT_AND_STEEL;
            }
        }
        return cachedActivatorItem;
    }
    
    /**
     * Обработчик события изменения конфигурации
     */
    @Mod.EventBusSubscriber(modid = Tags.MODID)
    public static class EventHandler {
        /**
         * Обработчик события изменения конфигурации
         * @param event Событие изменения конфигурации
         */
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Tags.MODID)) {
                ConfigManager.sync(Tags.MODID, Config.Type.INSTANCE);
                cachedActivatorItem = null; // Сбрасываем кэш при изменении конфигурации
                LOGGER.info("Configuration reloaded");
            }
        }
    }
}
