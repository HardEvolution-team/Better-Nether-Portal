package com.ded.bnp;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Базовый прокси-класс для общего функционала мода
 */
@Mod.EventBusSubscriber
public class CommonProxy {

    /**
     * Регистрация предметов
     * Метод переопределяется в ClientProxy
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // Регистрация предметов происходит в ModItems
    }
}
