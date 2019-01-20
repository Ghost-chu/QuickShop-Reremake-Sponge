package org.maxgamer.quickshop.Shop;

import org.maxgamer.quickshop.Shop.Shop;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;

public class ShopPurchaseEvent extends AbstractEvent implements Cancellable {
	private Shop shop;
	private Player p;
	private int amount;
	private boolean cancelled;
	private Cause cause;

	/**
	 * Builds a new shop purchase event
	 * 
	 * @param shop
	 *            The shop bought from
	 * @param p
	 *            The player buying
	 * @param amount
	 *            The amount they're buying
	 */
	public ShopPurchaseEvent(Shop shop, Player p, int amount, Cause cause) {
		this.shop = shop;
		this.p = p;
		this.amount = amount;
		this.cause = cause;
	}

	/**
	 * The shop used in this event
	 * 
	 * @return The shop used in this event
	 */
	public Shop getShop() {
		return this.shop;
	}

	/**
	 * The player trading with the shop
	 * 
	 * @return The player trading with the shop
	 */
	public Player getPlayer() {
		return this.p;
	}

	/**
	 * The amount the purchase was for
	 * 
	 * @return The amount the purchase was for
	 */
	public int getAmount() {
		return this.amount;
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