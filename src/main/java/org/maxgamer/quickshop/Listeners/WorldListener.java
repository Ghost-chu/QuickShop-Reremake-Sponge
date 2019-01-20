package org.maxgamer.quickshop.Listeners;

import java.util.HashMap;
import java.util.Map.Entry;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Shop.Shop;
import org.maxgamer.quickshop.Shop.ShopChunk;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class WorldListener {
	QuickShop plugin;

	public WorldListener(QuickShop plugin) {
		this.plugin = plugin;
	}

	@org.spongepowered.api.event.Listener
	public void onWorldLoad(LoadWorldEvent e) {
		/* *************************************
		 * This listener fixes any broken world references. Such as hashmap
		 * lookups will fail, because the World reference is different, but the
		 * world value is the same.
		 *  ************************************
		 */
		World world = e.getTargetWorld();
		// New world data
		HashMap<ShopChunk, HashMap<Location<World>, Shop>> inWorld = new HashMap<ShopChunk, HashMap<Location<World>, Shop>>(1);
		// Old world data
		HashMap<ShopChunk, HashMap<Location<World>, Shop>> oldInWorld = plugin.getShopManager().getShops(world.getName());
		// Nothing in the old world, therefore we don't care. No locations to
		// update.
		if (oldInWorld == null)
			return;
		for (Entry<ShopChunk, HashMap<Location<World>, Shop>> oldInChunk : oldInWorld.entrySet()) {
			HashMap<Location<World>, Shop> inChunk = new HashMap<Location<World>, Shop>(1);
			// Put the new chunk were the old chunk was
			inWorld.put(oldInChunk.getKey(), inChunk);
			for (Entry<Location<World>, Shop> entry : oldInChunk.getValue().entrySet()) {
				Shop shop = entry.getValue();
				/**@TODO Set world removed, but i don't know what will happed.**/
				//shop.getLocation().setWorld(world);
				inChunk.put(shop.getLocation(), shop);
			}
		}
		// Done - Now we can store the new world dataz!
		plugin.getShopManager().getShops().put(world.getName(), inWorld);
		// This is a workaround, because I don't get parsed chunk events when a
		// world first loads....
		// So manually tell all of these shops they're loaded.
		for (Chunk chunk : world.getLoadedChunks()) {
			HashMap<Location<World>, Shop> inChunk = plugin.getShopManager().getShops(chunk);
			if (inChunk == null)
				continue;
			for (Shop shop : inChunk.values()) {
				shop.onLoad();
			}
		}
	}

	@Listener
	public void onWorldUnload(UnloadWorldEvent e) {
	    if(e.isCancelled()){
	        return;
        }
		// This is a workaround, because I don't get parsed chunk events when a
		// world unloads, I think...
		// So manually tell all of these shops they're unloaded.
		for (Chunk chunk : e.getTargetWorld().getLoadedChunks()) {
			HashMap<Location<World>, Shop> inChunk = plugin.getShopManager().getShops(chunk);
			if (inChunk == null)
				continue;
			for (Shop shop : inChunk.values()) {
				shop.onUnload();
			}
		}
	}
}