package org.maxgamer.quickshop.Economy;

import java.util.Optional;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.service.economy.EconomyService;

public class Economy_Sponge implements EconomyCore {
	private EconomyService economyService;
	QuickShop plugin;

	public Economy_Sponge() {
		setupEconomy();
		Sponge.getEventManager().registerListeners(plugin, this);
	}
	
	public void economyWasChanged(ChangeServiceProviderEvent e) {
		Util.debugLog("Reloading economy service: Sponge say it changed.");
		Optional<EconomyService> serviceOpt = Sponge.getServiceManager().provide(EconomyService.class);
		if (!serviceOpt.isPresent()) {
		    plugin.getLogger().warn("No economy service registed!");
		}
		economyService = serviceOpt.get();
	}

	private boolean setupEconomy() {
		Optional<EconomyService> serviceOpt = Sponge.getServiceManager().provide(EconomyService.class);
		if (!serviceOpt.isPresent()) {
		    plugin.getLogger().warn("No economy service registed!");
		}
		economyService = serviceOpt.get();
//		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
//		if (economyProvider != null) {
//			this.vault = ((Economy) economyProvider.getProvider());
//		}
		return this.economyService!= null;
	}

	@Override
	public boolean isValid() {
		return this.economyService != null;
	}

	@Override
	public String format(double balance) {
		try {
			return this.vault.format(balance);
		} catch (Exception e) {
		}
		try {
		return String.valueOf(QuickShop.instance.getConfig().getString("shop.alternate-currency-symbol") + balance);
		}catch(Exception e) {
			return String.valueOf('$' + balance);
		}
	}

	@Override
	public boolean deposit(UUID name, double amount) {
		OfflinePlayer p = Bukkit.getOfflinePlayer(name);
		boolean result = this.vault.depositPlayer(p, amount).transactionSuccess();
		return result;
	}

	@Override
	public boolean withdraw(UUID name, double amount) {
		OfflinePlayer p = Bukkit.getOfflinePlayer(name);
		boolean result = this.vault.withdrawPlayer(p, amount).transactionSuccess();
		return result;
	}

	@Override
	public boolean transfer(UUID from, UUID to, double amount) {
		OfflinePlayer pFrom = Bukkit.getOfflinePlayer(from);
		OfflinePlayer pTo = Bukkit.getOfflinePlayer(to);
		if (this.vault.getBalance(pFrom) >= amount) {
			if (this.vault.withdrawPlayer(pFrom, amount).transactionSuccess()) {
				if (!this.vault.depositPlayer(pTo, amount).transactionSuccess()) {
					this.vault.depositPlayer(pFrom, amount);
					return false;
				}
				return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public double getBalance(UUID name) {
		OfflinePlayer p = Bukkit.getOfflinePlayer(name);
		return this.vault.getBalance(p);
	}
}