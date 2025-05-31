package com.ded.bnp;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class TileEntityCustomPortal extends TileEntity {
    private UUID portalId;
    private BlockPos corePos;
    private EnumFacing facing = EnumFacing.NORTH;

    public void setPortalId(UUID id) {
        this.portalId = id;
        markDirty();
    }

    public UUID getPortalId() {
        return portalId;
    }

    public void setCorePos(BlockPos pos) {
        this.corePos = pos;
        markDirty();
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    public void setFacing(EnumFacing facing) {
        this.facing = facing;
        markDirty();
    }

    public EnumFacing getFacing() {
        return facing;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasUniqueId("PortalId")) {
            portalId = compound.getUniqueId("PortalId");
        }
        if (compound.hasKey("CoreX")) {
            corePos = new BlockPos(
                    compound.getInteger("CoreX"),
                    compound.getInteger("CoreY"),
                    compound.getInteger("CoreZ")
            );
        }
        if (compound.hasKey("Facing")) {
            facing = EnumFacing.byIndex(compound.getInteger("Facing"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (portalId != null) {
            compound.setUniqueId("PortalId", portalId);
        }
        if (corePos != null) {
            compound.setInteger("CoreX", corePos.getX());
            compound.setInteger("CoreY", corePos.getY());
            compound.setInteger("CoreZ", corePos.getZ());
        }
        compound.setInteger("Facing", facing.getIndex());
        return compound;
    }
}
