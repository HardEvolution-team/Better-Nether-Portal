package com.ded.bnp;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.Level;
import java.lang.reflect.Constructor;
import com.google.common.base.Strings;
import com.google.common.collect.ObjectArrays;

public class ModBlocks {
    public static Block CustomPortal;
    public static Block PortalCore;

    public static void init() {
        CustomPortal = new BlockCustomPortal().setTranslationKey("custom_portal");
        PortalCore = new BlockPortalCore().setTranslationKey("portal_core").setCreativeTab(CreativeTabs.MISC);
    }

    public static void InGameRegister() {
        registerBlock(CustomPortal, CustomPortal.getTranslationKey().substring(5));
        registerBlock(PortalCore, PortalCore.getTranslationKey().substring(5));
    }

    public static void Render() {
        registerRender(PortalCore);
        registerRender(CustomPortal);
    }

    @SideOnly(Side.CLIENT)
    public static void initModels() {
    }

    @Deprecated
    public static Block registerBlock(Block block)
    {
        ForgeRegistries.BLOCKS.register(block);
        ForgeRegistries.ITEMS.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        return block;
    }

    @Deprecated
    public static Block registerBlock(Block block, String name)
    {
        if (block.getRegistryName() == null && Strings.isNullOrEmpty(name))
            throw new IllegalArgumentException("Attempted to register a Block with no name: " + block);
        if (block.getRegistryName() != null && !block.getRegistryName().toString().equals(name))
            throw new IllegalArgumentException("Attempted to register a Block with conflicting names. Old: " + block.getRegistryName() + " New: " + name);
        return registerBlock(block.getRegistryName() != null ? block : block.setRegistryName(name));
    }

    @Deprecated
    public static Block registerBlock(Block block, Class<? extends ItemBlock> itemclass, String name, Object... itemCtorArgs)
    {
        if (Strings.isNullOrEmpty(name))
        {
            throw new IllegalArgumentException("Attempted to register a block with no name: " + block);
        }
        if (Loader.instance().isInState(LoaderState.CONSTRUCTING))
        {
            FMLLog.warning("The mod %s is attempting to register a block whilst it it being constructed. This is bad modding practice - please use a proper mod lifecycle event.", Loader.instance().activeModContainer());
        }
        try
        {
            assert block != null : "registerBlock: block cannot be null";
            if (block.getRegistryName() != null && !block.getRegistryName().toString().equals(name))
                throw new IllegalArgumentException("Attempted to register a Block with conflicting names. Old: " + block.getRegistryName() + " New: " + name);
            ItemBlock i = null;
            if (itemclass != null)
            {
                Class<?>[] ctorArgClasses = new Class<?>[itemCtorArgs.length + 1];
                ctorArgClasses[0] = Block.class;
                for (int idx = 1; idx < ctorArgClasses.length; idx++)
                {
                    ctorArgClasses[idx] = itemCtorArgs[idx - 1].getClass();
                }
                Constructor<? extends ItemBlock> itemCtor = itemclass.getConstructor(ctorArgClasses);
                i = itemCtor.newInstance(ObjectArrays.concat(block, itemCtorArgs));
            }
            // block registration has to happen first
            ForgeRegistries.BLOCKS.register(block.getRegistryName() == null ? block.setRegistryName(name) : block);
            if (i != null)
                ForgeRegistries.ITEMS.register(i.setRegistryName(name));
            return block;
        } catch (Exception e)
        {
            FMLLog.log(Level.ERROR, e, "Caught an exception during block registration");
            throw new LoaderException(e);
        }
    }

    public static void registerRender(Block block) {
        Item item = Item.getItemFromBlock(block);
        String registryName = item.getTranslationKey().substring(5);
        ModelResourceLocation modelLoc = new ModelResourceLocation(Tags.MODID + ":" + registryName, "inventory");
        
        // Добавляем логирование регистрации рендера
        FMLLog.log(Level.INFO, "[BNP] Регистрация рендера для блока: " + block.getTranslationKey() + ", модель: " + modelLoc);
        
        try {
            // Проверяем наличие текстуры, если это портал
            if (block instanceof BlockCustomPortal) {
                ResourceLocation portalTexture = new ResourceLocation(Tags.MODID, "textures/blocks/portal.png");
                try {
                    boolean textureExists = Minecraft.getMinecraft().getResourceManager().getResource(portalTexture) != null;
                    FMLLog.log(Level.INFO, "[BNP] Текстура портала " + portalTexture + " существует: " + textureExists);
                } catch (Exception e) {
                    FMLLog.log(Level.ERROR, "[BNP] Ошибка при проверке текстуры портала: " + e.getMessage());
                }
            }
            
            Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, modelLoc);
            FMLLog.log(Level.INFO, "[BNP] Рендер для блока " + block.getTranslationKey() + " успешно зарегистрирован");
        } catch (Exception e) {
            FMLLog.log(Level.ERROR, "[BNP] Ошибка при регистрации рендера для блока " + block.getTranslationKey() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
