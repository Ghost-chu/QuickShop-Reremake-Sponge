package org.maxgamer.quickshop.Listeners;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Shop.Shop;
import org.maxgamer.quickshop.Util.MsgUtil;
import org.maxgamer.quickshop.Util.Util;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.yaml.snakeyaml.tokens.WhitespaceToken;

public class LockListener {
	private QuickShop plugin;

	public LockListener(QuickShop plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InteractBlockEvent.Secondary e) {
		Location<World> b = e.getTargetBlock().getLocation().get();
		Player p = "No Player I Can Get In Event, Sucks";
//		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
//			return; // Didn't right click it, we dont care.
		if (!Util.canBeShop(b,null,true))
			return; // Interacted with air
		Shop shop = plugin.getShopManager().getShop(e.getTargetBlock().getLocation().get());
		// Make sure they're not using the non-shop half of a double chest.
		if (shop == null) {
			b = Util.getSecondHalf(e.getTargetBlock().getLocation().get());
			if (b == null)
				return;
			shop = plugin.getShopManager().getShop(b);
			if (shop == null)
				return;
		}
		if (!shop.getOwner().equals(p.getUniqueId())) {
			if (p.hasPermission("quickshop.other.open")) {
				
				p.sendMessage(Text.of(MsgUtil.getMessage("bypassing-lock")));
				return;
			}
			p.sendMessage(Text.of(MsgUtil.getMessage("that-is-locked")));
			e.setCancelled(true);
			return;
		}
	}

	/**
	 * Handles hopper placement
	 */
	@Listener
	public void onPlace(ChangeBlockEvent.Place e) {
		Location<World> b = "What the fuck, I can't get block location in event, Sucks API";
		try {
			if (b.getBlock().getType() != BlockTypes.HOPPER)
				return;
		} catch (NoSuchFieldError er) {
			return; // Your server doesn't have hoppers
		}
		Player p = e.getPlayer();
		if (Util.isOtherShopWithinHopperReach(b, p) == false)
			return;

		if (p.hasPermission("quickshop.other.open")) {
			p.sendMessage(Text.of(MsgUtil.getMessage("bypassing-lock")));
			return;
		}
		p.sendMessage(Text.of(MsgUtil.getMessage("that-is-locked")));
		e.setCancelled(true);
	}

	/**
	 * Removes chests when they're destroyed.
	 */
	@Listener
	public void onBreak(ChangeBlockEvent.Break e) {
		Block b = e.getBlock();
		if(b.getState() instanceof Sign) {
			Sign sign = (Sign)b.getState();
			if(sign.getLine(0).equals(plugin.getConfig().getString("lockette.private"))||sign.getLine(0).equals(plugin.getConfig().getString("lockette.more_users"))){
				//Ignore break lockette sign
				plugin.getLogger().info("Skipped a dead-lock shop sign.(Lockette or other sign-lock plugin)");
				return;
			}
		}
		Player p = e.getPlayer();
		// If the chest was a chest
		if (Util.canBeShop(b,null,true)) {
			Shop shop = plugin.getShopManager().getShop(b.getLocation());
			if (shop == null)
				return; // Wasn't a shop
			// If they owned it or have bypass perms, they can destroy it
			if (!shop.getOwner().equals(p.getUniqueId()) && !p.hasPermission("quickshop.other.destroy")) {
				e.setCancelled(true);
				p.sendMessage(MsgUtil.getMessage("no-permission"));
				return;
			}
		} else if (b.getType() == Material.WALL_SIGN) {
			if(b instanceof Sign) {
				Sign sign = (Sign)b;
				if(sign.getLine(0).equals(plugin.getConfig().getString("lockette.private"))||sign.getLine(0).equals(plugin.getConfig().getString("lockette.more_users"))){
					//Ignore break lockette sign
					plugin.getLogger().info("Skipped a dead-lock shop sign.(Lockette)");
					return;
				}
			}
			b = Util.getAttached(b);
			if (b == null)
				return;
			Shop shop = plugin.getShopManager().getShop(b.getLocation());
			if (shop == null)
				return;
			// If they're the shop owner or have bypass perms, they can destroy
			// it.
			if (!shop.getOwner().equals(p.getUniqueId()) && !p.hasPermission("quickshop.other.destroy")) {
				e.setCancelled(true);
				p.sendMessage(MsgUtil.getMessage("no-permission"));
				return;
			}
		}
	}

	/**
	 * Handles shops breaking through explosions
	 */
	@EventHandler(priority = EventPriority.LOW,ignoreCancelled=true)
	public void onExplode(EntityExplodeEvent e) {
		if (e.isCancelled())
			return;
		for (int i = 0; i < e.blockList().size(); i++) {
			Block b = e.blockList().get(i);
			Shop shop = plugin.getShopManager().getShop(b.getLocation());
			if (shop != null) {
				// ToDo: Shouldn't I be decrementing 1 here? Concurrency and
				// all..
				e.blockList().remove(b);
			}
		}
	}
}