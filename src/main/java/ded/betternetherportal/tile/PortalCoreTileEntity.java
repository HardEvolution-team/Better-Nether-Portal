package ded.betternetherportal.tile;

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






public class PortalCoreTileEntity extends TileEntity {

    private static final int NO_DIM = Integer.MIN_VALUE;

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
