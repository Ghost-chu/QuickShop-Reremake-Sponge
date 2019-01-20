package org.maxgamer.quickshop.Economy;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

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
			return economyService.getDefaultCurrency().format(BigDecimal.valueOf(balance)).toPlain();
		} catch (Exception e) {
			try {
			return String.valueOf(QuickShop.instance.getConfig().getString("shop.alternate-currency-symbol") + balance);
			}catch(Exception e2) {
				return String.valueOf('$' + balance);
			}
		}
		
	}

	@Override
	public boolean deposit(UUID name, double amount) {
		Account account = this.economyService.getOrCreateAccount(name).get();
		TransactionResult result = account.deposit(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Cause.builder().build(EventContext.empty()));
		switch (result.getResult()) {
		case ACCOUNT_NO_FUNDS:
			return false;
		case ACCOUNT_NO_SPACE:
			return false;
		case CONTEXT_MISMATCH:
			return false;
		case FAILED:
			return false;
		case SUCCESS:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean withdraw(UUID name, double amount) {
		Account account = this.economyService.getOrCreateAccount(name).get();
		TransactionResult result = account.withdraw(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Cause.builder().build(EventContext.empty()));
		switch (result.getResult()) {
		case ACCOUNT_NO_FUNDS:
			return false;
		case ACCOUNT_NO_SPACE:
			return false;
		case CONTEXT_MISMATCH:
			return false;
		case FAILED:
			return false;
		case SUCCESS:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean transfer(UUID from, UUID to, double amount) {
		Account fromAccount = this.economyService.getOrCreateAccount(from).get();
		Account toAccount = this.economyService.getOrCreateAccount(to).get();
		TransactionResult result = fromAccount.transfer(toAccount,economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Cause.builder().build(EventContext.empty()));
		switch (result.getResult()) {
		case ACCOUNT_NO_FUNDS:
			return false;
		case ACCOUNT_NO_SPACE:
			return false;
		case CONTEXT_MISMATCH:
			return false;
		case FAILED:
			return false;
		case SUCCESS:
			return true;
		default:
			return false;
		}
	}

	@Override
	public double getBalance(UUID name) {
		Account account = this.economyService.getOrCreateAccount(name).get();
		return Double.parseDouble(account.getBalance(economyService.getDefaultCurrency()).toString());
	}
}