package org.maxgamer.quickshop.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.maxgamer.quickshop.QuickShop;

/**
 * 
 * @author Netherfoam
 * 
 */
public class ChatListener implements Listener {
	QuickShop plugin;

	public ChatListener(QuickShop plugin) {
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.LOWEST,ignoreCancelled=true)
	public void onChat(AsyncPlayerChatEvent e) {
		if (!plugin.getShopManager().getActions().containsKey(e.getPlayer().getUniqueId())) {
			plugin.log(plugin.getShopManager().getActions().toString());
			return;
		}
		plugin.getShopManager().handleChat(e.getPlayer(), e.getMessage());
		e.setCancelled(true);
	}
}