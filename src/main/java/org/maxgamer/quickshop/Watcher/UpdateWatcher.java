package org.maxgamer.quickshop.Watcher;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.Updater;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Color;

public class UpdateWatcher implements Listener {
	static BukkitTask cronTask = null;
	static boolean hasNewUpdate = false;

	public static void init() {
		cronTask = new BukkitRunnable() {

			@Override
			public void run() {
				if (!Updater.checkUpdate()) {
					hasNewUpdate = false;
					return;
				}
				QuickShop.instance.getLogger().info("New QuickShop released, now updated on SpigotMC.org!");
				QuickShop.instance.getLogger().info("Update here: https://www.spigotmc.org/resources/62575/");
				hasNewUpdate = true;
			}
		}.runTaskTimerAsynchronously(QuickShop.instance, 1, 20 * 60 * 60);
	}

	public static void uninit() {
		hasNewUpdate = false;
		if (cronTask == null) {
			return;
		}
		cronTask.cancel();
	}
	@EventHandler
	public void PlayerJoin(PlayerJoinEvent e) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (e.getPlayer().hasPermission("quickshop.alert") && hasNewUpdate) {
					e.getPlayer().sendMessage(TextColors.GREEN + "New QuickShop released, now updated on SpigotMC.org!");
					e.getPlayer().sendMessage(
							TextColors.GREEN + "Update here: https://www.spigotmc.org/resources/62575/");
				}

			}
		}.runTaskLaterAsynchronously(QuickShop.instance, 80);
	}

}
