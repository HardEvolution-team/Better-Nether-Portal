package ded.betternetherportal.config;

import ded.betternetherportal.Reference;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.io.File;






public final class ModConfig {

    private ModConfig() {}

    
    public static boolean disableVanillaPortal = true;

    
    public static String activationItem = "minecraft:ender_pearl";

    



    public static boolean strict5x5Frame = false;

    
    public static String frameBlock = "minecraft:obsidian";

    
    public static int teleportCooldownTicks = 100;

    
    public static boolean allowMobTeleport = true;

    
    public static boolean playTeleportSound = true;

    
    public static int netherCoordinateScale = 8;

    
    private static Configuration config;
    private static Item cachedActivationItem;
    private static boolean activationItemResolved;
    private static Block cachedFrameBlock;
    private static boolean frameBlockResolved;

    public static void load(File configFile) {
        config = new Configuration(configFile);
        sync();
    }

    
    public static Configuration getConfig() {
        return config;
    }

    public static void sync() {
        if (config == null) {
            return;
        }

        disableVanillaPortal = config.getBoolean(
                "disableVanillaPortal", Configuration.CATEGORY_GENERAL, true,
                "Disable the vanilla nether portal ignition. The portal core must be used instead.");

        activationItem = config.getString(
                "activationItem", Configuration.CATEGORY_GENERAL, "minecraft:ender_pearl",
                "Registry name of the item used to activate a portal core (e.g. minecraft:ender_pearl).");

        strict5x5Frame = config.getBoolean(
                "strict5x5Frame", Configuration.CATEGORY_GENERAL, false,
                "If true, the core only activates inside an exact 5x5 frame with the core at the bottom-center. "
                        + "If false, any closed frame around the core is accepted.");

        frameBlock = config.getString(
                "frameBlock", Configuration.CATEGORY_GENERAL, "minecraft:obsidian",
                "Registry name of the block the portal frame must be made of.");

        teleportCooldownTicks = config.getInt(
                "teleportCooldownTicks", Configuration.CATEGORY_GENERAL, 100, 0, 1200,
                "Cooldown in ticks before an entity that just teleported can teleport again.");

        allowMobTeleport = config.getBoolean(
                "allowMobTeleport", Configuration.CATEGORY_GENERAL, true,
                "If false, only players can travel through portals; mobs, items and other entities cannot.");

        playTeleportSound = config.getBoolean(
                "playTeleportSound", Configuration.CATEGORY_GENERAL, true,
                "Play the portal travel sound when an entity teleports.");

        netherCoordinateScale = config.getInt(
                "netherCoordinateScale", Configuration.CATEGORY_GENERAL, 8, 1, 64,
                "Horizontal coordinate scale between the Overworld and the Nether when linking portals.");

        
        
        for (net.minecraftforge.common.config.Property prop
                : config.getCategory(Configuration.CATEGORY_GENERAL).values()) {
            prop.setLanguageKey(Reference.MOD_ID + ".config." + prop.getName());
        }

        cachedActivationItem = null;
        activationItemResolved = false;
        cachedFrameBlock = null;
        frameBlockResolved = false;

        if (config.hasChanged()) {
            config.save();
        }
    }

    
    public static Item getActivationItem() {
        if (!activationItemResolved) {
            cachedActivationItem = Item.getByNameOrId(activationItem);
            if (cachedActivationItem == null) {
                cachedActivationItem = Item.getByNameOrId("minecraft:ender_pearl");
            }
            activationItemResolved = true;
        }
        return cachedActivationItem;
    }

    
    public static Block getFrameBlock() {
        if (!frameBlockResolved) {
            cachedFrameBlock = Block.getBlockFromName(frameBlock);
            if (cachedFrameBlock == null) {
                cachedFrameBlock = net.minecraft.init.Blocks.OBSIDIAN;
            }
            frameBlockResolved = true;
        }
        return cachedFrameBlock;
    }

    
    public static ResourceLocation getFrameBlockName() {
        return new ResourceLocation(frameBlock);
    }

    public static String modId() {
        return Reference.MOD_ID;
    }
}
