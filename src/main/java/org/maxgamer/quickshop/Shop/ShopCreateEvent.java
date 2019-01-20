package org.maxgamer.quickshop.Shop;

import org.maxgamer.quickshop.Shop.Shop;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class ShopCreateEvent extends AbstractEvent implements Cancellable {
	private Shop shop;
	private boolean cancelled;
	private Player p;
	private Cause cause;

	public ShopCreateEvent(Shop shop, Player p, Cause cause) {
		this.shop = shop;
		this.p = p;
		this.cause = cause;
	}

	/**
	 * The shop to be created
	 * 
	 * @return The shop to be created
	 */
	public Shop getShop() {
		return this.shop;
	}

	/**
	 * The player who is creating this shop
	 * 
	 * @return The player who is creating this shop
	 */
	public Player getPlayer() {
		return p;
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