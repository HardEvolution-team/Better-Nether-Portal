package com.ded.bnp;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;

import java.util.Random;
import java.util.UUID;

/**
 * Блок портала, через который происходит телепортация
 */
public class BlockCustomPortal extends Block {
    public static final PropertyEnum<EnumFacing> FACING = PropertyEnum.create("facing", EnumFacing.class,
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST);

    protected static final AxisAlignedBB NORTH_SOUTH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D);
    protected static final AxisAlignedBB EAST_WEST_AABB = new AxisAlignedBB(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D);
    protected static final AxisAlignedBB Y_AABB = new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);

    /**
     * Конструктор блока портала
     */
    public BlockCustomPortal() {
        super(Material.PORTAL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        setTickRandomly(true);
        setLightLevel(0.75F);
        setHardness(-1.0F);
        setResistance(6000000.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        TileEntityCustomPortal te = new TileEntityCustomPortal();
        te.setFacing(state.getValue(FACING));
        return te;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        switch (state.getValue(FACING)) {
            case NORTH: return 0;
            case SOUTH: return 1;
            case EAST:  return 2;
            case WEST:  return 3;
            default:    return 0;
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        switch (meta & 3) {
            case 0: return getDefaultState().withProperty(FACING, EnumFacing.NORTH);
            case 1: return getDefaultState().withProperty(FACING, EnumFacing.SOUTH);
            case 2: return getDefaultState().withProperty(FACING, EnumFacing.EAST);
            default: return getDefaultState().withProperty(FACING, EnumFacing.WEST);
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);

        // Синхронизируем направление с ядром портала
        if (!world.isRemote) {
            TileEntityPortalCore core = findPortalCore(world, pos, 5);
            if (core != null) {
                EnumFacing coreFacing = core.getFacing();
                if (coreFacing != null && state.getValue(FACING) != coreFacing) {
                    world.setBlockState(pos, state.withProperty(FACING, coreFacing), 3);

                    // Обновляем TileEntity
                    TileEntity te = world.getTileEntity(pos);
                    if (te instanceof TileEntityCustomPortal) {
                        ((TileEntityCustomPortal) te).setFacing(coreFacing);
                    }
                }
            }
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, net.minecraft.world.IBlockAccess source, BlockPos pos) {
        EnumFacing facing = state.getValue(FACING);
        if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) {
            return NORTH_SOUTH_AABB;
        } else if (facing == EnumFacing.EAST || facing == EnumFacing.WEST) {
            return EAST_WEST_AABB;
        } else {
            return Y_AABB;
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, net.minecraft.world.IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    /**
     * Проверяет, может ли сущность телепортироваться через портал
     *
     * @param entity Сущность для проверки
     * @return true, если сущность может телепортироваться
     */
    private boolean canEntityTeleport(Entity entity) {
        if (entity.world.isRemote) return false;
        if (entity.isRiding() || entity.isBeingRidden()) return false;
        if (!entity.isNonBoss()) return false;
        if (entity.timeUntilPortal > 0) return false;

        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            if (player.getActivePotionEffect(PotionEffectsCustom.PORTAL_COOLDOWN) != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Находит ядро портала в заданном радиусе
     *
     * @param world Мир
     * @param pos Позиция для поиска
     * @param radius Радиус поиска
     * @return Найденное ядро портала или null
     */
    public TileEntityPortalCore findPortalCore(World world, BlockPos pos, int radius) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCustomPortal) {
            TileEntityCustomPortal portalTE = (TileEntityCustomPortal) te;
            BlockPos corePos = portalTE.getCorePos();
            if (corePos != null) {
                TileEntity coreTe = world.getTileEntity(corePos);
                if (coreTe instanceof TileEntityPortalCore) {
                    return (TileEntityPortalCore) coreTe;
                }
            }
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    TileEntity checkTe = world.getTileEntity(checkPos);
                    if (checkTe instanceof TileEntityPortalCore) {
                        TileEntityPortalCore core = (TileEntityPortalCore) checkTe;

                        if (te instanceof TileEntityCustomPortal) {
                            ((TileEntityCustomPortal) te).setCorePos(checkPos);
                            ((TileEntityCustomPortal) te).setPortalId(core.getPortalId());
                            ((TileEntityCustomPortal) te).setFacing(core.getFacing());

                            // Обновляем состояние блока, если направление отличается
                            IBlockState state = world.getBlockState(pos);
                            if (state.getValue(FACING) != core.getFacing()) {
                                world.setBlockState(pos, state.withProperty(FACING, core.getFacing()), 3);
                            }
                        }
                        return core;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (canEntityTeleport(entityIn)) {
            handlePortalTeleport(worldIn, pos, entityIn);
        }
    }

    /**
     * Обрабатывает телепортацию сущности через портал
     *
     * @param world Мир
     * @param pos Позиция портала
     * @param entity Телепортируемая сущность
     */
    private void handlePortalTeleport(World world, BlockPos pos, Entity entity) {
        if (entity.timeUntilPortal > 0) {
            return;
        }

        if (entity instanceof EntityPlayerMP) {
            // Принудительно загружаем чанк с порталом
            world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        }

        TileEntity portalTE = world.getTileEntity(pos);
        if (!(portalTE instanceof TileEntityCustomPortal)) {
            FMLLog.log(Level.ERROR, "[BNP] TileEntity портала не найден или имеет неверный тип");
            return;
        }

        TileEntityCustomPortal customPortal = (TileEntityCustomPortal) portalTE;
        UUID portalId = customPortal.getPortalId();

        // Улучшенная логика поиска и обновления UUID
        if (portalId == null) {
            FMLLog.log(Level.WARN, "[BNP] UUID портала не установлен, пытаемся найти ядро");
            TileEntityPortalCore core = findPortalCore(world, pos, 5);
            if (core != null && core.isActive()) {
                portalId = core.getPortalId();
                if (portalId != null) {
                    FMLLog.log(Level.INFO, "[BNP] Найден UUID ядра: %s", portalId.toString());
                    customPortal.setPortalId(portalId);
                    customPortal.setCorePos(core.getPos());
                    customPortal.setFacing(core.getFacing());
                } else {
                    FMLLog.log(Level.ERROR, "[BNP] UUID ядра не установлен");
                    return;
                }
            } else {
                FMLLog.log(Level.ERROR, "[BNP] Не удалось найти активное ядро портала");
                return;
            }
        }

        TileEntityPortalCore core = findPortalCore(world, pos, 5);
        if (core == null) {
            FMLLog.log(Level.ERROR, "[BNP] Не удалось найти ядро портала");
            return;
        }

        if (!core.isActive()) {
            FMLLog.log(Level.ERROR, "[BNP] Ядро портала неактивно");
            return;
        }

        if (core.getLinkedPortalPos() == null) {
            FMLLog.log(Level.ERROR, "[BNP] Позиция связанного портала не установлена");
            return;
        }

        int targetDimension = core.getLinkedDimension();
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            FMLLog.log(Level.ERROR, "[BNP] Не удалось получить доступ к серверу");
            return;
        }

        WorldServer targetWorld = server.getWorld(targetDimension);
        if (targetWorld == null) {
            FMLLog.log(Level.ERROR, "[BNP] Не удалось получить доступ к целевому измерению: %d", targetDimension);
            return;
        }

        BlockPos linkedPos = core.getLinkedPortalPos();
        FMLLog.log(Level.INFO, "[BNP] Позиция связанного портала: %s в измерении %d", linkedPos.toString(), targetDimension);

        // Проверяем, загружен ли чанк с целевым порталом
        if (!targetWorld.isBlockLoaded(linkedPos)) {
            FMLLog.log(Level.WARN, "[BNP] Чанк с целевым порталом не загружен, загружаем...");
            targetWorld.getChunkProvider().loadChunk(linkedPos.getX() >> 4, linkedPos.getZ() >> 4);

            // Ждем небольшую задержку для полной загрузки чанка
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Игнорируем прерывание
            }
        }

        TileEntity linkedTe = targetWorld.getTileEntity(linkedPos);
        if (!(linkedTe instanceof TileEntityPortalCore)) {
            FMLLog.log(Level.ERROR, "[BNP] TileEntity связанного ядра не найден или имеет неверный тип");
            return;
        }

        TileEntityPortalCore linkedCore = (TileEntityPortalCore) linkedTe;
        if (!linkedCore.isActive()) {
            FMLLog.log(Level.ERROR, "[BNP] Связанное ядро неактивно");
            return;
        }

        // Проверка и восстановление UUID, если они не совпадают
        if (!portalId.equals(linkedCore.getPortalId())) {
            FMLLog.log(Level.WARN, "[BNP] UUID порталов не совпадают, восстанавливаем связь");
            FMLLog.log(Level.INFO, "[BNP] Исходный UUID: %s, Целевой UUID: %s",
                       portalId.toString(),
                       linkedCore.getPortalId() != null ? linkedCore.getPortalId().toString() : "null");

            // Синхронизируем UUID между порталами
            linkedCore.setPortalId(portalId);
            return; // Пропускаем текущую попытку телепортации, чтобы дать время на синхронизацию
        }

        EnumFacing facing = linkedCore.getFacing();
        EnumFacing.Axis axis = facing.getAxis();

        final double xPos = linkedPos.getX() + 0.5 + (axis == EnumFacing.Axis.X ? 0.0 : 2.0);
        final double yPos = linkedPos.getY() + 1.5;
        final double zPos = linkedPos.getZ() + 0.5 + (axis == EnumFacing.Axis.X ? 2.0 : 0.0);

        entity.timeUntilPortal = 100;

        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            player.addPotionEffect(new PotionEffect(PotionEffectsCustom.PORTAL_COOLDOWN, 100, 0, false, false));

            FMLLog.log(Level.INFO, "[BNP] Телепортируем игрока %s в измерение %d на позицию [%.2f, %.2f, %.2f]",
                       player.getName(), targetDimension, xPos, yPos, zPos);

            player.changeDimension(targetDimension, new ITeleporter() {
                @Override
                public void placeEntity(World world, Entity entity, float yaw) {
                    entity.setLocationAndAngles(xPos, yPos, zPos, entity.rotationYaw, entity.rotationPitch);
                }
            });

            FMLLog.log(Level.INFO, "[BNP] Телепортация завершена");
        }

        world.playSound(null, pos, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.4F + 0.8F);
        targetWorld.playSound(null, linkedPos, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.BLOCKS, 0.5F, targetWorld.rand.nextFloat() * 0.4F + 0.8F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (rand.nextInt(100) == 0) {
            worldIn.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.5F, rand.nextFloat() * 0.4F + 0.8F, false);
        }

        for (int i = 0; i < 4; ++i) {
            double xPos = pos.getX() + rand.nextDouble();
            double yPos = pos.getY() + rand.nextDouble();
            double zPos = pos.getZ() + rand.nextDouble();
            double xSpeed = (rand.nextFloat() - 0.5D) * 0.5D;
            double ySpeed = (rand.nextFloat() - 0.5D) * 0.5D;
            double zSpeed = (rand.nextFloat() - 0.5D) * 0.5D;
            int j = rand.nextInt(2) * 2 - 1;

            EnumFacing facing = stateIn.getValue(FACING);
            if (facing == EnumFacing.EAST || facing == EnumFacing.WEST) {
                xPos = pos.getX() + 0.5D + 0.25D * j;
                xSpeed = rand.nextFloat() * 2.0F * j;
            } else {
                zPos = pos.getZ() + 0.5D + 0.25D * j;
                zSpeed = rand.nextFloat() * 2.0F * j;
            }

            worldIn.spawnParticle(EnumParticleTypes.PORTAL, xPos, yPos, zPos, xSpeed, ySpeed, zSpeed);
        }
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return ItemStack.EMPTY;
    }

    /**
     * Вызывается при разрушении блока портала
     * Если портал был активен, инициирует дроп предмета активации с ядра
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            // Находим ядро портала
            TileEntityPortalCore core = findPortalCore(world, pos, 5);
            if (core != null && core.isActive()) {
                // Если ядро активно, дропаем предмет активации
                ItemStack activationItem = core.getActivationItem();
                if (!activationItem.isEmpty()) {
                    EntityItem entityItem = new EntityItem(
                        world,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        activationItem.copy()
                    );
                    world.spawnEntity(entityItem);

                    // Очищаем предмет активации в ядре, чтобы избежать повторного дропа
                    core.setActivationItem(ItemStack.EMPTY);
                }

                // Деактивируем портал
                core.setActive(false);
                IBlockState coreState = world.getBlockState(core.getPos());
                if (coreState.getBlock() instanceof BlockPortalCore) {
                    world.setBlockState(core.getPos(), coreState.withProperty(BlockPortalCore.ACTIVE, false), 3);
                }

                // Разрушаем противоположную сторону портала
                core.breakLinkedPortalCompletely();
            }
        }

        super.breakBlock(world, pos, state);
    }
}
