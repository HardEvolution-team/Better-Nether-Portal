package ded.betternetherportal.client.gui;

import ded.betternetherportal.Reference;
import ded.betternetherportal.config.ModConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class ModGuiConfig extends GuiConfig {

    public ModGuiConfig(GuiScreen parent) {
        super(parent,
                collectElements(),
                Reference.MOD_ID,
                false,
                false,
                net.minecraft.client.resources.I18n.format(Reference.MOD_ID + ".config.title"));
    }

    private static List<IConfigElement> collectElements() {
        List<IConfigElement> elements = new ArrayList<>();
        Configuration config = ModConfig.getConfig();
        if (config != null) {
            elements.add(new ConfigElement(
                    config.getCategory(Configuration.CATEGORY_GENERAL)));
        }
        return elements;
    }
}
