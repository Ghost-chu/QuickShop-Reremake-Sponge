package org.maxgamer.quickshop.Shop;

import java.util.List;
import java.util.UUID;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public abstract interface Shop {
	public abstract Shop clone();

	public abstract int getRemainingStock();

	public abstract int getRemainingSpace();

	public abstract boolean matches(ItemStack paramItemStack);

	public abstract Location<World> getLocation();

	public abstract double getPrice();

	public abstract void setPrice(double paramDouble);

	public abstract void update();

	public abstract short getDurability();

	public abstract UUID getOwner();

	public abstract ItemStack getItem();

	public abstract void remove(ItemStack paramItemStack, int paramInt);

	public abstract void add(ItemStack paramItemStack, int paramInt);

	public abstract void sell(Player paramPlayer, int paramInt);

	public abstract void buy(Player paramPlayer, int paramInt);

	public abstract void setOwner(UUID paramString);

	public abstract void setUnlimited(boolean paramBoolean);

	public abstract boolean isUnlimited();

	public abstract ShopType getShopType();

	public abstract boolean isBuying();

	public abstract boolean isSelling();

	public abstract void setShopType(ShopType paramShopType);

	public abstract void setSignText();

	public abstract void setSignText(String[] paramArrayOfString);

	public abstract List<TileEntity> getSigns();

	public abstract boolean isAttached(BlockState paramBlock);

	public abstract String getDataName();

	public abstract void delete();

	public abstract void delete(boolean paramBoolean);

	public abstract boolean isValid();

	public abstract void onUnload();

	public abstract void onLoad();

	public abstract void onClick();
	
	public abstract String ownerName();
}