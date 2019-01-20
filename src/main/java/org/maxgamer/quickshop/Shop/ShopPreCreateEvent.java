package org.maxgamer.quickshop.Shop;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * This event is called before the shop creation request is sent. E.g. A player
 * clicks a chest, this event is thrown, if successful, the player is asked how
 * much they wish to trade for.
 */
public class ShopPreCreateEvent extends AbstractEvent implements Cancellable {
	private boolean cancelled;
	private Player p;
	private Location<World> loc;
	private Cause cause;

	public ShopPreCreateEvent(Player p, Location<World> loc,Cause cause) {
		this.loc = loc;
		this.p = p;
		this.cause = cause;
	}

	/**
	 * The location of the shop that will be created.
	 * 
	 * @return The location of the shop that will be created.
	 */
	public Location<World> getLocation() {
		return loc;
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