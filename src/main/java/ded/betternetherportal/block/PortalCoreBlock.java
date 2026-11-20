package ded.betternetherportal.block;

import ded.betternetherportal.BetterNetherPortal;
import ded.betternetherportal.config.ModConfig;
import ded.betternetherportal.portal.PortalManager;
import ded.betternetherportal.tile.PortalCoreTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;






public class PortalCoreBlock extends Block {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    public PortalCoreBlock() {
        super(Material.ROCK);
        setRegistryName(BetterNetherPortal.MODID, "portal_core");
        setTranslationKey(BetterNetherPortal.MODID + ".portal_core");
        setHardness(50.0F);
        setResistance(2000.0F);
        setCreativeTab(BetterNetherPortal.CREATIVE_TAB);
        setHarvestLevel("pickaxe", 3);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(ACTIVE, false)
                .withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(pos);

        
        if (player.isSneaking() && te instanceof PortalCoreTileEntity) {
            PortalCoreTileEntity core = (PortalCoreTileEntity) te;
            if (core.hasLink()) {
                BlockPos link = core.getLinkedPos();
                String dimName = net.minecraft.world.DimensionType.getById(core.getLinkedDim()).getName();
                player.sendMessage(new TextComponentTranslation("message.betternetherportal.link_info",
                        dimName, link.getX(), link.getY(), link.getZ()));
            } else {
                player.sendMessage(new TextComponentTranslation("message.betternetherportal.not_linked"));
            }
            return true;
        }

        if (state.getValue(ACTIVE)) {
            player.sendMessage(new TextComponentTranslation("message.betternetherportal.already_active"));
            return true;
        }

        ItemStack held = player.getHeldItem(hand);
        Item activationItem = ModConfig.getActivationItem();
        if (held.getItem() != activationItem) {
            return false;
        }

        if (PortalManager.tryActivate(world, pos, player)) {
            if (!player.isCreative()) {
                held.shrink(1);
            }
            world.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_PORTAL_TRIGGER,
                    SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.4F + 0.8F);
        } else {
            player.sendMessage(new TextComponentTranslation("message.betternetherportal.no_frame"));
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            PortalManager.onCoreBroken(world, pos, state);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        
        
        
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
        boolean active = (meta & 4) > 0;
        return getDefaultState().withProperty(FACING, facing).withProperty(ACTIVE, active);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(ACTIVE)) {
            meta |= 4;
        }
        return meta;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE, FACING);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new PortalCoreTileEntity();
    }
}
