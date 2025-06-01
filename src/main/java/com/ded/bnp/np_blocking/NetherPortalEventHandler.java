package com.ded.bnp.np_blocking;

import net.minecraft.block.BlockPortal;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.ded.bnp.Tags;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class NetherPortalEventHandler {

    /**
     * Отменяет создание стандартного портала в Ад
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        // Отменяем создание блока портала
        if (event.getPlacedBlock().getBlock() == Blocks.PORTAL) {
            event.setCanceled(true);
        }
    }

    /**
     * Отменяет телепортацию через стандартный портал в Ад
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        // Отменяем телепортацию в/из Ада (измерение -1)
        if (event.getDimension() == -1 || event.getEntity().dimension == -1) {
            event.setCanceled(true);
        }
    }

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
        // Проверяем только загруженные чанки
        for (BlockPos pos : BlockPos.getAllInBox(world.getSpawnPoint().add(-128, -64, -128),
                world.getSpawnPoint().add(128, 64, 128))) {
            if (world.getBlockState(pos).getBlock() == Blocks.PORTAL) {
                world.setBlockToAir(pos);
            }
        }
    }
}