package org.maxgamer.quickshop.Shop;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Info {
	private Location<World> loc;
	private ShopAction action;
	private ItemStack item;
	private BlockState last;
	private Shop shop;
	private Location<World> lastloc;

	public Info(Location<World> loc, ShopAction action, ItemStack item, BlockState last, Location<World> lastLoc) {
		this.loc = loc;
		this.action = action;
		this.last = last;
		this.lastloc = lastLoc;
		if (item != null)
			this.item = item.copy();
	}

	public Info(Location<World> loc, ShopAction action, ItemStack item, BlockState last, Shop shop) {
		this.loc = loc;
		this.action = action;
		this.last = last;
		if (item != null)
			this.item = item.copy();
		if (shop != null) {
			this.shop = shop.clone();
		}
	}
	/** 
	 * Get shop is or not has changed.
	 * @param Shop shop,  The need checked with this shop.
	 * */
	public boolean hasChanged(Shop shop) {
		if (this.shop.isUnlimited() != shop.isUnlimited())
			return true;
		if (this.shop.getShopType() != shop.getShopType())
			return true;
		if (!this.shop.getOwner().equals(shop.getOwner()))
			return true;
		if (this.shop.getPrice() != shop.getPrice())
			return true;
		if (!this.shop.getLocation().equals(shop.getLocation()))
			return true;
		if (!this.shop.matches(shop.getItem()))
			return true;
		return false;
	}
	/**
	 * @return ShopAction action, Get shop action.
	 * */
	public ShopAction getAction() {
		return this.action;
	}
	/**
	 * @return Location loc, Get shop's location,
	 * */
	public Location<World> getLocation() {
		return this.loc;
	}

	/*
	 * public Material getMaterial(){ return this.item.getType(); } public byte
	 * getData(){ return this.getData(); }
	 */
	/**
	 * @return ItemStack iStack, Get Shop's selling/buying item's ItemStack.
	 * */
	public ItemStack getItem() {
		return this.item;
	}

	public void setAction(ShopAction action) {
		this.action = action;
	}
	/**
	 * @return BlockState signBlock, Get block of shop's sign, may return the null.
	 * */
	public BlockState getSignBlock() {
		return this.last;
	}
	public Location<World> getSignBlockLoc() {
		return lastloc;
	}
}