package ded.betternetherportal;

import ded.betternetherportal.block.BetterPortalBlock;
import ded.betternetherportal.block.PortalCoreBlock;
import ded.betternetherportal.config.ModConfig;
import ded.betternetherportal.event.PortalEventHandler;
import ded.betternetherportal.item.PortalCoreItemBlock;
import ded.betternetherportal.proxy.IProxy;
import ded.betternetherportal.tile.PortalCoreTileEntity;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        guiFactory = "ded.betternetherportal.client.gui.ModGuiFactory")
public class BetterNetherPortal {

    public static final String MODID = Reference.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    public static Block portalCoreBlock;
    public static Block betterPortalBlock;

    public static Item portalCoreItem;

    



    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(Reference.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(portalCoreItem != null ? portalCoreItem : Item.getItemFromBlock(portalCoreBlock));
        }
    };

    @SidedProxy(modId = Reference.MOD_ID,
            clientSide = "ded.betternetherportal.proxy.ClientProxy",
            serverSide = "ded.betternetherportal.proxy.CommonProxy")
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.load(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(this);
        GameRegistry.registerTileEntity(PortalCoreTileEntity.class,
                new ResourceLocation(MODID, "portal_core"));
        MinecraftForge.EVENT_BUS.register(new PortalEventHandler());
        LOGGER.info("Better Nether Portal pre-init complete. Vanilla portal disabled: {}",
                ModConfig.disableVanillaPortal);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        
    }

    @Mod.EventHandler
    public void serverStopped(net.minecraftforge.fml.common.event.FMLServerStoppedEvent event) {
        ded.betternetherportal.portal.PortalIndex.clear();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Reference.MOD_ID.equals(event.getModID())) {
            ModConfig.sync();
        }
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        portalCoreBlock = new PortalCoreBlock();
        betterPortalBlock = new BetterPortalBlock();
        event.getRegistry().register(portalCoreBlock);
        event.getRegistry().register(betterPortalBlock);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        portalCoreItem = new PortalCoreItemBlock(portalCoreBlock)
                .setRegistryName(portalCoreBlock.getRegistryName());
        portalCoreItem.setCreativeTab(CREATIVE_TAB);
        event.getRegistry().register(portalCoreItem);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(portalCoreItem, 0,
                new net.minecraft.client.renderer.block.model.ModelResourceLocation(
                        portalCoreBlock.getRegistryName(), "active=false,facing=north"));
    }
}
