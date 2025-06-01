package com.ded.bnp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    
    private static final Logger LOGGER = LogManager.getLogger(Tags.MODID);

    /**
     * Регистрация рендеров блоков
     */
    @SubscribeEvent
    public void registerRenders() {
        ModBlocks.Render();
    }
    
    /**
     * Регистрация моделей для всех блоков и предметов мода
     */
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        LOGGER.info("[BNP] Начало регистрации моделей");
        
        registerPortalCoreModels();
        registerCustomPortalModels();
        
        // Регистрация моделей предметов
        ModItems.initModels();
        
        LOGGER.info("[BNP] Регистрация моделей завершена");
    }
    
    /**
     * Регистрация моделей для ядра портала
     */
    private static void registerPortalCoreModels() {
        // Регистрируем только одну модель для предмета в инвентаре/руке
        // Это гарантирует, что в руке всегда будет отображаться неактивная модель
        ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(ModBlocks.PortalCore), 0,
                new ModelResourceLocation(ModBlocks.PortalCore.getRegistryName(), "inventory")
        );
        
        // Остальные модели для блока в мире регистрируются через blockstate JSON
    }
    
    /**
     * Регистрация моделей для кастомного портала
     */
    private static void registerCustomPortalModels() {
        LOGGER.info("[BNP] Регистрация моделей для кастомного портала");
        
        try {
            // Регистрируем модели для всех направлений портала
            String[] directions = {"north", "south", "east", "west"};
            for (int i = 0; i < directions.length; i++) {
                ModelResourceLocation modelLoc = new ModelResourceLocation(
                        ModBlocks.CustomPortal.getRegistryName(), "facing=" + directions[i]);
                        
                ModelLoader.setCustomModelResourceLocation(
                        Item.getItemFromBlock(ModBlocks.CustomPortal), i, modelLoc
                );
            }
        } catch (Exception e) {
            LOGGER.error("[BNP] Ошибка при регистрации моделей для кастомного портала: " + e.getMessage());
        }
    }
}
