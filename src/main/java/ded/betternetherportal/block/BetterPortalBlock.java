package ded.betternetherportal.block;

import ded.betternetherportal.BetterNetherPortal;
import ded.betternetherportal.portal.PortalManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

public class BetterPortalBlock extends Block {

    public static final PropertyEnum<EnumFacing.Axis> AXIS =
            PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);

    protected static final AxisAlignedBB X_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D);
    protected static final AxisAlignedBB Z_AABB = new AxisAlignedBB(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D);

    public BetterPortalBlock() {
        super(Material.PORTAL);
        setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.X));
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setLightLevel(0.75F);
        setTranslationKey(BetterNetherPortal.MODID + ".better_portal");
        setRegistryName(BetterNetherPortal.MODID, "better_portal");
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AXIS, meta == 2 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS) == EnumFacing.Axis.Z ? 2 : 0;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(AXIS) == EnumFacing.Axis.Z ? Z_AABB : X_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && !entity.isRiding() && !entity.isBeingRidden() && entity.isNonBoss()) {
            PortalManager.onEntityInPortal(world, pos, entity);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        for (int i = 0; i < 4; ++i) {
            double x = pos.getX() + rand.nextFloat();
            double y = pos.getY() + rand.nextFloat();
            double z = pos.getZ() + rand.nextFloat();
            double sx = (rand.nextFloat() - 0.5D) * 0.5D;
            double sy = (rand.nextFloat() - 0.5D) * 0.5D;
            double sz = (rand.nextFloat() - 0.5D) * 0.5D;
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL, x, y, z, sx, sy, sz);
        }
    }

    @Override
    public net.minecraft.item.ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return net.minecraft.item.ItemStack.EMPTY;
    }
}
