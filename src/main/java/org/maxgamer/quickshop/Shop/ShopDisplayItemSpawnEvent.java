package org.maxgamer.quickshop.Shop;

import org.maxgamer.quickshop.Shop.Shop;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.item.inventory.ItemStack;

public class ShopDisplayItemSpawnEvent extends AbstractEvent implements Cancellable{
		private boolean cancelled;
		private Shop shop;
		private ItemStack iStack;
		private boolean fakeItem;
		private Cause cause;
		/**
		 * This event is called before the shop display item created
		 */
		public ShopDisplayItemSpawnEvent(Shop shop, ItemStack iStack, Cause cause) {
			this.shop = shop;
			this.iStack = iStack;
			this.cause = cause;
		}
		/**
		 * This event is called before the shop display item created
		 */
		public ShopDisplayItemSpawnEvent(Shop shop, ItemStack iStack, boolean fakeItem) {
			this.shop = shop;
			this.iStack = iStack;
			this.fakeItem = fakeItem;
		}
		public Shop getShop() {
			return shop;
		}
	    public ItemStack getItemStack() {
			return iStack;
		}
		public boolean getFakeItem() {return fakeItem;}
		@Override
		public boolean isCancelled() {
			return this.cancelled;
		}

		@Override
		public void setCancelled(boolean cancel) {
			this.cancelled = cancel;
		}
		@Override
		public Cause getCause() {
			return this.cause;
		}
	
}
