package org.maxgamer.quickshop.Shop;

import org.maxgamer.quickshop.Shop.Shop;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class ShopLoadEvent extends AbstractEvent implements Cancellable {
	private boolean cancelled;
	private Shop shop;
	private Cause cause;

	/** Getting loading shops **/

	public ShopLoadEvent(Shop shop,Cause cause) {
		this.shop = shop;
		this.cause = cause;
	}

	public Shop getShop() {
		return shop;
	}

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
