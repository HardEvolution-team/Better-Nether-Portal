package com.ded.bnp.np_blocking;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.ded.bnp.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class BlockIgnitionHandler {

    /**
     * Предотвращает создание порталов через поджигание обсидиана
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockIgnite(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();

        // Проверяем, является ли блок огнем
        if (world.getBlockState(pos).getBlock() == Blocks.FIRE) {
            // Проверяем наличие обсидиана вокруг
            boolean hasObsidian = false;

            for (BlockPos checkPos : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                if (world.getBlockState(checkPos).getBlock() == Blocks.OBSIDIAN) {
                    hasObsidian = true;
                    break;
                }
            }

            // Если рядом есть обсидиан, удаляем огонь для предотвращения создания портала
            if (hasObsidian) {
                world.setBlockToAir(pos);
            }
        }
    }
}