package ded.betternetherportal.portal;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;





public class PortalTeleporter implements ITeleporter {

    private final WorldServer world;
    private final BlockPos dest;

    public PortalTeleporter(WorldServer world, BlockPos dest) {
        this.world = world;
        this.dest = dest;
    }

    @Override
    public void placeEntity(World world, Entity entity, float yaw) {
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;
        entity.setLocationAndAngles(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D, yaw, entity.rotationPitch);
    }
}
