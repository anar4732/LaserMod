package lasermod.block;

import lasermod.LaserMod;
import lasermod.network.PacketDispatcher;
import lasermod.network.packet.client.ColourConverterMessage;
import lasermod.tileentity.TileEntityColourConverter;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author ProPercivalalb
 */
public class BlockColourConverter extends BlockPoweredLaser {
	
	public static final PropertyEnum<EnumDyeColor> COLOUR = PropertyEnum.create("colour", EnumDyeColor.class);
	
	public BlockColourConverter() {
		super(Material.ROCK);
		this.setHardness(1.0F);
		this.setCreativeTab(LaserMod.TAB_LASER);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) { 
		return new TileEntityColourConverter(); 
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		if(!world.isRemote && stack != null) {
			if(stack.getItem() == Items.DYE) {
				TileEntityColourConverter colourConverter = (TileEntityColourConverter)world.getTileEntity(pos);
				
				int colour = 15 - stack.getItemDamage();
				if(colour > 15) colour = 15;
				else if(colour < 0) colour = 0;
				
				if(colour == colourConverter.colour) return true;
				
				colourConverter.colour = colour;
				if(!player.capabilities.isCreativeMode) stack.shrink(1);
				
				
				PacketDispatcher.sendToAllAround(new ColourConverterMessage(colourConverter), colourConverter, 512);
				
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] {FACING, POWERED});
	}
	
	/**
	@Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if(te instanceof TileEntityColourConverter) {
        	TileEntityColourConverter colourConverter = (TileEntityColourConverter)te;
            return state.withProperty(COLOUR, EnumDyeColor.byMetadata(colourConverter.colour));
        }
        return super.getExtendedState(state, world, pos);
    }**/
}
