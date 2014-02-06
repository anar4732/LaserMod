package lasermod.tileentity;

import java.util.ArrayList;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import lasermod.LaserMod;
import lasermod.api.ILaser;
import lasermod.api.ILaserProvider;
import lasermod.api.ILaserReciver;
import lasermod.api.LaserInGame;
import lasermod.network.packet.PacketColourConverter;
import lasermod.network.packet.PacketReflector;
import lasermod.util.LaserUtil;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Facing;
import net.minecraft.world.World;

/**
 * @author ProPercivalalb
 */
public class TileEntityReflector extends TileEntityLaserDevice implements ILaserProvider, ILaserReciver {

	public boolean[] openSides = new boolean[] {true, true, true, true, true, true};
	public ArrayList<LaserInGame> lasers = new ArrayList<LaserInGame>();
	
	private int lagReduce = -1;
	
	@Override
	public void updateEntity() {
		if(this.worldObj.isRemote) return;
		
		this.lagReduce += 1;
		if(this.lagReduce % LaserUtil.TICK_RATE != 0) return;
		
		for(int i = 0; i < this.openSides.length; ++i)
			if(!LaserUtil.isValidSourceOfPowerOnSide(this, i))
				this.removeAllLasersFromSide(i);
		
		for(int i = 0; i < this.openSides.length; ++i) {
			if(this.openSides[i] || this.containsInputSide(i) || this.lasers.size() == 0)
				continue;
			ILaserReciver reciver = LaserUtil.getFirstReciver(this, i);
			if(reciver != null) {
				LaserInGame laserInGame = this.getOutputLaser(i);
			  	if(reciver.canPassOnSide(this.worldObj, this.xCoord, this.yCoord, this.zCoord, Facing.oppositeSide[this.getBlockMetadata()], laserInGame)) {
					reciver.passLaser(this.worldObj, this.xCoord, this.yCoord, this.zCoord, Facing.oppositeSide[this.getBlockMetadata()], laserInGame);
				}
			}
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		
		int[] open = tag.getIntArray("openSides");
		for(int i = 0; i < open.length; ++i)
			this.openSides[i] = open[i] == 1;
		
		int amount = tag.getInteger("laserCount");
		 for(int i = 0; i < amount; ++i)
			 this.lasers.add(new LaserInGame(tag.getCompoundTag("laser" + i)));
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		
		int[] open = new int[this.openSides.length];
		for(int i = 0; i < this.openSides.length; ++i)
			open[i] = this.openSides[i] ? 1 : 0;
		tag.setIntArray("openSides", open);
		
		tag.setInteger("laserCount", this.lasers.size());
		
		 for(int i = 0; i < lasers.size(); ++i)
			 tag.setTag("laser" + i, this.lasers.get(i).writeToNBT(new NBTTagCompound()));
	}
	
	public boolean addLaser(LaserInGame laserInGame, int side) {
		int i = getIndexOfLaserSide(side);
		if(i == -1) {
			FMLLog.info("-1");
			lasers.add(laserInGame);
			return true;
		}
		else {
			FMLLog.info("" + i);
			lasers.add(i, laserInGame);
			lasers.remove(i + 1);
			return true;
		}
		
	}
	
	public int openSides() {
		int count = 0;
		for(boolean bool : this.openSides)
			if(!bool)
				count++;
		return count;
	}
	
	public boolean containsInputSide(int side) {
		for(int i = 0; i < this.lasers.size(); ++i)
			if(lasers.get(i).getSide() == side)
				return true;
		return false;
	}
	
	public int getIndexOfLaserSide(int side) {
		for(int i = 0; i < this.lasers.size(); ++i)
			if(lasers.get(i).getSide() == side)
				return i;
		return -1;
	}
	
	public LaserInGame getLaserFromSide(int side) {
		for(int i = 0; i < this.lasers.size(); ++i)
			if(lasers.get(i).getSide() == side)
				return lasers.get(i);
		return null;
	}
	
	public boolean removeAllLasersFromSide(int side) {
		boolean flag = false;
		for(int i = 0; i < lasers.size(); ++i) {
			LaserInGame old = lasers.get(i);
			if(old.getSide() == side) {
				lasers.remove(i);
				flag = true;
			}
		}
		if(flag) {
			this.checkAllRecivers();
			LaserMod.NETWORK_MANAGER.sendPacketToAllAround(new PacketReflector(this.xCoord, this.yCoord, this.zCoord, this), this.worldObj.provider.dimensionId, this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D, 512);
		}
		
		return flag;
	}
	
	public void checkAllRecivers() {
		for(int i = 0; i < this.openSides.length; ++i) {
			if((!this.openSides[i] && !(this.lasers.size() == 0)) || this.containsInputSide(i))
				continue;
			ILaserReciver reciver = LaserUtil.getFirstReciver(this, i);
			
			if(reciver != null) {
			  	reciver.removeLasersFromSide(this.worldObj, this.xCoord, this.yCoord, this.zCoord, Facing.oppositeSide[i]);
			}
		}
	}
	
	@Override
	public LaserInGame getOutputLaser(int side) {
		int facing = Facing.oppositeSide[side];
		ArrayList<ILaser> laserList = new ArrayList<ILaser>();
		for(LaserInGame lig : this.lasers) {
			for(ILaser laser : lig.getLaserType()) {
				if(!laserList.contains(laserList))
					laserList.add(laser);
			}
		}
		
		LaserInGame laserInGame = new LaserInGame(laserList);
		double totalPower = 0.0D;
		int red = lasers.get(0).red;
		int green = lasers.get(0).green;
		int blue = lasers.get(0).blue;
		for(int i = 1; i < lasers.size(); ++i) {
			red = (red * lasers.get(i).red) / 255;
			green = (green * lasers.get(i).green) / 255;
			blue = (blue * lasers.get(i).blue) / 255;
		}
		
		laserInGame.red = red;
		laserInGame.green = green;
		laserInGame.blue = blue;
				
		for(LaserInGame laser : lasers)
			totalPower += laser.getStrength();
		
		laserInGame.setSide(facing);
		laserInGame.setStrength(totalPower);
		
		return laserInGame;
	}
	
	@Override
	public int getX() { return this.xCoord; }
	@Override
	public int getY() { return this.yCoord; }
	@Override
	public int getZ() { return this.zCoord; }

	@Override
	public World getWorld() {
		return this.worldObj;
	}
	
	@Override
	public boolean canPassOnSide(World world, int orginX, int orginY, int orginZ, int side, LaserInGame laserInGame) {
		return !this.openSides[side] && (!this.containsInputSide(side) || !this.getLaserFromSide(side).equals(lasers));
	}
	
	@Override
	public void passLaser(World world, int orginX, int orginY, int orginZ, int side, LaserInGame laserInGame) {
		this.addLaser(laserInGame, side);
		
		LaserMod.NETWORK_MANAGER.sendPacketToAllAround(new PacketReflector(this.xCoord, this.yCoord, this.zCoord, this), world.provider.dimensionId, this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D, 512);
		
	}

	@Override
	public void removeLasersFromSide(World world, int orginX, int orginY, int orginZ, int side) {
		
		boolean flag = this.removeAllLasersFromSide(side);
		
		if(flag)
			LaserMod.NETWORK_MANAGER.sendPacketToAllAround(new PacketReflector(this.xCoord, this.yCoord, this.zCoord, this), world.provider.dimensionId, this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D, 512);
	}
	
	@Override
	public boolean isSendingSignalFromSide(World world, int askerX, int askerY, int askerZ, int side) {
		return !this.openSides[side] && this.lasers.size() > 0;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
    	return INFINITE_EXTENT_AABB;
    }
	
	@Override
	@SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
