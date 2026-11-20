package ded.betternetherportal.event;

import ded.betternetherportal.block.BetterPortalBlock;
import ded.betternetherportal.block.PortalCoreBlock;
import ded.betternetherportal.config.ModConfig;
import ded.betternetherportal.portal.PortalManager;
import ded.betternetherportal.tile.PortalCoreTileEntity;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;






public class PortalEventHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        BlockPos pos = event.getPos();
        Block block = event.getState().getBlock();

        
        if (block instanceof PortalCoreBlock) {
            return;
        }

        
        
        boolean interior = block instanceof BetterPortalBlock;
        if (!interior && block != ModConfig.getFrameBlock()) {
            return;
        }

        PortalCoreTileEntity core = PortalManager.findOwningCore(world, pos, interior);
        if (core != null) {
            PortalManager.deactivateByFrameBreak(world, core.getPos());
        }
    }

    



    @SubscribeEvent
    public void onVanillaPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (ModConfig.disableVanillaPortal) {
            event.setCanceled(true);
        }
    }
}
