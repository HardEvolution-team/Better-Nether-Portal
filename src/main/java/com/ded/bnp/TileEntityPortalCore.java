package com.ded.bnp;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TileEntityPortalCore extends TileEntity implements ITickable {
    private boolean isActive = false;
    private EnumFacing facing = EnumFacing.NORTH;
    private UUID portalId = null;
    private BlockPos linkedPortalPos = null;
    private int linkedDimension = 0;
    private boolean isNetherSide = false;
    private List<BlockPos> portalBlocks = new ArrayList<>();
    private List<BlockPos> frameBlocks = new ArrayList<>();
    private boolean beingDestroyed = false;
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;

    // Флаг для отслеживания, был ли портал разрушен игроком
    private boolean brokenByPlayer = false;

    // Хранение предмета активации для возможного выпадения при разрушении
    private ItemStack activationItem = ItemStack.EMPTY;

    public void setActive(boolean active) {
        this.isActive = active;
        markDirty();

        if (!world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    /**
     * Устанавливает флаг разрушения игроком
     * @param brokenByPlayer true если ядро разрушено игроком, false если системой
     */
    public void setBrokenByPlayer(boolean brokenByPlayer) {
        this.brokenByPlayer = brokenByPlayer;
        markDirty();
    }

    /**
     * Проверяет, было ли ядро разрушено игроком
     * @return true если ядро разрушено игроком, false если системой
     */
    public boolean isBrokenByPlayer() {
        return brokenByPlayer;
    }

    /**
     * Сохраняет предмет активации для возможного выпадения при разрушении
     * @param item Предмет, использованный для активации портала
     */
    public void setActivationItem(ItemStack item) {
        // Создаем копию предмета с количеством 1, чтобы избежать дюпа
        if (!item.isEmpty()) {
            ItemStack singleItem = item.copy();
            singleItem.setCount(1);
            this.activationItem = singleItem;
        } else {
            this.activationItem = ItemStack.EMPTY;
        }
        markDirty();
    }

    /**
     * Получает предмет активации
     * @return Предмет, использованный для активации портала
     */
    public ItemStack getActivationItem() {
        return activationItem;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setFacing(EnumFacing facing) {
        this.facing = facing;
        markDirty();
    }

    public EnumFacing getFacing() {
        return facing;
    }

    // Для обратной совместимости
    public EnumFacing.Axis getAxis() {
        return facing.getAxis();
    }

    // Для обратной совместимости
    public void setAxis(EnumFacing.Axis axis) {
        // Преобразуем ось в направление, сохраняя текущее направление если возможно
        if (axis == EnumFacing.Axis.Z) {
            if (facing != EnumFacing.NORTH && facing != EnumFacing.SOUTH) {
                this.facing = EnumFacing.NORTH;
            }
        } else if (axis == EnumFacing.Axis.X) {
            if (facing != EnumFacing.EAST && facing != EnumFacing.WEST) {
                this.facing = EnumFacing.EAST;
            }
        }
        markDirty();
    }

    public void setPortalId(UUID id) {
        this.portalId = id;
        markDirty();
    }

    public UUID getPortalId() {
        return portalId;
    }

    public void setLinkedPortal(BlockPos pos, int dimension) {
        this.linkedPortalPos = pos;
        this.linkedDimension = dimension;
        markDirty();
    }

    public BlockPos getLinkedPortalPos() {
        return linkedPortalPos;
    }

    public int getLinkedDimension() {
        return linkedDimension;
    }

    public void setNetherSide(boolean netherSide) {
        this.isNetherSide = netherSide;
        markDirty();
    }

    public boolean isNetherSide() {
        return isNetherSide;
    }

    public void registerPortalBlock(BlockPos pos) {
        if (!portalBlocks.contains(pos)) {
            portalBlocks.add(pos);
            markDirty();
        }
    }

    public void registerFrameBlock(BlockPos pos) {
        if (!frameBlocks.contains(pos)) {
            frameBlocks.add(pos);
            markDirty();
        }
    }

    public List<BlockPos> getPortalBlocks() {
        return new ArrayList<>(portalBlocks);
    }

    public List<BlockPos> getFrameBlocks() {
        return new ArrayList<>(frameBlocks);
    }

    public void setBeingDestroyed(boolean beingDestroyed) {
        this.beingDestroyed = beingDestroyed;
    }

    @Override
    public void update() {
        if (!world.isRemote && isActive && !beingDestroyed) {
            tickCounter++;
            if (tickCounter >= CHECK_INTERVAL) {
                checkPortalIntegrity();
                tickCounter = 0;
            }
        }
    }

    /**
     * Проверяет целостность портала и разрушает его при нарушении
     */
    public void checkPortalIntegrity() {
        if (!isActive || world.isRemote || beingDestroyed) {
            return;
        }

        // Проверяем только если чанк загружен (стандартное поведение Minecraft)
        if (!world.isBlockLoaded(pos)) {
            return;
        }

        EnumFacing.Axis axis = facing.getAxis();
        boolean isValid = true;
        String invalidReason = "";

        // Проверка рамки
        for (int i = -2; i <= 2; i++) {
            for (int dy = -1; dy <= 3; dy++) {
                if ((i >= -1 && i <= 1 && dy >= 0 && dy <= 2) || (i == 0 && dy == -1)) {
                    continue;
                }
                BlockPos framePos = axis == EnumFacing.Axis.Z ?
                        new BlockPos(pos.getX() + i, pos.getY() + dy + 1, pos.getZ()) :
                        new BlockPos(pos.getX(), pos.getY() + dy + 1, pos.getZ() + i);
                if (world.getBlockState(framePos).getBlock() != Blocks.OBSIDIAN) {
                    isValid = false;
                    invalidReason = "Отсутствует блок обсидиана в рамке на позиции " + framePos.toString();
                    break;
                }
            }
            if (!isValid) {
                break;
            }
        }

        // Проверка блоков портала
        if (isValid) {
            for (int i = -1; i <= 1; i++) {
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos portalPos = axis == EnumFacing.Axis.Z ?
                            new BlockPos(pos.getX() + i, pos.getY() + dy, pos.getZ()) :
                            new BlockPos(pos.getX(), pos.getY() + dy, pos.getZ() + i);
                    if (world.getBlockState(portalPos).getBlock() != ModBlocks.CustomPortal) {
                        isValid = false;
                        invalidReason = "Отсутствует блок портала на позиции " + portalPos.toString();
                        break;
                    }
                }
                if (!isValid) {
                    break;
                }
            }
        }

        // Проверка связанного портала
        if (isValid && linkedPortalPos != null) {
            WorldServer linkedWorld = world.getMinecraftServer().getWorld(linkedDimension);
            if (linkedWorld != null && linkedWorld.isBlockLoaded(linkedPortalPos)) {
                TileEntity te = linkedWorld.getTileEntity(linkedPortalPos);
                if (!(te instanceof TileEntityPortalCore) || !((TileEntityPortalCore) te).isActive()) {
                    isValid = false;
                    invalidReason = "Связанное ядро портала неактивно или отсутствует";
                }
            }
        }

        if (!isValid && isActive) {
            FMLLog.log(Level.WARN, "[BNP] Портал деактивирован из-за нарушения целостности: %s", invalidReason);

            // Деактивируем портал
            setActive(false);

            // Удаляем блоки портала
            breakPortal();

            // Обновляем состояние блока ядра
            IBlockState state = world.getBlockState(pos);
            world.setBlockState(pos, state.withProperty(BlockPortalCore.ACTIVE, false), 3);

            // Разрушаем связанный портал полностью
            breakLinkedPortalCompletely();

            // Разрушаем ядро на текущей стороне
            world.setBlockToAir(pos);
        }
    }

    /**
     * Разрушает все блоки портала
     */
    public void breakPortal() {
        for (BlockPos blockPos : portalBlocks) {
            if (world.isBlockLoaded(blockPos) && world.getBlockState(blockPos).getBlock() == ModBlocks.CustomPortal) {
                world.setBlockToAir(blockPos);
            }
        }
        portalBlocks.clear();
        markDirty();
    }

    /**
     * Разрушает все блоки рамки
     */
    public void breakFrame() {
        for (BlockPos blockPos : frameBlocks) {
            if (world.isBlockLoaded(blockPos) && world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) {
                world.setBlockToAir(blockPos);
            }
        }
        frameBlocks.clear();
        markDirty();
    }

    /**
     * Полностью разрушает связанный портал на другой стороне
     */
    public void breakLinkedPortalCompletely() {
        if (linkedPortalPos == null || beingDestroyed) {
            return;
        }

        WorldServer targetWorld = world.getMinecraftServer().getWorld(linkedDimension);
        if (targetWorld == null) {
            FMLLog.log(Level.ERROR, "[BNP] Не удалось получить доступ к целевому измерению: %d", linkedDimension);
            return;
        }

        // Проверяем, загружен ли чанк с порталом
        if (!targetWorld.isBlockLoaded(linkedPortalPos)) {
            FMLLog.log(Level.WARN, "[BNP] Чанк с целевым порталом не загружен, загружаем...");
            targetWorld.getChunkProvider().loadChunk(linkedPortalPos.getX() >> 4, linkedPortalPos.getZ() >> 4);

            // Ждем небольшую задержку для полной загрузки чанка
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Игнорируем прерывание
            }
        }

        TileEntity te = targetWorld.getTileEntity(linkedPortalPos);
        if (te instanceof TileEntityPortalCore) {
            TileEntityPortalCore linkedCore = (TileEntityPortalCore) te;
            linkedCore.setBeingDestroyed(true);
            linkedCore.setActive(false);

            // Важно: устанавливаем флаг, что ядро разрушено системой, а не игроком
            // чтобы предмет активации не выпал при системном разрушении
            linkedCore.setBrokenByPlayer(false);

            // Разрушаем блоки портала
            linkedCore.breakPortal();

            // Разрушаем рамку из обсидиана
            linkedCore.breakFrame();

            // Разрушаем ядро портала
            targetWorld.setBlockToAir(linkedPortalPos);

            FMLLog.log(Level.INFO, "[BNP] Связанный портал успешно разрушен");
        } else {
            FMLLog.log(Level.ERROR, "[BNP] Не удалось найти TileEntity связанного ядра");
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        isActive = compound.getBoolean("Active");

        // Загружаем направление, с поддержкой обратной совместимости
        if (compound.hasKey("Facing")) {
            // Новый формат - полное направление
            facing = EnumFacing.byName(compound.getString("Facing"));
        } else if (compound.hasKey("Axis")) {
            // Старый формат - только ось
            EnumFacing.Axis axis = EnumFacing.Axis.byName(compound.getString("Axis"));
            if (axis == EnumFacing.Axis.Z) {
                facing = EnumFacing.NORTH;
            } else if (axis == EnumFacing.Axis.X) {
                facing = EnumFacing.EAST;
            }
        }

        if (compound.hasUniqueId("PortalId")) {
            portalId = compound.getUniqueId("PortalId");
        }
        if (compound.hasKey("LinkedX")) {
            linkedPortalPos = new BlockPos(
                    compound.getInteger("LinkedX"),
                    compound.getInteger("LinkedY"),
                    compound.getInteger("LinkedZ")
            );
        }
        linkedDimension = compound.getInteger("LinkedDimension");
        isNetherSide = compound.getBoolean("IsNetherSide");

        // Загружаем флаг разрушения игроком
        brokenByPlayer = compound.getBoolean("BrokenByPlayer");

        // Загружаем предмет активации
        if (compound.hasKey("ActivationItem")) {
            activationItem = new ItemStack(compound.getCompoundTag("ActivationItem"));
        }

        // Загружаем блоки портала
        portalBlocks.clear();
        int[] portalBlockArray = compound.getIntArray("PortalBlocks");
        for (int i = 0; i < portalBlockArray.length; i += 3) {
            if (i + 2 < portalBlockArray.length) {
                portalBlocks.add(new BlockPos(portalBlockArray[i], portalBlockArray[i + 1], portalBlockArray[i + 2]));
            }
        }

        // Загружаем блоки рамки
        frameBlocks.clear();
        int[] frameBlockArray = compound.getIntArray("FrameBlocks");
        for (int i = 0; i < frameBlockArray.length; i += 3) {
            if (i + 2 < frameBlockArray.length) {
                frameBlocks.add(new BlockPos(frameBlockArray[i], frameBlockArray[i + 1], frameBlockArray[i + 2]));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("Active", isActive);
        compound.setString("Facing", facing.getName());

        // Для обратной совместимости сохраняем также ось
        compound.setString("Axis", facing.getAxis().getName());

        if (portalId != null) {
            compound.setUniqueId("PortalId", portalId);
        }
        if (linkedPortalPos != null) {
            compound.setInteger("LinkedX", linkedPortalPos.getX());
            compound.setInteger("LinkedY", linkedPortalPos.getY());
            compound.setInteger("LinkedZ", linkedPortalPos.getZ());
        }
        compound.setInteger("LinkedDimension", linkedDimension);
        compound.setBoolean("IsNetherSide", isNetherSide);

        // Сохраняем флаг разрушения игроком
        compound.setBoolean("BrokenByPlayer", brokenByPlayer);

        // Сохраняем предмет активации
        if (!activationItem.isEmpty()) {
            NBTTagCompound itemTag = new NBTTagCompound();
            activationItem.writeToNBT(itemTag);
            compound.setTag("ActivationItem", itemTag);
        }

        // Сохраняем блоки портала
        int[] portalBlockArray = new int[portalBlocks.size() * 3];
        for (int i = 0; i < portalBlocks.size(); i++) {
            BlockPos pos = portalBlocks.get(i);
            portalBlockArray[i * 3] = pos.getX();
            portalBlockArray[i * 3 + 1] = pos.getY();
            portalBlockArray[i * 3 + 2] = pos.getZ();
        }
        compound.setIntArray("PortalBlocks", portalBlockArray);

        // Сохраняем блоки рамки
        int[] frameBlockArray = new int[frameBlocks.size() * 3];
        for (int i = 0; i < frameBlocks.size(); i++) {
            BlockPos pos = frameBlocks.get(i);
            frameBlockArray[i * 3] = pos.getX();
            frameBlockArray[i * 3 + 1] = pos.getY();
            frameBlockArray[i * 3 + 2] = pos.getZ();
        }
        compound.setIntArray("FrameBlocks", frameBlockArray);

        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
