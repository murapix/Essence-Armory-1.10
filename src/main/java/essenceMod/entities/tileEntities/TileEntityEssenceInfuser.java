package essenceMod.entities.tileEntities;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import essenceMod.items.ItemShardContainer;
import essenceMod.registry.ModBlocks;
import essenceMod.registry.ModItems;
import essenceMod.registry.crafting.InfuserRecipes;
import essenceMod.registry.crafting.ItemRecipe;
import essenceMod.registry.crafting.upgrades.Upgrade;
import essenceMod.utility.Reference;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityEssenceInfuser extends TileEntity implements IInventory, ITickable
{
	public static final int InfuserSlotCount = 1;
	public static final int PylonSlotCount = 120;
	public static final int TotalSlotCount = InfuserSlotCount + PylonSlotCount;

	public static final int InfuserSlot = 0;
	public static final int FirstInnerSlot = InfuserSlot + InfuserSlotCount;

	protected ItemStack[] slots = new ItemStack[TotalSlotCount];

	private ArrayList<TileEntity> pylons;

	private boolean active;

	public int infuseTime;
	public final short TotalInfuseTime = 200;

	@Override
	public void update()
	{
		checkPylons();
		grabPylons();

		markDirty();
		worldObj.markBlockRangeForRenderUpdate(pos, pos.add(1, 1, 1));

		Upgrade upgrade = InfuserRecipes.checkUpgradeRecipe(slots[InfuserSlot], getPylonItems());
		ItemStack output = InfuserRecipes.checkItemRecipe(slots[InfuserSlot], getPylonItems());
		if (active && (upgrade != null || output != null))
		{
			infuseTime++;
			if (infuseTime % 30 == 0)
			{
				for (TileEntity pylon : pylons)
				{
					if (((TileEntityEssencePylon) pylon).getStackInSlot(0) == null)
						continue;
					double pX = pylon.getPos().getX() + 0.5;
					double pY = pylon.getPos().getY() + 1.5;
					double pZ = pylon.getPos().getZ() + 0.5;
					double iX = getPos().getX() + 0.5;
					double iY = getPos().getY() + 1.5;
					double iZ = getPos().getZ() + 0.5;
					double dX = pX - iX;
					double dY = pY - iY;
					double dZ = pZ - iZ;
					double dist = Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2) + Math.pow(dZ, 2));
					double cX = dX / (dist * 16);
					double cY = dY / (dist * 16);
					double cZ = dZ / (dist * 16);
					for (int i = 0; i < dist * 16; i++)
						essenceMod.EssenceMod.proxy.spawnGlow(getWorld(), iX + i * cX, iY + i * cY, iZ + i * cZ, 179, 117, 255);
				}
			}
			if (infuseTime >= TotalInfuseTime)
			{
				if (upgrade != null)
				{
					updatePylons();
					InfuserRecipes.addUpgrade(slots[InfuserSlot], upgrade);
				}
				else if (output != null)
				{
					ItemRecipe recipe = InfuserRecipes.getItemRecipe(slots[InfuserSlot], getPylonItems());
					updatePylons(recipe);
					slots[InfuserSlot] = output;
				}
				infuseTime = 0;
				active = false;
			}
		}
		else
		{
			infuseTime = 0;
			active = false;
		}
	}

	public void activate()
	{
		active = true;
	}

	public boolean isActive()
	{
		return active;
	}

	private boolean checkPylons()
	{
		pylons = new ArrayList<TileEntity>();

		for (int xDiff = -5; xDiff <= 5; xDiff++)
		{
			for (int zDiff = -5; zDiff <= 5; zDiff++)
			{
				TileEntity entity = worldObj.getTileEntity(pos.add(xDiff, -1, zDiff));
				if (entity != null && entity instanceof TileEntityEssencePylon)
					pylons.add(entity);
			}
		}
		return pylons.size() >= 1;
	}

	private void grabPylons()
	{
		for (int i = 0; i < pylons.size(); i++)
		{
			if (pylons.get(i) == null || !(pylons.get(i) instanceof TileEntityEssencePylon))
			{
				pylons.remove(i);
				i--;
			}
			else slots[i + 1] = ((TileEntityEssencePylon) pylons.get(i)).getStackInSlot(0);
		}
		for (int i = pylons.size() + 1; i < slots.length; i++)
			slots[i] = null;
	}

	private void updatePylons()
	{
		ArrayList<ItemStack> shardContainers = new ArrayList<ItemStack>();
		for (int i = 0; i < pylons.size(); i++)
		{
			if (pylons.get(i) == null || !(pylons.get(i) instanceof TileEntityEssencePylon))
			{
				pylons.remove(i);
				i--;
			}
			else
			{
				TileEntityEssencePylon pylon = (TileEntityEssencePylon) pylons.get(i);
				ItemStack stack = pylon.getStackInSlot(0);
				if (stack != null && stack.getItem() instanceof ItemShardContainer)
					shardContainers.add(pylon.getStackInSlot(0));
				else pylon.infuse();
			}
		}
	}

	private void updatePylons(ItemRecipe recipe)
	{
		int numShards = recipe.getShardCount();
		ArrayList<TileEntityEssencePylon> shardPylons = new ArrayList<TileEntityEssencePylon>();
		for (int i = 0; i < pylons.size(); i++)
		{
			if (pylons.get(i) == null || !(pylons.get(i) instanceof TileEntityEssencePylon))
			{
				pylons.remove(i);
				i--;
			}
			else
			{
				TileEntityEssencePylon pylon = (TileEntityEssencePylon) pylons.get(i);
				ItemStack item = pylon.getStackInSlot(0);
				if (item == null)
					continue;
				else if (item.isItemEqual(new ItemStack(ModItems.infusedShard)))
				{
					pylon.infuse();
					numShards--;
				}
				else if (item.isItemEqual(new ItemStack(ModBlocks.shardBlock)))
				{
					pylon.infuse();
					numShards -= 9;
				}
				else if (item.getItem() instanceof ItemShardContainer)
				{
					shardPylons.add(pylon);
				}
				else pylon.infuse();
			}
		}

		while (numShards > 0 && shardPylons.size() > 0)
		{
			ItemStack container = shardPylons.remove(0).getStackInSlot(0);
			numShards = ItemShardContainer.removeShards(container, numShards);
		}
	}

	public ArrayList<ItemStack> getPylonItems()
	{
		ArrayList<ItemStack> items = new ArrayList<ItemStack>();
		for (int i = FirstInnerSlot; i < TotalSlotCount; i++)
		{
			ItemStack item = getStackInSlot(i);
			if (item != null)
				items.add(item);
		}
		return items;
	}

	@Override
	public int getSizeInventory()
	{
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		return slots[i];
	}

	@Override
	public ItemStack decrStackSize(int index, int count)
	{
		ItemStack item = getStackInSlot(index);
		if (item == null)
			return null;

		ItemStack temp;
		if (item.stackSize <= count)
		{
			temp = item;
			setInventorySlotContents(index, null);
		}
		else
		{
			temp = item.splitStack(count);
			if (temp.stackSize == 0)
				setInventorySlotContents(index, null);
		}
		worldObj.markBlockRangeForRenderUpdate(pos, pos.add(1, 1, 1));
		return temp;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack item)
	{
		slots[index] = item;
		if (item != null && item.stackSize > getInventoryStackLimit())
			item.stackSize = getInventoryStackLimit();
		worldObj.markBlockRangeForRenderUpdate(pos, pos.add(1, 1, 1));
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 1;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		if (worldObj.getTileEntity(pos) != this)
			return false;
		double xOffset = 0.5;
		double yOffset = 0.5;
		double zOffset = 0.5;
		double maxDistance = 8.0 * 8.0;
		return player.getDistanceSq(pos.getX() - xOffset, pos.getY() - yOffset, pos.getZ() - zOffset) < maxDistance;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
	{
		super.writeToNBT(tagCompound);

		NBTTagList inventory = new NBTTagList();
		for (int i = 0; i < slots.length; i++)
		{
			if (slots[i] != null)
			{
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				slots[i].writeToNBT(tag);
				inventory.appendTag(tag);
			}
		}

		tagCompound.setTag("Items", inventory);

		tagCompound.setInteger("InfuseTime", infuseTime);
		return tagCompound;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound)
	{
		super.readFromNBT(tagCompound);

		NBTTagList inventory = tagCompound.getTagList("Items", NBT.TAG_COMPOUND);

		Arrays.fill(slots, null);
		for (int i = 0; i < inventory.tagCount(); i++)
		{
			NBTTagCompound item = inventory.getCompoundTagAt(i);
			byte slot = item.getByte("Slot");
			if (slot >= 0 && slot < slots.length)
				slots[slot] = ItemStack.loadItemStackFromNBT(item);
		}

		infuseTime = tagCompound.getInteger("InfuseTime");
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack item)
	{
		return item != null;
	}

	@Override
	public String getName()
	{
		return Reference.MODID + ".TileEntityEssenceInfuser";
	}

	@Override
	public boolean hasCustomName()
	{
		return false;
	}

	@Override
	public ITextComponent getDisplayName()
	{
		return new TextComponentString("Essence Infuser");
	}

	@Override
	public ItemStack removeStackFromSlot(int index)
	{
		ItemStack item = getStackInSlot(index);
		if (item != null)
			setInventorySlotContents(index, null);
		return item;
	}

	@Override
	public void openInventory(EntityPlayer player)
	{}

	@Override
	public void closeInventory(EntityPlayer player)
	{}

	@Override
	public int getField(int id)
	{
		return 0;
	}

	@Override
	public void setField(int id, int value)
	{

	}

	@Override
	public int getFieldCount()
	{
		return 0;
	}

	@Override
	public void clear()
	{

	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
	{
		return oldState.getBlock() != newState.getBlock();
	}

	@Override
	public final SPacketUpdateTileEntity getUpdatePacket()
	{
		return new SPacketUpdateTileEntity(getPos(), -999, writeToNBT(new NBTTagCompound()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		super.onDataPacket(net, pkt);
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public final NBTTagCompound getUpdateTag()
	{
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public final void handleUpdateTag(NBTTagCompound tag)
	{
		readFromNBT(tag);
	}
}