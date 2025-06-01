package com.ded.bnp;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import com.ded.bnp.config.ModConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class BlockPortalCore extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", Arrays.asList(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST));
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockPortalCore() {
        super(Material.ROCK);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ACTIVE, false));
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        switch (state.getValue(FACING)) {
            case NORTH: meta = 0; break;
            case SOUTH: meta = 1; break;
            case EAST:  meta = 2; break;
            case WEST:  meta = 3; break;
            default:    meta = 0; break;
        }
        if (state.getValue(ACTIVE)) {
            meta |= 4;
        }
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing;
        switch (meta & 3) {
            case 0: facing = EnumFacing.NORTH; break;
            case 1: facing = EnumFacing.SOUTH; break;
            case 2: facing = EnumFacing.EAST; break;
            default: facing = EnumFacing.WEST; break;
        }
        boolean active = (meta & 4) != 0;
        return this.getDefaultState().withProperty(FACING, facing).withProperty(ACTIVE, active);
    }

    // Устанавливаем направление в зависимости от того, куда смотрит игрок
    // Блок будет повернут лицевой стороной К игроку
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        EnumFacing direction = state.getValue(FACING);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityPortalCore) {
            ((TileEntityPortalCore) tile).setFacing(direction);
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityPortalCore();
    }
    
    /**
     * Вызывается при разрушении блока
     * Различает разрушение игроком и системой
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityPortalCore) {
                TileEntityPortalCore core = (TileEntityPortalCore) te;
                
                // Если ядро было активным и разрушено игроком, выпадает предмет активации
                if (core.isActive() && core.isBrokenByPlayer() && !core.getActivationItem().isEmpty()) {
                    EntityItem entityItem = new EntityItem(
                        world, 
                        pos.getX() + 0.5, 
                        pos.getY() + 0.5, 
                        pos.getZ() + 0.5, 
                        core.getActivationItem().copy()
                    );
                    world.spawnEntity(entityItem);
                    
                    // Очищаем предмет активации, чтобы избежать повторного дропа
                    core.setActivationItem(ItemStack.EMPTY);
                }
                
                // Разрушаем блоки портала на своей стороне
                core.breakPortal();
                
                // Всегда разрушаем противоположную сторону полностью
                core.breakLinkedPortalCompletely();
            }
        }
        
        super.breakBlock(world, pos, state);
    }
    
    /**
     * Вызывается при добыче блока игроком
     * Устанавливает флаг разрушения игроком
     */
    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityPortalCore) {
                ((TileEntityPortalCore) te).setBrokenByPlayer(true);
            }
        }
        
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && stack.getItem() == ModConfig.getPortalActivatorItem() && !state.getValue(ACTIVE)) {
            EnumFacing portalFacing = checkFrame(world, pos);
            if (portalFacing != null) {
                // Store the activation item for potential drop when core is broken
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof TileEntityPortalCore) {
                    ((TileEntityPortalCore) te).setActivationItem(stack.copy());
                }
                
                // Activate the portal
                activatePortal(world, pos, state, portalFacing, player, hand);
                
                // Consume the activation item (remove from player's hand/inventory)
                if (!player.capabilities.isCreativeMode) {
                    stack.shrink(1);
                }
                
                return true;
            }
        }
        return false;
    }

    private EnumFacing checkFrame(World world, BlockPos pos) {
        // Проверяем все 4 направления
        for (EnumFacing facing : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
            boolean valid = true;

            // Получаем ось из направления
            EnumFacing.Axis axis = facing.getAxis();

            // Проверяем рамку
            for (int i = -2; i <= 2; i++) {
                for (int dy = -1; dy <= 3; dy++) {
                    if ((i >= -1 && i <= 1 && dy >= 0 && dy <= 2) || (i == 0 && dy == -1)) continue;

                    BlockPos framePos;
                    if (axis == EnumFacing.Axis.Z) {
                        framePos = new BlockPos(pos.getX() + i, pos.getY() + dy + 1, pos.getZ());
                    } else {
                        framePos = new BlockPos(pos.getX(), pos.getY() + dy + 1, pos.getZ() + i);
                    }

                    if (world.getBlockState(framePos).getBlock() != Blocks.OBSIDIAN) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) break;
            }

            if (valid) {

                return facing;
            }
        }
        return null;
    }
    @SideOnly(Side.CLIENT)
    public void initModel() {
        FMLLog.log(Level.INFO, "[BNP] Инициализация модели для PortalCore");
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0,
                new ModelResourceLocation(getRegistryName(), "inventory"));
    }
    private void activatePortal(World world, BlockPos pos, IBlockState state, EnumFacing facing, EntityPlayer player, EnumHand hand) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityPortalCore)) {
            return;
        }

        TileEntityPortalCore core = (TileEntityPortalCore) te;
        UUID portalId = UUID.randomUUID();
        core.setPortalId(portalId);
        core.setActive(true);
        core.setFacing(facing);

        // Сохраняем текущее направление блока при активации
        EnumFacing blockFacing = state.getValue(FACING);
        world.setBlockState(pos, state.withProperty(ACTIVE, true).withProperty(FACING, blockFacing), 3);
        spawnPortalBlocks(world, pos, facing, portalId);

        // Удалено повреждение предмета, так как теперь предмет полностью потребляется в onBlockActivated

        createLinkedPortal(world, pos, facing, portalId);
    }

    private void spawnPortalBlocks(World world, BlockPos pos, EnumFacing facing, UUID portalId) {

        EnumFacing.Axis axis = facing.getAxis();

        // Создаем все блоки портала внутри рамки 5x5
        for (int i = -1; i <= 1; i++) {
            for (int dy = 1; dy <= 3; dy++) {
                BlockPos portalPos = axis == EnumFacing.Axis.Z ?
                        new BlockPos(pos.getX() + i, pos.getY() + dy, pos.getZ()) :
                        new BlockPos(pos.getX(), pos.getY() + dy, pos.getZ() + i);

                // Принудительно заменяем блоки на портал
                world.setBlockState(portalPos, ModBlocks.CustomPortal.getDefaultState()
                        .withProperty(BlockCustomPortal.FACING, facing), 3);

                TileEntity te = world.getTileEntity(portalPos);
                if (te instanceof TileEntityCustomPortal) {
                    TileEntityCustomPortal portalTE = (TileEntityCustomPortal) te;
                    portalTE.setPortalId(portalId);
                    portalTE.setCorePos(pos);

                }

                // Регистрируем блок портала в ядре
                TileEntity coreTE = world.getTileEntity(pos);
                if (coreTE instanceof TileEntityPortalCore) {
                    ((TileEntityPortalCore) coreTE).registerPortalBlock(portalPos);
                }
            }
        }
    }

    private void createLinkedPortal(World world, BlockPos pos, EnumFacing facing, UUID portalId) {

        try {
            int targetDimension = world.provider.getDimension() == 0 ? -1 : 0;

            WorldServer targetWorld = world.getMinecraftServer().getWorld(targetDimension);
            if (targetWorld == null) {

                return;
            }
            BlockPos targetPos = calculateTargetPosition(world, pos);


            targetPos = findSafeLocation(targetWorld, targetPos, facing);


            buildPortal(targetWorld, targetPos, facing);


            // Устанавливаем состояние блока ядра как активное, сохраняя направление
            targetWorld.setBlockState(targetPos, this.getDefaultState()
                    .withProperty(FACING, facing)
                    .withProperty(ACTIVE, true), 3);

            // Настраиваем ядро второго портала
            TileEntity te = targetWorld.getTileEntity(targetPos);
            if (te instanceof TileEntityPortalCore) {
                TileEntityPortalCore targetCore = (TileEntityPortalCore) te;
                targetCore.setActive(true);
                targetCore.setFacing(facing);
                targetCore.setNetherSide(world.provider.getDimension() != 0);
                targetCore.setLinkedPortal(pos, world.provider.getDimension());
                targetCore.setPortalId(portalId);

                // Связываем исходное ядро с целевым
                TileEntity sourceTE = world.getTileEntity(pos);
                if (sourceTE instanceof TileEntityPortalCore) {
                    ((TileEntityPortalCore) sourceTE).setLinkedPortal(targetPos, targetDimension);
                }

                // Спавним блоки портала
                spawnPortalBlocks(targetWorld, targetPos, facing, portalId);


                // Регистрируем блоки рамки в ядре
                registerFrameBlocks(targetWorld, targetPos, facing, targetCore);

                // Обновляем состояние блоков
                targetWorld.notifyBlockUpdate(targetPos, targetWorld.getBlockState(targetPos), targetWorld.getBlockState(targetPos), 3);
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);

            }
        } catch (Exception e) {
        }
    }

    // Регистрируем блоки рамки в ядре для последующего удаления
    private void registerFrameBlocks(World world, BlockPos pos, EnumFacing facing, TileEntityPortalCore core) {
        EnumFacing.Axis axis = facing.getAxis();

        for (int i = -2; i <= 2; i++) {
            for (int dy = -1; dy <= 3; dy++) {
                if ((i >= -1 && i <= 1 && dy >= 0 && dy <= 2) || (i == 0 && dy == -1)) continue;
                BlockPos framePos = axis == EnumFacing.Axis.Z ?
                        new BlockPos(pos.getX() + i, pos.getY() + dy + 1, pos.getZ()) :
                        new BlockPos(pos.getX(), pos.getY() + dy + 1, pos.getZ() + i);
                core.registerFrameBlock(framePos);
            }
        }
    }

    private BlockPos calculateTargetPosition(World world, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (world.provider.getDimension() == 0) {
            x = x / 8;
            z = z / 8;
        } else {
            x = x * 8;
            z = z * 8;
        }
        return new BlockPos(x, y, z);
    }

    private BlockPos findSafeLocation(World world, BlockPos pos, EnumFacing facing) {
        int x = pos.getX();
        int y = Math.max(20, Math.min(world.getHeight() - 20, pos.getY()));
        int z = pos.getZ();

        // Проверка на крышу Ада
        if (world.provider.getDimension() == -1 && y > 120) {
            y = 100; // Перемещаем ниже крыши Ада
        }

        // Оптимизированный радиус поиска
        int searchRadius = 32; // Увеличиваем радиус поиска
        int verticalRadius = 16; // Увеличиваем вертикальный радиус

        // Сначала проверяем исходную позицию
        if (isValidPosition(world, new BlockPos(x, y, z), facing)) {
            return new BlockPos(x, y, z);
        }

        // Спиральный поиск для более эффективного нахождения позиции
        for (int r = 1; r <= searchRadius; r++) {
            // Проверяем по спирали на текущем уровне
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Проверяем только периметр текущего квадрата спирали
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    // Проверяем на разных высотах
                    for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                        // Избегаем крыши Ада
                        if (world.provider.getDimension() == -1 && y + dy > 120) {
                            continue;
                        }

                        BlockPos checkPos = new BlockPos(x + dx, y + dy, z + dz);
                        if (isValidPosition(world, checkPos, facing)) {
                            return checkPos;
                        }
                    }
                }
            }
        }

        // Если не нашли подходящую позицию, пробуем создать платформу
        BlockPos fallbackPos = new BlockPos(x, y, z);

        createSafePlatform(world, fallbackPos);
        return fallbackPos;
    }

    private void createSafePlatform(World world, BlockPos pos) {
        // Создаем платформу из обсидиана
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos platformPos = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                world.setBlockState(platformPos, Blocks.OBSIDIAN.getDefaultState(), 3);
            }
        }

        // Очищаем пространство над платформой
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos airPos = new BlockPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    world.setBlockToAir(airPos);
                }
            }
        }
    }

    private boolean isValidPosition(World world, BlockPos pos, EnumFacing facing) {
        EnumFacing.Axis axis = facing.getAxis();

        // Проверка на крышу Ада
        if (world.provider.getDimension() == -1 && pos.getY() > 120) {
            return false;
        }

        // Проверяем, что под порталом есть твердая поверхность
        boolean hasSolidGround = true;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos groundPos = new BlockPos(pos.getX() + dx, pos.getY() - 1, pos.getZ() + dz);
                if (!world.getBlockState(groundPos).getMaterial().isSolid()) {
                    hasSolidGround = false;
                    break;
                }
            }
            if (!hasSolidGround) break;
        }

        if (!hasSolidGround) {
            return false;
        }

        // Проверяем, что в области портала почти нет блоков (ванильное поведение)
        // Считаем количество непустых блоков в области портала
        int nonAirBlockCount = 0;
        int totalPortalBlocks = 9; // 3x3 блока портала

        // Проверяем только область, где будет сам портал (без рамки)
        for (int i = -1; i <= 1; i++) {
            for (int dy = 1; dy <= 3; dy++) {
                BlockPos portalPos = axis == EnumFacing.Axis.Z ?
                        new BlockPos(pos.getX() + i, pos.getY() + dy, pos.getZ()) :
                        new BlockPos(pos.getX(), pos.getY() + dy, pos.getZ() + i);

                if (!world.isAirBlock(portalPos)) {
                    nonAirBlockCount++;
                }
            }
        }

        // Если более 20% блоков в области портала - не воздух, считаем позицию невалидной
        float nonAirRatio = (float)nonAirBlockCount / totalPortalBlocks;
        if (nonAirRatio > 0.2f) {
            return false;
        }

        return true;
    }

    private void buildPortal(World world, BlockPos pos, EnumFacing facing) {
        EnumFacing.Axis axis = facing.getAxis();

        // Строим полную рамку 5x5 из обсидиана

        // 1. Строим основание (нижняя часть рамки)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos basePos = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                world.setBlockState(basePos, Blocks.OBSIDIAN.getDefaultState(), 3);
            }
        }

        // 2. Строим боковые стороны рамки
        for (int dy = 1; dy <= 4; dy++) {
            // Левая и правая стороны рамки
            for (int i = -2; i <= 2; i += 4) {
                BlockPos sidePos;
                if (axis == EnumFacing.Axis.Z) {
                    sidePos = new BlockPos(pos.getX() + i/2, pos.getY() + dy, pos.getZ());
                } else {
                    sidePos = new BlockPos(pos.getX(), pos.getY() + dy, pos.getZ() + i/2);
                }
                world.setBlockState(sidePos, Blocks.OBSIDIAN.getDefaultState(), 3);
            }

            // Передняя и задняя стороны рамки (только для крайних блоков, не трогаем середину)
            for (int i = -2; i <= 2; i++) {
                // Пропускаем центральные блоки (они будут порталом)
                if (i >= -1 && i <= 1 && dy <= 3) continue;

                BlockPos framePos;
                if (axis == EnumFacing.Axis.Z) {
                    framePos = new BlockPos(pos.getX() + i, pos.getY() + dy, pos.getZ());
                } else {
                    framePos = new BlockPos(pos.getX(), pos.getY() + dy, pos.getZ() + i);
                }
                world.setBlockState(framePos, Blocks.OBSIDIAN.getDefaultState(), 3);
            }
        }

        // 3. Строим верхнюю часть рамки (все 5 блоков)
        for (int i = -2; i <= 2; i++) {
            BlockPos topPos;
            if (axis == EnumFacing.Axis.Z) {
                topPos = new BlockPos(pos.getX() + i, pos.getY() + 4, pos.getZ());
            } else {
                topPos = new BlockPos(pos.getX(), pos.getY() + 4, pos.getZ() + i);
            }
            world.setBlockState(topPos, Blocks.OBSIDIAN.getDefaultState(), 3);
        }

        // 4. Строим основание ядра
        BlockPos basePos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        world.setBlockState(basePos, Blocks.OBSIDIAN.getDefaultState(), 3);
    }
}
