package com.ded.bnp.np_blocking;

import net.minecraft.block.BlockPortal;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Level;

import com.ded.bnp.Tags;
import com.ded.bnp.config.ModConfig;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class NetherPortalEventHandler {

    /**
     * Отменяет создание стандартного портала в Ад
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        // Отменяем создание блока портала
        if (event.getPlacedBlock().getBlock() == Blocks.PORTAL) {
            FMLLog.log(Level.INFO, "[BNP] Отменено создание ванильного портала на позиции %s", event.getPos().toString());
            event.setCanceled(true);
        }
    }

    /**
     * Отменяет телепортацию через стандартный портал в Ад
     */
//    @SubscribeEvent(priority = EventPriority.HIGHEST)
//    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
//        // Отменяем телепортацию в/из Ада (измерение -1)
//        if (event.getDimension() == -1 || event.getEntity().dimension == -1) {
//            FMLLog.log(Level.INFO, "[BNP] Отменена телепортация сущности %s в измерение %d",
//                       event.getEntity().getName(), event.getDimension());
//            event.setCanceled(true);
//        }
//    }

    /**
     * Предотвращает создание порталов через обсидиан и огонь
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world != null && !event.world.isRemote) {
            // Выполняем только раз в 20 тиков (1 секунду) для оптимизации
            if (event.world.getTotalWorldTime() % 20 == 0) {
                removeVanillaPortals(event.world);
            }
        }
    }

    /**
     * Удаляет существующие ванильные порталы
     */
    private static void removeVanillaPortals(World world) {
        int removedCount = 0;
        
        // Проверяем только загруженные чанки вокруг точки спавна
        for (BlockPos pos : BlockPos.getAllInBox(world.getSpawnPoint().add(-128, -64, -128),
                world.getSpawnPoint().add(128, 64, 128))) {
            if (world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() == Blocks.PORTAL) {
                world.setBlockToAir(pos);
                removedCount++;
            }
        }
        
        // Логируем только если были удалены порталы
        if (removedCount > 0) {
            FMLLog.log(Level.INFO, "[BNP] Удалено %d ванильных порталов в измерении %d", 
                       removedCount, world.provider.getDimension());
        }
    }
}
