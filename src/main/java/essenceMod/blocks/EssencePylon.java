package essenceMod.blocks;

import java.util.Random;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import essenceMod.entities.tileEntities.TileEntityEssencePylon;
import essenceMod.items.IUpgradeable;
import essenceMod.tabs.ModTabs;

public class EssencePylon extends BlockContainer implements IUpgradeable, ITileEntityProvider
{
	public EssencePylon()
	{
		super(Material.ROCK);
		setCreativeTab(ModTabs.tabEssence);
		setHardness(5.0F);
		setResistance(10.0F);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata)
	{
		return new TileEntityEssencePylon();
	}
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.MODEL;
	}

	@SideOnly(Side.CLIENT)
	public void initModel()
	{
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side)
	{
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack stack, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (world.isRemote) return true;
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity == null || !(tileEntity instanceof TileEntityEssencePylon)) return true;
		TileEntityEssencePylon pylonEntity = (TileEntityEssencePylon) tileEntity;
		
		pylonEntity.markDirty();
		
		ItemStack item = pylonEntity.getStackInSlot(0);
		ItemStack playerItem = player.getHeldItem(player.getActiveHand());
		if (item != null && item.stackSize > 0)
		{
			EntityItem itemEntity = new EntityItem(world, player.posX, player.posY + player.getDefaultEyeHeight() / 2.0F, player.posZ, item.copy());
			world.spawnEntityInWorld(itemEntity);
			pylonEntity.setInventorySlotContents(0, null);
			
			return true;
		}
		else if (playerItem != null && playerItem.stackSize > 0 && pylonEntity.isItemValidForSlot(0, playerItem))
		{
			ItemStack tempItem = playerItem.splitStack(1);
			pylonEntity.setInventorySlotContents(0, tempItem);
			
			if (playerItem.stackSize == 0) player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, null);
			else player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, playerItem);
			
			return true;
		}
		return true;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		dropItems(world, pos);
		super.breakBlock(world, pos, state);
	}

	private void dropItems(World world, BlockPos pos)
	{
		Random rand = new Random();

		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof IInventory)) return;
		IInventory inventory = (IInventory) tileEntity;

		for (int i = 0; i < inventory.getSizeInventory(); i++)
		{
			ItemStack item = inventory.getStackInSlot(i);
			if (item != null && item.stackSize > 0)
			{
				float rx = rand.nextFloat() * 0.8F + 0.1F;
				float ry = rand.nextFloat() * 0.8F + 0.1F;
				float rz = rand.nextFloat() * 0.8F + 0.1F;

				EntityItem entityItem = new EntityItem(world, pos.getX() + rx, pos.getY() + ry, pos.getZ() + rz, item.copy());

				float factor = 0.05F;
				entityItem.motionX = rand.nextGaussian() * factor;
				entityItem.motionY = rand.nextGaussian() * factor + 0.2F;
				entityItem.motionY = rand.nextGaussian() * factor;
				world.spawnEntityInWorld(entityItem);
				item.stackSize = 0;
			}
		}
	}
}