package com.ded.bnp;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ModItems {



    @GameRegistry.ObjectHolder("bnp:portal_core")
    public static BlockPortalCore portalCore;





    @SideOnly(Side.CLIENT)
    public static void initModels() {
        portalCore.initModel();
    }
}
