package com.ded.bnp.np_blocking;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.ded.bnp.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class FlintAndSteelHandler {

    /**
     * Предотвращает использование кремня и стали на обсидиане
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        Block targetBlock = event.getWorld().getBlockState(event.getPos()).getBlock();

        // Если игрок пытается использовать кремень и сталь на обсидиане
        if (!heldItem.isEmpty() && heldItem.getItem() == Items.FLINT_AND_STEEL && targetBlock == Blocks.OBSIDIAN) {
            // Отменяем событие, если это не наш собственный блок ядра портала
            if (!isCustomPortalCore(event.getPos(), event.getWorld())) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Проверяет, является ли блок нашим собственным ядром портала
     */
    private static boolean isCustomPortalCore(BlockPos pos, World world) {
        Block block = world.getBlockState(pos).getBlock();
        return block instanceof com.ded.bnp.BlockPortalCore;
    }
}