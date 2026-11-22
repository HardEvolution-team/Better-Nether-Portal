package ded.betternetherportal.tile;

import ded.betternetherportal.config.ModConfig;
import ded.betternetherportal.portal.PortalManager;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import ded.betternetherportal.portal.PortalIndex;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PortalCoreTileEntity extends TileEntity implements ITickable {

    private static final int NO_DIM = Integer.MIN_VALUE;

    private int energyStored;
    
    private boolean generatedDestination;

    public void setGeneratedDestination(boolean value) {
        generatedDestination = value;
        markDirty();
    }
    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = Math.min(Math.max(0, maxReceive), Math.min(ModConfig.portalMaxReceive,
                    Math.max(0, ModConfig.portalEnergyCapacity - energyStored)));
            if (!simulate && received > 0) { energyStored += received; markDirty(); }
            return received;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energyStored; }
        @Override public int getMaxEnergyStored() { return ModConfig.portalEnergyCapacity; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public int getEnergyStored() { return energyStored; }
    public boolean canPayActivation() { return !ModConfig.requirePortalEnergy || energyStored >= ModConfig.portalActivationCost; }
    public void payActivation() {
        if (ModConfig.requirePortalEnergy) {
            energyStored = Math.max(0, energyStored - ModConfig.portalActivationCost);
            markDirty();
        }
    }
    @Override public void update() {
        if (world == null || world.isRemote || !active || !ModConfig.requirePortalEnergy || generatedDestination) return;
        int cost = ModConfig.portalEnergyPerTick;
        if (energyStored < cost) {
            PortalManager.deactivateByFrameBreak(world, pos);
        } else if (cost > 0) {
            energyStored -= cost;
            if (world.getTotalWorldTime() % 20 == 0) markDirty();
        }
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }
    @Nullable @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(energy);
        return super.getCapability(capability, facing);
    }

    private boolean active;
    private UUID id;
    private final Set<BlockPos> interior = new HashSet<>();
    private final Set<BlockPos> frame = new HashSet<>();
    private int linkedDim = NO_DIM;
    private BlockPos linkedPos;

    public boolean isActive() {
        return active;
    }

    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
            markDirty();
        }
        return id;
    }

    public Set<BlockPos> getInterior() {
        return Collections.unmodifiableSet(interior);
    }

    public Set<BlockPos> getFrame() {
        return Collections.unmodifiableSet(frame);
    }

    public boolean hasLink() {
        return linkedDim != NO_DIM && linkedPos != null;
    }

    public int getLinkedDim() {
        return linkedDim;
    }

    @Nullable
    public BlockPos getLinkedPos() {
        return linkedPos;
    }

    public void setLink(int dim, BlockPos pos) {
        this.linkedDim = dim;
        this.linkedPos = pos;
        markDirty();
    }

    public void clearLink() {
        this.linkedDim = NO_DIM;
        this.linkedPos = null;
        markDirty();
    }

    public void activate(Set<BlockPos> frameBlocks, Set<BlockPos> interiorBlocks) {
        unindex();
        this.active = true;
        this.frame.clear();
        this.frame.addAll(frameBlocks);
        this.interior.clear();
        this.interior.addAll(interiorBlocks);
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        index();
        markDirty();
    }

    public void deactivate() {
        unindex();
        this.active = false;
        this.frame.clear();
        this.interior.clear();
        clearLink();
        markDirty();
    }

    private void index() {
        if (world != null && !world.isRemote && active) {
            PortalIndex.register(world.provider.getDimension(), pos, interior, frame);
        }
    }

    private void unindex() {
        if (world != null && !world.isRemote) {
            PortalIndex.unregister(world.provider.getDimension(), pos, interior, frame);
        }
    }

    @Override
    public void validate() {
        super.validate();
        
        index();
    }

    @Override
    public void invalidate() {
        unindex();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        unindex();
        super.onChunkUnload();
    }

    public boolean isInInterior(BlockPos pos) {
        return interior.contains(pos);
    }

    public boolean isInFrame(BlockPos pos) {
        return frame.contains(pos);
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos,
                                 net.minecraft.block.state.IBlockState oldState,
                                 net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("active", active);
        compound.setInteger("energyStored", energyStored);
        compound.setBoolean("generatedDestination", generatedDestination);
        if (id != null) {
            compound.setUniqueId("id", id);
        }
        compound.setTag("interior", writePosList(interior));
        compound.setTag("frame", writePosList(frame));
        if (hasLink()) {
            compound.setInteger("linkedDim", linkedDim);
            compound.setLong("linkedPos", linkedPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        active = compound.getBoolean("active");
        energyStored = Math.max(0, Math.min(ModConfig.portalEnergyCapacity, compound.getInteger("energyStored")));
        generatedDestination = compound.getBoolean("generatedDestination");
        if (compound.hasUniqueId("id")) {
            id = compound.getUniqueId("id");
        }
        readPosList(compound, "interior", interior);
        readPosList(compound, "frame", frame);
        if (compound.hasKey("linkedDim") && compound.hasKey("linkedPos")) {
            linkedDim = compound.getInteger("linkedDim");
            linkedPos = BlockPos.fromLong(compound.getLong("linkedPos"));
        } else {
            linkedDim = NO_DIM;
            linkedPos = null;
        }
    }

    private static NBTTagList writePosList(Set<BlockPos> set) {
        NBTTagList list = new NBTTagList();
        for (BlockPos pos : set) {
            list.appendTag(new NBTTagLong(pos.toLong()));
        }
        return list;
    }

    private static void readPosList(NBTTagCompound compound, String key, Set<BlockPos> target) {
        target.clear();
        NBTTagList longs = compound.getTagList(key, Constants.NBT.TAG_LONG);
        if (longs.tagCount() > 0) {
            for (int i = 0; i < longs.tagCount(); i++) {
                target.add(BlockPos.fromLong(((NBTTagLong) longs.get(i)).getLong()));
            }
            return;
        }
        
        NBTTagList legacy = compound.getTagList(key, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < legacy.tagCount(); i++) {
            target.add(BlockPos.fromLong(legacy.getCompoundTagAt(i).getLong("p")));
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return super.writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return null;
    }
}
