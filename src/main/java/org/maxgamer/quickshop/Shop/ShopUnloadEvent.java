package org.maxgamer.quickshop.Shop;

import org.maxgamer.quickshop.Shop.Shop;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class ShopUnloadEvent extends AbstractEvent{
	private Cause cause;
	/**Getting the unloading shop, Can't cancel.**/
	public ShopUnloadEvent(Shop shop,Cause cause) {
		this.shop = shop;
		this.cause = cause;
	}
	private Shop shop;
	public Shop getShop() {
		return shop;
	}

	@Override
	public Cause getCause() {
		return this.cause;
	}
}
