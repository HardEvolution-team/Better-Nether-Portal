package com.ded.bnp;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Основной класс мода, содержащий инициализацию и обработчики событий
 */
@Mod(modid = Tags.MODID, version = Tags.VERSION, name = Tags.MODNAME, acceptedMinecraftVersions = "[1.12.2]")
public class MyMod {

    @Mod.Instance
    public static MyMod instance;

    public static final Logger LOGGER = LogManager.getLogger(Tags.MODID);
    
    @SidedProxy(clientSide = "com.ded.bnp.ClientProxy", serverSide = "com.ded.bnp.CommonProxy")
    public static CommonProxy proxy;
    
    /**
     * Инициализация мода перед загрузкой
     */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("I am " + Tags.MODNAME + " at version " + Tags.VERSION);
        ModBlocks.init();
        ModBlocks.InGameRegister();
    }

    /**
     * Регистрация рецептов
     */
    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        // Рецепты не используются в текущей версии
    }
    
    /**
     * Регистрация предметов
     */
    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        // Предметы регистрируются в ModItems
    }

    /**
     * Регистрация блоков
     */
    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        // Блоки регистрируются в ModBlocks
    }

    /**
     * Основная инициализация мода
     */
    @EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.registerTileEntity(TileEntityPortalCore.class, "tileEntityPortalCore");
        GameRegistry.registerTileEntity(TileEntityCustomPortal.class, "tileEntityCustomPortal");
    }
    
    /**
     * Пост-инициализация мода
     */
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Пост-инициализация не требуется в текущей версии
    }

    /**
     * Инициализация серверных команд
     */
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // Серверные команды не используются в текущей версии
    }
}
