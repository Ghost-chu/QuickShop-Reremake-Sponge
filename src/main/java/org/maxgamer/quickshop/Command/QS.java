package org.maxgamer.quickshop.Command;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Database.Database;
import org.maxgamer.quickshop.Database.DatabaseHelper;
import org.maxgamer.quickshop.Database.MySQLCore;
import org.maxgamer.quickshop.Database.SQLiteCore;
import org.maxgamer.quickshop.Shop.ContainerShop;
import org.maxgamer.quickshop.Shop.Info;
import org.maxgamer.quickshop.Shop.Shop;
import org.maxgamer.quickshop.Shop.ShopAction;
import org.maxgamer.quickshop.Shop.ShopChunk;
import org.maxgamer.quickshop.Shop.ShopManager;
import org.maxgamer.quickshop.Shop.ShopType;
import org.maxgamer.quickshop.Util.MsgUtil;
import org.maxgamer.quickshop.Util.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class QS implements CommandCallable{
	QuickShop plugin;

	public QS(QuickShop plugin) {
		this.plugin = plugin;
	}


	private void setUnlimited(CommandSource sender) {
		if (sender instanceof Player && sender.hasPermission("quickshop.unlimited")) {
			BlockIterator bIt = new BlockIterator((Player) sender, 10);
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null) {
					shop.setUnlimited(!shop.isUnlimited());
					shop.setSignText();
					shop.update();
					sender.sendMessage(Text.of(MsgUtil.getMessage("command.toggle-unlimited",
							(shop.isUnlimited() ? "unlimited" : "limited"))));
					return;
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop")));
			return;
		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
			return;
		}
	}
	private void silentUnlimited(CommandSource sender, String[]args) {
		if (sender.hasPermission("quickshop.unlimited")) {
			
				Shop shop = plugin.getShopManager().getShop(new Location<World>(Sponge.getServer().getWorld(args[1]).get(), Integer.valueOf(args[2]),
						Integer.valueOf(args[3]), Integer.valueOf(args[4])));
				if (shop != null) {
					shop.setUnlimited(!shop.isUnlimited());
					shop.setSignText();
					shop.update();
					MsgUtil.sendControlPanelInfo(sender, shop);
					sender.sendMessage(Text.of(MsgUtil.getMessage("command.toggle-unlimited",
							(shop.isUnlimited() ? "unlimited" : "limited")));
					return;
				}
			
		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission"));
			return;
		}
	}
	private void remove(CommandSource sender, String[] args) {
		if (sender instanceof Player == false) {
			sender.sendMessage(Text.of(TextColors.RED + "Only players may use that command.");
			return;
		}
		Player p = (Player) sender;
		BlockIterator bIt = new BlockIterator(p, 10);
		while (bIt.hasNext()) {
			Block b = bIt.next();
			Shop shop = plugin.getShopManager().getShop(b.getLocation());
			if (shop != null) {
				if (shop.getOwner().equals(p.getUniqueId())||sender.hasPermission("quickshop.other.destroy")) {
					shop.delete();
				} else {
					sender.sendMessage(Text.of(TextColors.RED + MsgUtil.getMessage("no-permission")));
				}
				return;
			}
		}
		p.sendMessage(Text.of(TextColors.RED + "No shop found!"));
	}
	private void fetchMessage(CommandSource sender, String[] args) {
		if (sender instanceof Player == false) {
			sender.sendMessage(Text.of(TextColors.RED + "Only players may use that command."));
			return;
		}
		Player p = (Player) sender;
			Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {
				@Override
				public void run() {
					MsgUtil.flush(p);
				}
			}).async().delayTicks(60).submit(plugin);
	}
	private void silentRemove(CommandSource sender, String[] args) {
		// silentRemove world x y z
		if (args.length < 4)
			return;
		Player p = (Player) sender;
		Shop shop = plugin.getShopManager().getShop(new Location<World>(Sponge.getServer().getWorld(args[1]).get(), Integer.valueOf(args[2]),
				Integer.valueOf(args[3]), Integer.valueOf(args[4])));
		if (shop == null)
			return;

		if (shop != null) {
			if (shop.getOwner().equals(p.getUniqueId())||sender.hasPermission("quickshop.other.destroy")) {
				shop.delete();
				try {
					DatabaseHelper.removeShop(plugin.getDB(), Integer.valueOf(args[2]), Integer.valueOf(args[3]), Integer.valueOf(args[4]), Sponge.getServer().getWorld(args[1]).get().getName());
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else {
				sender.sendMessage(Text.of(TextColors.RED + MsgUtil.getMessage("no-permission")));
			}
			return;
		}
	}
	@SuppressWarnings("unused")
	private void export(CommandSource sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(Text.of(TextColors.RED + "Usage: /qs export mysql|sqlite"));
			return;
		}
		String type = args[1].toLowerCase();
		if (type.startsWith("mysql")) {
			if (plugin.getDB().getCore() instanceof MySQLCore) {
				sender.sendMessage(Text.of(TextColors.RED + "Database is already MySQL"));
				return;
			}
			ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("database");
			String host = cfg.getString("host");
			String port = cfg.getString("port");
			String user = cfg.getString("user");
			String pass = cfg.getString("password");
			String name = cfg.getString("database");
			MySQLCore core = new MySQLCore(host, user, pass, name, port);
			Database target;
			try {
				target = new Database(core);
				QuickShop.instance.getDB().copyTo(target);
				sender.sendMessage(Text.of(TextColors.GREEN + "Success - Exported to MySQL " + user + "@" + host + "." + name));
			} catch (Exception e) {
				e.printStackTrace();
				sender.sendMessage(Text.of(TextColors.RED + "Failed to export to MySQL " + user + "@" + host + "." + name
						+ TextColors.RED + " Reason: " + e.getMessage()));
			}
			return;
		}
		if (type.startsWith("sql") || type.contains("file")) {
			if (plugin.getDB().getCore() instanceof SQLiteCore) {
				sender.sendMessage(Text.of(TextColors.RED + "Database is already SQLite"));
				return;
			}
			File file = new File(plugin.getConfiguration().getDataFolder(), "shops.db");
			if (file.exists()) {
				if (file.delete() == false) {
					sender.sendMessage(Text.of(
							TextColors.RED + "Warning: Failed to delete old shops.db file. This may cause errors."));
				}
			}
			SQLiteCore core = new SQLiteCore(file);
			try {
				Database target = new Database(core);
				QuickShop.instance.getDB().copyTo(target);
				sender.sendMessage(Text.of(TextColors.GREEN + "Success - Exported to SQLite: " + file.toString()));
			} catch (Exception e) {
				e.printStackTrace();
				sender.sendMessage(Text.of(TextColors.RED + "Failed to export to SQLite: " + file.toString() + " Reason: "
						+ e.getMessage()));
			}
			return;
		}
		sender.sendMessage(Text.of(TextColors.RED + "No target given. Usage: /qs export mysql|sqlite");
	}
	private void setOwner(CommandSource sender, String[] args) {
		if (sender instanceof Player && sender.hasPermission("quickshop.setowner")) {
			if (args.length < 2) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("command.no-owner-given"));
				return;
			}
			BlockIterator bIt = new BlockIterator((Player) sender, 10);
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null) {
					@SuppressWarnings("deprecation")
					OfflinePlayer p = this.plugin.getServer().getOfflinePlayer(args[1]);
					shop.setOwner(p.getUniqueId());
					shop.update();
					sender.sendMessage(Text.of(MsgUtil.getMessage("command.new-owner",
							this.plugin.getServer().getOfflinePlayer(shop.getOwner()).getName()));
					return;
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop"));
			return;
		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission"));
			return;
		}
	}
	private void refill(CommandSource sender, String[] args) {
		if (sender instanceof Player && sender.hasPermission("quickshop.refill")) {
			if (args.length < 2) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("command.no-amount-given"));
				return;
			}
			int add;
			try {
				add = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("thats-not-a-number"));
				return;
			}
			BlockIterator bIt = new BlockIterator((Living) (Player) sender, 10);
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null) {
					shop.add(shop.getItem(), add);
					sender.sendMessage(Text.of(MsgUtil.getMessage("refill-success"));
					return;
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop"));
			return;
		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission"));
			return;
		}
	}
	private void silentEmpty(CommandSource sender, String[] args) {
		if (sender.hasPermission("quickshop.refill")) {

			Shop shop = plugin.getShopManager().getShop(new Location<World>(Sponge.getServer().getWorld(args[1]).get(), Integer.valueOf(args[2]),
					Integer.valueOf(args[3]), Integer.valueOf(args[4])));
			if (shop != null) {
				if (shop instanceof ContainerShop) {
					ContainerShop cs = (ContainerShop) shop;
					cs.getInventory().clear();
					MsgUtil.sendControlPanelInfo(sender, shop);
					sender.sendMessage(Text.of(MsgUtil.getMessage("empty-success"));
					return;
				}
			}

		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission"));
			return;
		}
	}
	private void empty(CommandSource sender, String[] args) {
		if (sender instanceof Player && sender.hasPermission("quickshop.refill")) {
			BlockIterator bIt = new BlockIterator((Living) (Player) sender, 10);
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null) {
					if (shop instanceof ContainerShop) {
						ContainerShop cs = (ContainerShop) shop;
						cs.getInventory().clear();
						sender.sendMessage(Text.of(MsgUtil.getMessage("empty-success")));
						return;
					} else {
						sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop")));
						return;
					}
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop")));
			return;
		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
			return;
		}
	}
	private void find(CommandSource sender, String[] args) {
		if (sender instanceof Player && sender.hasPermission("quickshop.find")) {
			if (args.length < 2) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("command.no-type-given")));
				return;
			}
			StringBuilder sb = new StringBuilder(args[1]);
			for (int i = 2; i < args.length; i++) {
				sb.append(" " + args[i]);
			}
			String lookFor = sb.toString();
			lookFor = lookFor.toLowerCase();
			Player p = (Player) sender;
			Location<World> loc = p.getEyeLocation().clone();
			double minDistance = plugin.getConfig().getInt("shop.find-distance");
			double minDistanceSquared = minDistance * minDistance;
			int chunkRadius = (int) minDistance / 16 + 1;
			Shop closest = null;
			Chunk c = loc.getChunk();
			for (int x = -chunkRadius + c.getX(); x < chunkRadius + c.getX(); x++) {
				for (int z = -chunkRadius + c.getZ(); z < chunkRadius + c.getZ(); z++) {
					Chunk d = c.getWorld().getChunkAt(x, z);
					HashMap<Location<World>, Shop> inChunk = plugin.getShopManager().getShops(d);
					if (inChunk == null)
						continue;
					for (Shop shop : inChunk.values()) {
						if (MsgUtil.getItemi18n(shop.getDataName()).toLowerCase().contains(lookFor)
								&& shop.getLocation().getBlockPosition().distanceSquared(loc.getBlockPosition()) < minDistanceSquared) {
							closest = shop;
							minDistanceSquared = shop.getLocation().getBiomePosition().distanceSquared(loc.getBlockPosition());
						}
					}
				}
			}
			if (closest == null) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("no-nearby-shop", args[1]));
				return;
			}
			Location<World> lookat = closest.getLocation().copy().add(0.5, 0.5, 0.5);
			// Hack fix to make /qs find not used by /back
			p.teleport(this.lookAt(loc, lookat).add(0, -1.62, 0), TeleportCause.UNKNOWN);
			p.sendMessage(Text.of(
					MsgUtil.getMessage("nearby-shop-this-way", "" + (int) Math.floor(Math.sqrt(minDistanceSquared)))));
			return;
		} else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
			return;
		}
	}
	
	
	private void create(CommandSource sender, String[] args) {
		if (sender instanceof Player) {
			Player p = (Player) sender;
			ItemStack item = p.getInventory().getItemInMainHand();
			if (item.getType() != ItemTypes.AIR) {
				if (sender.hasPermission("quickshop.create.sell")) {
					BlockIterator bIt = new BlockIterator((Living) (Player) sender, 10);

					while (bIt.hasNext()) {
						Block b = bIt.next();
						if (Util.canBeShop(b,p.getUniqueId(),false)) {
							if (p != null && b != null && p.isOnline()) {
								BlockBreakEvent be = new BlockBreakEvent(b, p);
								Bukkit.getPluginManager().callEvent(be);
								if (be.isCancelled()) {
									return;
								}
							}

							if (!plugin.getShopManager().canBuildShop(p, b,
									Util.getYawFace(p.getLocation().getYaw()))) {
								// As of the new checking system, most plugins will tell the
								// player why they can't create a shop there.
								// So telling them a message would cause spam etc.
								return;
							}

							if (Util.getSecondHalf(b) != null && !p.hasPermission("quickshop.create.double")) {
								p.sendMessage(Text.of(MsgUtil.getMessage("no-double-chests")));
								return;
							}
							if (Util.isBlacklisted(item.getType())
									&& !p.hasPermission("quickshop.bypass." + item.getType().getName())) {
								p.sendMessage(Text.of(MsgUtil.getMessage("blacklisted-item")));
								return;
							}

							if (args.length < 2) {
								// Send creation menu.
								Info info = new Info(b.getLocation(), ShopAction.CREATE,
										p.getInventory().getItemInMainHand(),
										b.getRelative(p.getFacing().getOppositeFace()));
								plugin.getShopManager().getActions().put(p.getUniqueId(), info);
								p.sendMessage(
										Text.of(MsgUtil.getMessage("how-much-to-trade-for", Util.getName(info.getItem()))));
							} else {
								plugin.getShopManager().handleChat(p, args[1]);
							}
							return;
						}
					}
				} else {
					sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
				}
			} else {
				sender.sendMessage(Text.of(MsgUtil.getMessage("no-anythings-in-your-hand")));
			}
		} else {
			sender.sendMessage(Text.of("This command can't be run by console"));
		}
		return;
	}
	private void amount(CommandSource sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(Text.of("Missing amount"));
			return;
		}

		if (sender instanceof Player) {
			final Player player = (Player) sender;
			if (!plugin.getShopManager().getActions().containsKey(player.getUniqueId())) {
				sender.sendMessage(Text.of("You do not have any pending action!"));
				return;
			}
			plugin.getShopManager().handleChat(player, args[1]);
		} else {
			sender.sendMessage(Text.of("This command can't be run by console"));
		}
		return;
	}
	private void silentBuy(CommandSource sender, String[] args) {
		if (sender.hasPermission("quickshop.create.buy")) {
			Shop shop = plugin.getShopManager().getShop(new Location<World>(Sponge.getServer().getWorld(args[1]).get(), Integer.valueOf(args[2]),
					Integer.valueOf(args[3]), Integer.valueOf(args[4])));
			if (shop != null && shop.getOwner().equals(((Player) sender).getUniqueId())) {
				shop.setShopType(ShopType.BUYING);
				shop.setSignText();
				shop.update();
				MsgUtil.sendControlPanelInfo(sender, shop);
				sender.sendMessage(Text.of(MsgUtil.getMessage("command.now-buying", shop.getDataName())));
				return;
			}
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	private void setBuy(CommandSource sender) {
		if (sender instanceof Player && sender.hasPermission("quickshop.create.buy")) {
			BlockIterator bIt = new BlockIterator((Living) (Player) sender, 10);
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null && shop.getOwner().equals(((Player) sender).getUniqueId())) {
					shop.setShopType(ShopType.BUYING);
					shop.setSignText();
					shop.update();
					sender.sendMessage(Text.of(MsgUtil.getMessage("command.now-buying", shop.getDataName())));
					return;
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop")));
			return;
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	private void silentSell(CommandSource sender, String[] args) {
		if (sender.hasPermission("quickshop.create.sell")) {
				Shop shop = plugin.getShopManager().getShop(new Location<World>(Sponge.getServer().getWorld(args[1]).get(),
						Integer.valueOf(args[2]), Integer.valueOf(args[3]), Integer.valueOf(args[4])));
				if (shop != null && shop.getOwner().equals(((Player) sender).getUniqueId())) {
					shop.setShopType(ShopType.SELLING);
					shop.setSignText();
					shop.update();
					MsgUtil.sendControlPanelInfo(sender, shop);
					sender.sendMessage(Text.of(MsgUtil.getMessage("command.now-selling", shop.getDataName())));
					return;
				}
			
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	private void setSell(CommandSource sender) {
		if (sender instanceof Player && sender.hasPermission("quickshop.create.sell")) {
			BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null && shop.getOwner().equals(((Player) sender).getUniqueId())) {
					shop.setShopType(ShopType.SELLING);
					shop.setSignText();
					shop.update();
					sender.sendMessage(Text.of(MsgUtil.getMessage("command.now-selling", shop.getDataName())));
					return;
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop")));
			return;
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	private void setPrice(CommandSource sender, String[] args) {
		if (sender instanceof Player && sender.hasPermission("quickshop.create.changeprice")) {
			Player p = (Player) sender;
			if (args.length < 2) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("no-price-given")));
				return;
			}
			double price;
			try {
				price = Double.parseDouble(args[1]);
			} catch (NumberFormatException e) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("thats-not-a-number")));
				return;
			}
			if (price < 0.01) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("price-too-cheap")));
				return;
			}
			double fee = 0;
			if (plugin.priceChangeRequiresFee) {
				fee = plugin.getConfig().getDouble("shop.fee-for-price-change");
				if (fee > 0 && plugin.getEcon().getBalance(p.getUniqueId()) < fee) {
					sender.sendMessage(Text.of(
							MsgUtil.getMessage("you-cant-afford-to-change-price", plugin.getEcon().format(fee))));
					return;
				}
			}
			BlockIterator bIt = new BlockIterator(p, 10);
			// Loop through every block they're looking at upto 10 blocks away
			while (bIt.hasNext()) {
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if (shop != null && (shop.getOwner().equals(((Player) sender).getUniqueId())
						|| sender.hasPermission("quickshop.other.price"))) {
					if (shop.getPrice() == price) {
						// Stop here if there isn't a price change
						sender.sendMessage(Text.of(MsgUtil.getMessage("no-price-change")));
						return;
					}
					if (fee > 0) {
						if (!plugin.getEcon().withdraw(p.getUniqueId(), fee)) {
							sender.sendMessage(Text.of(MsgUtil.getMessage("you-cant-afford-to-change-price",
									plugin.getEcon().format(fee))));
							return;
						}
						sender.sendMessage(Text.of(
								MsgUtil.getMessage("fee-charged-for-price-change", plugin.getEcon().format(fee))));
						try {
									plugin.getEcon().deposit(Util.getOfflinePlayer(plugin.getConfig().getString("tax-account")).get().getUniqueId(), fee);
						} catch (Exception e) {
							e.getMessage();
							plugin.getLogger().warn(
									"QuickShop can't pay tax to account in config.yml,Please set tax account name to a exist player!");
						}

					}
					// Update the shop
					shop.setPrice(price);
					shop.setSignText();
					shop.update();
					sender.sendMessage(Text.of(MsgUtil.getMessage("price-is-now", plugin.getEcon().format(shop.getPrice()))));
					// Chest shops can be double shops.
					if (shop instanceof ContainerShop) {
						ContainerShop cs = (ContainerShop) shop;
						if (cs.isDoubleShop()) {
							Shop nextTo = cs.getAttachedShop();
							if (cs.isSelling()) {
								if (cs.getPrice() < nextTo.getPrice()) {
									sender.sendMessage(Text.of(MsgUtil.getMessage("buying-more-than-selling")));
								}
							} else {
								// Buying
								if (cs.getPrice() > nextTo.getPrice()) {
									sender.sendMessage(Text.of(MsgUtil.getMessage("buying-more-than-selling")));
								}
							}
						}
					}
					return;
				}
			}
			sender.sendMessage(Text.of(MsgUtil.getMessage("not-looking-at-shop")));
			return;
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	private void clean(CommandSource sender) {
		if (sender.hasPermission("quickshop.clean")) {
			sender.sendMessage(Text.of(MsgUtil.getMessage("command.cleaning")));
			Iterator<Shop> shIt = plugin.getShopManager().getShopIterator();
			int i = 0;
			while (shIt.hasNext()) {
				Shop shop = shIt.next();

				try {
					if (shop.getLocation().getExtent() != null && shop.isSelling() && shop.getRemainingStock() == 0
							&& shop instanceof ContainerShop) {
						ContainerShop cs = (ContainerShop) shop;
						if (cs.isDoubleShop())
							continue;
						shIt.remove(); // Is selling, but has no stock, and is a chest shop, but is not a double shop.
										// Can be deleted safely.
						i++;
					}
				} catch (IllegalStateException e) {
					shIt.remove(); // The shop is not there anymore, remove it
				}
			}
			MsgUtil.clean();
			sender.sendMessage(Text.of(MsgUtil.getMessage("command.cleaned", "" + i)));
			return;
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	private void about(CommandSource sender) {
		sender.sendMessage(Text.of("[QuickShop] About QuickShop"));
		sender.sendMessage(Text.of("[QuickShop] Hello, I'm Ghost_chu Author of QS reremake."));
		sender.sendMessage(Text.of("[QuickShop] This plugin is remake by SunnySide Community."));
		sender.sendMessage(Text.of("[QuickShop] Original author is KaiNoMood. This is QS unofficial version."));
		sender.sendMessage(Text.of("[QuickShop] Have more feature, and design for 1.13 and higher version."));
		sender.sendMessage(Text.of("[QuickShop] You can see our SpigotMC page to read more:"));
		sender.sendMessage(Text.of("[QuickShop] https://www.spigotmc.org/resources/quickshop-reremake-for-1-13.62575/"));
		sender.sendMessage(Text.of("[QuickShop] Thanks for use QuickShop-Reremake."));
	}
	private void reload(CommandSource sender) {
		if (sender.hasPermission("quickshop.reload")) {
			sender.sendMessage(Text.of(MsgUtil.getMessage("command.reloading")));
			Bukkit.getPluginManager().disablePlugin(plugin);
			Bukkit.getPluginManager().enablePlugin(plugin);
			return;
		}
		sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		return;
	}
	@Override
	public boolean onCommand(CommandSource sender, Command cmd, String commandLabel, String[] args) {
		if (args.length > 0) {
			String subArg = args[0].toLowerCase();
			if (subArg.equals("unlimited")) {
				setUnlimited(sender);
				return true;
			} else if (subArg.startsWith("silentunlimited")) {
				silentUnlimited(sender, args);
				return true;
			} else if (subArg.equals("setowner")) {
				setOwner(sender, args);
				return true;
			} else if (subArg.equals("find")) {
				find(sender, args);
				return true;
			} else if (subArg.startsWith("create")) {
				create(sender, args);
				return true;
			} else if (subArg.startsWith("amount")) {
				amount(sender, args);
				return true;
			} else if (subArg.startsWith("buy")) {
				setBuy(sender);
				return true;
			} else if (subArg.startsWith("silentbuy")) {
				silentBuy(sender, args);
				return true;
			} else if (subArg.startsWith("sell")) {
				setSell(sender);
				return true;
			} else if (subArg.startsWith("silentsell")) {
				silentSell(sender, args);
				return true;
			} else if (subArg.startsWith("price")) {
				setPrice(sender, args);
				return true;
			} else if (subArg.equals("remove")) {
				remove(sender, args);
				return true;
			} else if (subArg.startsWith("silentremove")) {
				silentRemove(sender, args);
				return true;
			} else if (subArg.equals("refill")) {
				refill(sender, args);
				return true;
			} else if (subArg.equals("empty")) {
				empty(sender, args);
				return true;
			} else if (subArg.startsWith("silentempty")) {
				silentEmpty(sender, args);
				return true;
			} else if (subArg.equals("clean")) {
				clean(sender);
				return true;
			} else if (subArg.equals("reload")) {
				reload(sender);
				return true;
			} else if (subArg.equals("export")) {
				//export(sender, args);
				return true;
			} else if (subArg.equals("about")) {
				about(sender);
				return true;
			} else if (subArg.equals("remove")) {
				remover(sender, args);
				return true;
			} else if (subArg.equals("debug")) {
				debug(sender, args);
				return true;
			} else if (subArg.equals("fetchmessage")) {
				fetchMessage(sender, args);
				return true;
			} else if (subArg.equals("info")) {
				info(sender,args);
				return true;
			}
		} else {
			// Invalid arg given
			sendHelp(sender);
			return true;
		}
		// No args given
		sendHelp(sender);
		return true;
	}
	private void debug(CommandSource sender, String[] args) {
		boolean debug = plugin.getConfig().getBoolean("dev-mode");
		if(debug) {
			plugin.getConfig().set("dev-mode", false);
			plugin.saveConfig();
			sender.sendMessage(Text.of(MsgUtil.getMessage("command.now-nolonger-debuging")));
			reload(sender);
		}else {
			plugin.getConfig().set("dev-mode", true);
			plugin.saveConfig();
			sender.sendMessage(Text.of(MsgUtil.getMessage("command.now-debuging")));
			reload(sender);
		}
	}

	private void info(CommandSource sender, String[] args) {
		if (sender.hasPermission("quickshop.info")) {
			int buying, selling, doubles, chunks, worlds;
			buying = selling = doubles = chunks = worlds = 0;
			int nostock = 0;
			for (HashMap<ShopChunk, HashMap<Location<World>, Shop>> inWorld : plugin.getShopManager().getShops()
					.values()) {
				worlds++;
				for (HashMap<Location<World>, Shop> inChunk : inWorld.values()) {
					chunks++;
					for (Shop shop : inChunk.values()) {
						if (shop.isBuying()) {
							buying++;
						} else if (shop.isSelling()) {
							selling++;
						}
						if (shop instanceof ContainerShop && ((ContainerShop) shop).isDoubleShop()) {
							doubles++;
						} else if (shop.isSelling() && shop.getRemainingStock() == 0) {
							nostock++;
						}
					}
				}
			}
			sender.sendMessage(Text.of(TextColors.RED + "QuickShop Statistics..."));
			sender.sendMessage(Text.of(TextColors.GREEN + "" + (buying + selling) + " shops in " + chunks
					+ " chunks spread over " + worlds + " worlds."));
			sender.sendMessage(Text.of(TextColors.GREEN + "" + doubles + " double shops. "));
			sender.sendMessage(Text.of(TextColors.GREEN + "" + nostock
					+ " nostock selling shops (excluding doubles) which will be removed by /qs clean."));
			sender.sendMessage(Text.of(TextColors.GREEN + "QuickShop "+QuickShop.getVersion()));
		}else {
			sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
		}
	}


	private void remover(CommandSource sender, String[] args) {
		if (args.length < 5) {
			return;
		}
		ShopManager manager = plugin.getShopManager();
		try {
			Shop shop = manager.getShop(new Location<World>(Sponge.getServer().getWorld(args[4]).get(), Integer.parseInt(args[1]),
					Integer.parseInt(args[2]), Integer.parseInt(args[3])));
			if (shop == null) {
				sender.sendMessage(Text.of(MsgUtil.getMessage("shop-not-exist")));
				return;
			}
			if(sender instanceof Player) {
				Player player = (Player)sender;
				if(!(shop.getOwner()!=player.getUniqueId())&&!(player.hasPermission("quickshop.other.destroy"))) {
					sender.sendMessage(Text.of(MsgUtil.getMessage("no-permission")));
					return;
				}
			}
			DatabaseHelper.removeShop(plugin.getDB(), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
					Integer.parseInt(args[3]), args[4]);
			shop.onUnload();
			sender.sendMessage(Text.of(MsgUtil.getMessage("success-removed-shop")));
		} catch (NumberFormatException | SQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Returns loc with modified pitch/yaw angles so it faces lookat
	 * 
	 * @param loc    The location a players head is
	 * @param lookat The location they should be looking
	 * @return The location the player should be facing to have their crosshairs on
	 *         the location lookAt Kudos to bergerkiller for most of this function
	 */
	public Location<World> lookAt(Location<World> loc, Location<World> lookat) {
		// Clone the loc to prevent applied changes to the input loc
		loc = loc.copy();
		// Values of change in distance (make it relative)
		double dx = lookat.getX() - loc.getX();
		double dy = lookat.getY() - loc.getY();
		double dz = lookat.getZ() - loc.getZ();
		// Set yaw
		if (dx != 0) {
			// Set yaw start value based on dx
			if (dx < 0) {
				loc.setYaw((float) (1.5 * Math.PI));
			} else {
				loc.setYaw((float) (0.5 * Math.PI));
			}
			loc.setYaw((float) loc.getYaw() - (float) Math.atan(dz / dx));
		} else if (dz < 0) {
			loc.setYaw((float) Math.PI);
		}
		// Get the distance from dx/dz
		double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));
		float pitch = (float) -Math.atan(dy / dxz);
		// Set values, convert to degrees
		// Minecraft yaw (vertical) angles are inverted (negative)
		loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI + 360);
		// But pitch angles are normal
		loc.setPitch(pitch * 180f / (float) Math.PI);
		return loc;
	}
	public void sendDebugInfomation(CommandSource s) {
		s.sendMessage(Text.of("Running " + plugin.getDescription().getVersion() + " on server "
				+ plugin.getServer().getVersion() + " for Bukkit " + plugin.getServer().getBukkitVersion())));
		try {
			s.sendMessage(Text.of("Database: " + plugin.getDB().getConnection().getMetaData().getDatabaseProductName() + " @ "
					+ plugin.getDB().getConnection().getMetaData().getDatabaseProductVersion()));
		} catch (SQLException e) {
			s.sendMessage(Text.of("Database: A error happed when getting data.");
			e.printStackTrace();
		}
	}
	public void sendHelp(CommandSource s) {

		s.sendMessage(Text.of(MsgUtil.getMessage("command.description.title")));
		if (s.hasPermission("quickshop.unlimited"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs unlimited" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.unlimited")));
		if (s.hasPermission("quickshop.setowner"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs setowner <player>" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.setowner")));
		if (s.hasPermission("quickshop.create.buy"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs buy" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.buy")));
		if (s.hasPermission("quickshop.create.sell")) {
			s.sendMessage(Text.of(TextColors.GREEN + "/qs sell" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.sell")));
			s.sendMessage(Text.of(TextColors.GREEN + "/qs create [price]" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.create")));
		}
		if (s.hasPermission("quickshop.create.changeprice"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs price" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.price")));
		if (s.hasPermission("quickshop.clean"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs clean" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.clean")));
		if (s.hasPermission("quickshop.find"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs find <item>" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.find")));
		if (s.hasPermission("quickshop.refill"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs refill <amount>" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.refill")));
		if (s.hasPermission("quickshop.empty"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs empty" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.empty")));
		if (s.hasPermission("quickshop.fetchmessage"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs fetchmessage" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.fetchmessage")));
		if (s.hasPermission("quickshop.info"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs info" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.info")));
		if (s.hasPermission("quickshop.debug"))
			s.sendMessage(Text.of(TextColors.GREEN + "/qs debug" + TextColors.YELLOW + " - "
					+ MsgUtil.getMessage("command.description.debug")));
//		if (s.hasPermission("quickshop.export"))
//			s.sendMessage(Text.of(TextColors.GREEN + "/qs export mysql|sqlite" + TextColors.YELLOW + " - "
//					+ MsgUtil.getMessage("command.description.export"));
	}


	@Override
	public CommandResult process(CommandSource source, String arguments) throws CommandException {
		// TODO Auto-generated method stub
		// TODO Do process there
		return null;
	}


	@Override
	public List<String> getSuggestions(CommandSource source, String arguments, Location<World> targetPosition)
			throws CommandException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean testPermission(CommandSource source) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public Optional<Text> getShortDescription(CommandSource source) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Optional<Text> getHelp(CommandSource source) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Text getUsage(CommandSource source) {
		// TODO Auto-generated method stub
		return null;
	}
	
}