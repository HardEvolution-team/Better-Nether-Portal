package com.ded.bnp;

import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PotionEffectsCustom {
    public static final Potion PORTAL_COOLDOWN = new PotionCooldown()
            .setRegistryName(new ResourceLocation(Tags.MODID, "portal_cooldown"))
            .setPotionName("effect." + Tags.MODID + ".portal_cooldown");

    public static class PotionCooldown extends Potion {
        public PotionCooldown() {
            super(true, 0x550055); // true - вредный эффект, фиолетовый цвет
            this.setIconIndex(0, 0); // Без иконки
        }
    }

    @Mod.EventBusSubscriber
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerPotions(RegistryEvent.Register<Potion> event) {
            event.getRegistry().register(PORTAL_COOLDOWN);
            MyMod.LOGGER.info("Registered portal cooldown potion effect");
        }
    }
}