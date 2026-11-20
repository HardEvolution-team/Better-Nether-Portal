package ded.betternetherportal.item;

import ded.betternetherportal.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;





public class PortalCoreItemBlock extends ItemBlock {

    public PortalCoreItemBlock(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        ItemStack activation = new ItemStack(ModConfig.getActivationItem());
        if (!activation.isEmpty()) {
            tooltip.add(I18n.format("tooltip.betternetherportal.portal_core.activation",
                    activation.getDisplayName()));
        }
    }
}
