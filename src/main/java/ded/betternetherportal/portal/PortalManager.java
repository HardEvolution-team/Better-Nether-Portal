package ded.betternetherportal.portal;

import ded.betternetherportal.BetterNetherPortal;
import ded.betternetherportal.block.BetterPortalBlock;
import ded.betternetherportal.block.PortalCoreBlock;
import ded.betternetherportal.config.ModConfig;
import ded.betternetherportal.tile.PortalCoreTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PortalManager {

    private PortalManager() {}

    private static final int MAX_INTERIOR = 256;
    private static final int NETHER = -1;
    private static final int OVERWORLD = 0;

    private static final Set<Long> cascadeGuard = new HashSet<>();

    public static boolean tryActivate(World world, BlockPos corePos, @Nullable EntityPlayer player) {
        FrameResult frame = detectFrame(world, corePos);
        if (frame == null) {
            return false;
        }

        PortalCoreTileEntity core = getCore(world, corePos);
        if (core == null || !core.canPayActivation()) return false;
        fillPortal(world, frame);
        core.payActivation();
        core.activate(frame.frame, frame.interior);
        world.setBlockState(corePos,
                world.getBlockState(corePos)
                        .withProperty(PortalCoreBlock.ACTIVE, true)
                        .withProperty(PortalCoreBlock.FACING, facingForAxis(frame.axis, player)), 3);

        buildLinkedPortal(world, corePos, core);
        return true;
    }

    private static EnumFacing facingForAxis(EnumFacing.Axis axis, @Nullable EntityPlayer player) {
        if (axis == EnumFacing.Axis.X) {
            if (player != null) {
                EnumFacing h = player.getHorizontalFacing();
                if (h.getAxis() == EnumFacing.Axis.Z) {
                    return h;
                }
            }
            return EnumFacing.NORTH;
        } else {
            if (player != null) {
                EnumFacing h = player.getHorizontalFacing();
                if (h.getAxis() == EnumFacing.Axis.X) {
                    return h;
                }
            }
            return EnumFacing.EAST;
        }
    }

    private static void fillPortal(World world, FrameResult frame) {
        IBlockState portalState = BetterNetherPortal.betterPortalBlock.getDefaultState()
                .withProperty(BetterPortalBlock.AXIS, frame.axis);
        for (BlockPos pos : frame.interior) {
            world.setBlockState(pos, portalState, 2);
        }
    }

    private static final class FrameResult {
        final Set<BlockPos> frame;
        final Set<BlockPos> interior;
        final EnumFacing.Axis axis;

        FrameResult(Set<BlockPos> frame, Set<BlockPos> interior, EnumFacing.Axis axis) {
            this.frame = frame;
            this.interior = interior;
            this.axis = axis;
        }
    }

    @Nullable
    private static FrameResult detectFrame(World world, BlockPos corePos) {
        if (ModConfig.strict5x5Frame) {
            FrameResult r = detectStrict5x5(world, corePos, EnumFacing.Axis.X);
            if (r == null) {
                r = detectStrict5x5(world, corePos, EnumFacing.Axis.Z);
            }
            return r;
        }
        FrameResult r = floodFill(world, corePos, EnumFacing.Axis.X);
        if (r == null) {
            r = floodFill(world, corePos, EnumFacing.Axis.Z);
        }
        return r;
    }

    @Nullable
    private static FrameResult floodFill(World world, BlockPos corePos, EnumFacing.Axis axis) {
        EnumFacing.Axis planeAxis = axis;
        EnumFacing[] dirs = planeAxis == EnumFacing.Axis.X ? DIRS_X : DIRS_Z;
        BlockPos start = corePos.up();
        if (!isReplaceable(world, start)) {
            return null;
        }

        Set<BlockPos> interior = new HashSet<>();
        Set<BlockPos> frame = new HashSet<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        interior.add(start);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (interior.size() > MAX_INTERIOR) {
                return null;
            }
            for (EnumFacing dir : dirs) {
                BlockPos next = cur.offset(dir);
                if (interior.contains(next)) {
                    continue;
                }
                if (isFrame(world, next) || next.equals(corePos)) {
                    frame.add(next);
                    continue;
                }
                if (isReplaceable(world, next)) {
                    interior.add(next);
                    queue.add(next);
                } else {
                    
                    return null;
                }
            }
        }

        if (interior.isEmpty()) {
            return null;
        }
        frame.add(corePos);
        return new FrameResult(frame, interior, planeAxis);
    }

    @Nullable
    private static FrameResult detectStrict5x5(World world, BlockPos corePos, EnumFacing.Axis axis) {
        EnumFacing along = (axis == EnumFacing.Axis.X) ? EnumFacing.EAST : EnumFacing.SOUTH;
        Set<BlockPos> frame = new HashSet<>();
        Set<BlockPos> interior = new HashSet<>();

        for (int a = -2; a <= 2; a++) {
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = corePos.offset(along, a).up(y);
                boolean border = (a == -2 || a == 2 || y == 0 || y == 4);
                if (pos.equals(corePos)) {
                    continue; 
                }
                if (border) {
                    if (!isFrame(world, pos)) {
                        return null;
                    }
                    frame.add(pos);
                } else {
                    if (!isReplaceable(world, pos)) {
                        return null;
                    }
                    interior.add(pos);
                }
            }
        }
        frame.add(corePos);
        return new FrameResult(frame, interior, axis);
    }

    private static final EnumFacing[] DIRS_X =
            {EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST, EnumFacing.WEST};
    private static final EnumFacing[] DIRS_Z =
            {EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH};

    private static boolean isFrame(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == ModConfig.getFrameBlock();
    }

    private static boolean isReplaceable(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return world.isAirBlock(pos) || state.getBlock().isReplaceable(world, pos)
                || state.getBlock() instanceof BetterPortalBlock;
    }

    private static void buildLinkedPortal(World world, BlockPos corePos, PortalCoreTileEntity core) {
        if (world.isRemote || world.getMinecraftServer() == null) {
            return;
        }
        int currentDim = world.provider.getDimension();
        int targetDim = (currentDim == NETHER) ? OVERWORLD : NETHER;

        WorldServer target = world.getMinecraftServer().getWorld(targetDim);
        if (target == null) {
            return;
        }

        BlockPos scaled = scaleForDimension(corePos, currentDim, targetDim);
        BlockPos targetCorePos = findBuildableSpot(target, scaled);

        PortalCoreTileEntity existing = getCore(target, targetCorePos);
        if (existing != null && existing.isActive()) {
            cross(core, currentDim, corePos, existing, targetDim, targetCorePos);
            return;
        }

        build5x5(target, targetCorePos);
        PortalCoreTileEntity targetCore = getCore(target, targetCorePos);
        if (targetCore == null) {
            return;
        }
        targetCore.setGeneratedDestination(true);
        cross(core, currentDim, corePos, targetCore, targetDim, targetCorePos);
    }

    private static void cross(PortalCoreTileEntity a, int dimA, BlockPos posA,
                              PortalCoreTileEntity b, int dimB, BlockPos posB) {
        a.setLink(dimB, posB);
        b.setLink(dimA, posA);
    }

    private static void build5x5(WorldServer world, BlockPos corePos) {
        Block frameBlock = ModConfig.getFrameBlock();
        EnumFacing along = EnumFacing.EAST;

        Set<BlockPos> frame = new HashSet<>();
        Set<BlockPos> interior = new HashSet<>();

        for (int a = -2; a <= 2; a++) {
            for (int y = 0; y <= 4; y++) {
                BlockPos pos = corePos.offset(along, a).up(y);
                boolean border = (a == -2 || a == 2 || y == 0 || y == 4);
                if (pos.equals(corePos)) {
                    continue;
                }
                if (border) {
                    world.setBlockState(pos, frameBlock.getDefaultState(), 2);
                    frame.add(pos);
                } else {
                    interior.add(pos);
                }
            }
        }

        world.setBlockState(corePos, BetterNetherPortal.portalCoreBlock.getDefaultState()
                .withProperty(PortalCoreBlock.ACTIVE, false), 2);
        frame.add(corePos);

        IBlockState portalState = BetterNetherPortal.betterPortalBlock.getDefaultState()
                .withProperty(BetterPortalBlock.AXIS, EnumFacing.Axis.X);
        for (BlockPos pos : interior) {
            world.setBlockState(pos, portalState, 2);
        }

        PortalCoreTileEntity core = getCore(world, corePos);
        if (core != null) {
            core.activate(frame, interior);
            world.setBlockState(corePos, world.getBlockState(corePos)
                    .withProperty(PortalCoreBlock.ACTIVE, true)
                    .withProperty(PortalCoreBlock.FACING, EnumFacing.NORTH), 3);
        }
    }

    private static BlockPos scaleForDimension(BlockPos pos, int fromDim, int toDim) {
        int scale = ModConfig.netherCoordinateScale;
        if (fromDim == OVERWORLD && toDim == NETHER) {
            return new BlockPos(pos.getX() / scale, clampY(pos.getY()), pos.getZ() / scale);
        }
        if (fromDim == NETHER && toDim == OVERWORLD) {
            return new BlockPos(pos.getX() * scale, clampY(pos.getY()), pos.getZ() * scale);
        }
        return new BlockPos(pos.getX(), clampY(pos.getY()), pos.getZ());
    }

    private static int clampY(int y) {
        return Math.max(10, Math.min(y, 240));
    }

    private static BlockPos findBuildableSpot(WorldServer world, BlockPos desired) {
        
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos p = desired.add(dx, dy, dz);
                    if (world.getBlockState(p).getBlock() instanceof PortalCoreBlock) {
                        return p;
                    }
                }
            }
        }
        
        for (int a = -3; a <= 3; a++) {
            for (int y = -1; y <= 5; y++) {
                for (int d = -1; d <= 1; d++) {
                    BlockPos p = desired.add(a, y, d);
                    boolean floor = (y == -1);
                    if (floor) {
                        if (world.isAirBlock(p)) {
                            world.setBlockState(p, ModConfig.getFrameBlock().getDefaultState(), 2);
                        }
                    } else {
                        if (!world.isAirBlock(p)) {
                            world.setBlockState(p, net.minecraft.init.Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }
        return desired;
    }

    public static void onEntityInPortal(World world, BlockPos portalPos, Entity entity) {
        if (!ModConfig.allowMobTeleport && !(entity instanceof EntityPlayer)) {
            return;
        }
        if (entity.timeUntilPortal > 0) {
            entity.timeUntilPortal = entity.getPortalCooldown();
            return;
        }

        PortalCoreTileEntity core = findOwningCore(world, portalPos, true);
        if (core == null || !core.hasLink()) {
            return;
        }
        if (world.getMinecraftServer() == null) {
            return;
        }

        int targetDim = core.getLinkedDim();
        BlockPos targetCorePos = core.getLinkedPos();
        
        WorldServer targetWorld = world.getMinecraftServer().getWorld(targetDim);
        if (targetWorld == null) {
            BetterNetherPortal.LOGGER.warn("Portal teleport aborted: target dimension {} unavailable", targetDim);
            return;
        }

        entity.timeUntilPortal = ModConfig.teleportCooldownTicks;

        BlockPos dest = destinationSpot(targetWorld, targetCorePos);
        PortalTeleporter teleporter = new PortalTeleporter(targetWorld, dest);

        playTravelEffects(world, portalPos);
        playTravelEffects(targetWorld, dest);

        if (entity.dimension == targetDim) {
            entity.setLocationAndAngles(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D,
                    entity.rotationYaw, entity.rotationPitch);
            entity.fallDistance = 0.0F;
            if (entity instanceof EntityPlayerMP) {
                ((EntityPlayerMP) entity).connection.setPlayerLocation(
                        dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D,
                        entity.rotationYaw, entity.rotationPitch);
            }
        } else {

            entity.changeDimension(targetDim, teleporter);
        }
    }

    private static void playTravelEffects(World world, BlockPos pos) {
        if (!ModConfig.playTeleportSound || !(world instanceof WorldServer)) {
            return;
        }
        ((WorldServer) world).playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.4F,
                world.rand.nextFloat() * 0.2F + 0.9F);
    }

    private static BlockPos destinationSpot(WorldServer world, BlockPos corePos) {
        IBlockState state = world.getBlockState(corePos);
        if (state.getBlock() instanceof PortalCoreBlock) {
            EnumFacing facing = state.getValue(PortalCoreBlock.FACING);
            BlockPos spot = corePos.up().offset(facing);
            if (world.isAirBlock(spot) || world.getBlockState(spot).getBlock() instanceof BetterPortalBlock) {
                ensureFloor(world, spot);
                return spot;
            }
        }
        return corePos.up();
    }

    private static void ensureFloor(WorldServer world, BlockPos spot) {
        BlockPos below = spot.down();
        if (world.isAirBlock(below) || world.getBlockState(below).getBlock().isReplaceable(world, below)) {
            world.setBlockState(below, ModConfig.getFrameBlock().getDefaultState(), 2);
        }
    }

    @Nullable
    public static PortalCoreTileEntity findOwningCore(World world, BlockPos pos, boolean interiorOwner) {
        BlockPos corePos = PortalIndex.find(world.provider.getDimension(), pos, interiorOwner);
        if (corePos == null) {
            return null;
        }
        if (!world.isBlockLoaded(corePos)) {
            return null;
        }
        PortalCoreTileEntity core = getCore(world, corePos);
        if (core == null || !core.isActive()) {
            return null;
        }
        boolean owns = interiorOwner ? core.isInInterior(pos) : core.isInFrame(pos);
        return owns ? core : null;
    }

    public static void deactivateByFrameBreak(World world, BlockPos corePos) {
        PortalCoreTileEntity core = getCore(world, corePos);
        if (core == null || !core.isActive()) {
            return;
        }
        removeInterior(world, core.getInterior());
        if (core.hasLink()) {
            destroyLinked(world, core.getLinkedDim(), core.getLinkedPos());
        }
        core.deactivate();
        IBlockState state = world.getBlockState(corePos);
        if (state.getBlock() instanceof PortalCoreBlock) {
            world.setBlockState(corePos, state.withProperty(PortalCoreBlock.ACTIVE, false), 3);
        }
    }

    public static void onCoreBroken(World world, BlockPos pos, IBlockState state) {
        long key = pos.toLong();
        boolean guarded = cascadeGuard.contains(key);
        PortalCoreTileEntity core = getCore(world, pos);

        boolean wasActive = state.getValue(PortalCoreBlock.ACTIVE);

        if (core != null) {
            removeInterior(world, core.getInterior());
        }

        if (!guarded) {
            if (wasActive) {
                dropStack(world, pos, new ItemStack(BetterNetherPortal.portalCoreItem));
                ItemStack activation = new ItemStack(ModConfig.getActivationItem());
                dropStack(world, pos, activation);
            } else {
                dropStack(world, pos, new ItemStack(BetterNetherPortal.portalCoreItem));
            }
        }

        if (!guarded && core != null && core.hasLink()) {
            destroyLinked(world, core.getLinkedDim(), core.getLinkedPos());
        }

        cascadeGuard.remove(key);
    }

    private static void destroyLinked(World world, int dim, BlockPos linkedPos) {
        if (world.getMinecraftServer() == null || linkedPos == null) {
            return;
        }
        WorldServer target = world.getMinecraftServer().getWorld(dim);
        if (target == null) {
            return;
        }
        if (!(target.getBlockState(linkedPos).getBlock() instanceof PortalCoreBlock)) {
            return;
        }
        cascadeGuard.add(linkedPos.toLong());

        target.setBlockToAir(linkedPos);
    }

    private static void removeInterior(World world, Set<BlockPos> interior) {
        for (BlockPos pos : interior) {
            if (world.getBlockState(pos).getBlock() instanceof BetterPortalBlock) {
                
                world.setBlockState(pos, net.minecraft.init.Blocks.AIR.getDefaultState(), 2);
            }
        }
    }

    private static void dropStack(World world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        EntityItem item = new EntityItem(world,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        item.setDefaultPickupDelay();
        world.spawnEntity(item);
    }

    @Nullable
    private static PortalCoreTileEntity getCore(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof PortalCoreTileEntity ? (PortalCoreTileEntity) te : null;
    }
}
