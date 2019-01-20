package org.maxgamer.quickshop.Watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Shop.Shop;
import org.maxgamer.quickshop.Shop.ShopChunk;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

/**
 * @author Netherfoam Maintains the display items, restoring them when needed.
 *         Also deletes invalid items.
 */
public class ItemWatcher implements Runnable {
	static QuickShop plugin = QuickShop.instance;

	public ItemWatcher(QuickShop plugin) {
		ItemWatcher.plugin = plugin;
	}

	public void run() {
		List<Shop> toRemove = new ArrayList<Shop>(1);
		for (Entry<String, HashMap<ShopChunk, HashMap<Location<World>, Shop>>> inWorld : plugin.getShopManager().getShops().entrySet()) {
			// This world
			World world = Sponge.getServer().getWorld(inWorld.getKey()).get();
			if (world == null)
				continue; // world not loaded.
			for (Entry<ShopChunk, HashMap<Location<World>, Shop>> inChunk : inWorld.getValue().entrySet()) {
				if (!world.getChunk(inChunk.getKey().getX(), 0, inChunk.getKey().getZ()).get().isLoaded()) {
					// If the chunk is not loaded, next chunk!
					continue;
				}
				for (Shop shop : inChunk.getValue().values()) {
					// Validate the shop.
					if (!shop.isValid()) {
						toRemove.add(shop);
						continue;
					}
				}
			}
		}
		// Now we can remove it.
		for (Shop shop : toRemove) {
			shop.delete();
		}
	}
}