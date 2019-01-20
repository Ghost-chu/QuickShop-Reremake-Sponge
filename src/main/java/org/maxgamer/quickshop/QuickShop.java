package org.maxgamer.quickshop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.maxgamer.quickshop.Command.QS;
import org.maxgamer.quickshop.Command.Tab;
import org.maxgamer.quickshop.Database.*;
import org.maxgamer.quickshop.Database.Database.ConnectionException;
import org.maxgamer.quickshop.Economy.Economy;
import org.maxgamer.quickshop.Economy.EconomyCore;
import org.maxgamer.quickshop.Economy.Economy_Sponge;
import org.maxgamer.quickshop.Listeners.*;
import org.maxgamer.quickshop.Shop.ContainerShop;
import org.maxgamer.quickshop.Shop.Shop;
import org.maxgamer.quickshop.Shop.ShopManager;
import org.maxgamer.quickshop.Shop.ShopType;
import org.maxgamer.quickshop.Util.MsgUtil;
import org.maxgamer.quickshop.Util.Util;
import org.maxgamer.quickshop.Watcher.ItemWatcher;
import org.maxgamer.quickshop.Watcher.LogWatcher;
import org.maxgamer.quickshop.Watcher.UpdateWatcher;
import org.slf4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.yaml.snakeyaml.Yaml;

import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.swing.LayoutFocusTraversalPolicy;
@Plugin(id = "QuickShop", name = "QuickShop", version = "0.1", description = "QuickShop-Reremake", authors= {"Netherfoam", "Timtower", "KaiNoMood","Ghost_chu", "Mgazul"})
public class QuickShop {
	@Inject
    private Logger logger;
	/** The active instance of QuickShop */
	public static QuickShop instance;
	/** The economy we hook into for transactions */
	private Economy economy;
	/** The Shop Manager used to store shops */
	private ShopManager shopManager;
	/**
	 * A set of players who have been warned ("Your shop isn't automatically
	 * locked")
	 */
	public HashSet<String> warnings = new HashSet<String>();
	/** The database for storing all our data for persistence */
	private Database database;
	// Listeners - We decide which one to use at runtime
	private ChatListener chatListener;
	// Listeners (These don't)
	private BlockListener blockListener;
	private PlayerListener playerListener;
	private DisplayProtectionListener inventoryListener;
	private ChunkListener chunkListener;
	private WorldListener worldListener;
	private BukkitTask itemWatcherTask;
	private LogWatcher logWatcher;
	/** Whether players are required to sneak to create/buy from a shop */
	public boolean sneak;
	/** Whether players are required to sneak to create a shop */
	public boolean sneakCreate;
	/** Whether players are required to sneak to trade with a shop */
	public boolean sneakTrade;
	/** Whether we should use display items or not */
	public boolean display = true;
	/**
	 * Whether we players are charged a fee to change the price on their shop (To
	 * help deter endless undercutting
	 */
	public boolean priceChangeRequiresFee = false;
	/** Whether or not to limit players shop amounts */
	public boolean limit = false;

	/** The plugin OpenInv (null if not present) */
	public Plugin openInvPlugin;
	private HashMap<String, Integer> limits = new HashMap<String, Integer>();
	/** Use SpoutPlugin to get item / block names */
	public boolean useSpout = false;
	// private Metrics metrics;
	QS commandExecutor =null;
	private int displayItemCheckTicks;
	private boolean noopDisable;
	private boolean setupDBonEnableding = false;
	private String dbPrefix="";
	private Tab commandTabCompleter;
	private Metrics metrics;
	private Configuration configuration;
	/** 
	 * Get the Player's Shop limit.
	 * @return int Player's shop limit
	 * @param Player p
	 * */
	public int getShopLimit(Player p) {
		int max = getConfig().getInt("limits.default");
		for (Entry<String, Integer> entry : limits.entrySet()) {
			if (entry.getValue() > max && p.hasPermission(entry.getKey()))
				max = entry.getValue();
		}
		return max;
	}
	public Logger getLogger() {
		return this.logger;
	}
	public Server getServer() {
		return Sponge.getServer();
	}
	@Listener
	public void onServerStart() {
		instance = this;
		configuration = new Configuration();
		configuration.setupConfig();
		getLogger().info("Quickshop Reremake by Ghost_chu(Minecraft SunnySide Server Community)");
		getLogger().info("THIS VERSION ONLY SUPPORT BUKKIT API 1.13-1.13.x VERSION!");
		getLogger().info("Author:Ghost_chu");
		getLogger().info("Original author:Netherfoam, Timtower, KaiNoMood");
		getLogger().info("Let's us start load plugin");
		// NMS.init();
		Sponge.getConfigManager().
		saveDefaultConfig(); // Creates the config folder and copies config.yml
								// (If one doesn't exist) as required.
		reloadConfig(); // Reloads messages.yml too, aswell as config.yml and
						// others.
		getConfig().options().copyDefaults(true); // Load defaults.
		if (Util.isDevEdition()) {
			getLogger().error("WARNING: You are running QSRR on dev-mode");
			getLogger().error("WARNING: Keep backup and DO NOT running on production environment!");
			getLogger().error("WARNING: Test version may destory anything!");
			getLogger().error(
					"WARNING: QSRR won't start without you confirm, nothing will changes before you turn on dev allowed.");
			if (!getConfig().getBoolean("dev-mode")) {
				getLogger().error(
						"WARNING: Set dev-mode: true in config.yml to allow qs load on dev mode(Maybe need add this line by your self).");
				noopDisable = true;
				Bukkit.getPluginManager().disablePlugin(this);
				return;
			}
		}
		if (loadEcon() == false)
			return;
		// ProtocolLib Support
		// protocolManager = ProtocolLibrary.getProtocolManager();
//		try {
//			getServer().spigot();
//		} catch (Throwable e) {
//			getLogger().error("You must use support Spigot or Spigot forks(eg.Paper) server not CraftBukkit");
//			Bukkit.getPluginManager().disablePlugin(this);
//			return;
//		}
//		if (getConfig().getBoolean("plugin.Multiverse-Core")) {
//			if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
//				mPlugin = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
//				getLogger().info("Successfully loaded MultiverseCore support!");
//			}
//		}
		// added for compatibility reasons with OpenInv - see
		// https://github.com/KaiKikuchi/QuickShop/issues/139
//		if (getConfig().getBoolean("plugin.OpenInv")) {
//			this.openInvPlugin = Bukkit.getPluginManager().getPlugin("OpenInv");
//			if (this.openInvPlugin != null)
//				getLogger().info("Successfully loaded OpenInv support!");
//		}
		if (getConfig().getInt("config-version") == 0)
			getConfig().set("config-version", 1);
		updateConfig(getConfig().getInt("config-version"));
		// Initialize Util
		Util.initialize();
		// Create the shop manager.
		this.shopManager = new ShopManager(this);
		if (this.display) {
			// Display item handler thread
			getLogger().info("Starting item scheduler");
			ItemWatcher itemWatcher = new ItemWatcher(this);
			itemWatcherTask = Bukkit.getScheduler().runTaskTimer(this, itemWatcher, 600, 600);
		}
		if (this.getConfig().getBoolean("log-actions")) {
			// Logger Handler
			this.logWatcher = new LogWatcher(this, new File(this.getDataFolder(), "qs.log"));
			logWatcher.task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.logWatcher, 150, 150);
		}
		if (getConfig().getBoolean("shop.lock")) {
			LockListener ll = new LockListener(this);
			Sponge.getEventManager().registerListeners(this,ll);
		}
		Sponge.getEventManager().registerListeners(this,new UpdateWatcher());
		ConfigurationSection limitCfg = this.getConfig().getConfigurationSection("limits");
		if (limitCfg != null) {
			getLogger().info("Limit cfg found...");
			this.limit = limitCfg.getBoolean("use", false);
			getLogger().info("Limits.use: " + limit);
			limitCfg = limitCfg.getConfigurationSection("ranks");
			for (String key : limitCfg.getKeys(true)) {
				limits.put(key, limitCfg.getInt(key));
			}
		}
		setupDBonEnableding = true;
		setupDatabase();
		setupDBonEnableding = false;
		/* Load shops from database to memory */
		int count = 0; // Shops count
		MsgUtil.loadItemi18n();
		MsgUtil.loadEnchi18n();
		MsgUtil.loadPotioni18n();
		try {
			getLogger().info("Loading shops from database...");
			ResultSet rs = DatabaseHelper.selectAllShops(database);
			int errors = 0;

			boolean isBackuped = false;

			while (rs.next()) {
				int x = 0;
				int y = 0;
				int z = 0;
				String worldName = null;
				ItemStack item = null;
				String owner = null;
				UUID ownerUUID = null;
				String step = "while init";
				// ==========================================================================================
				try {
					x = rs.getInt("x");
					y = rs.getInt("y");
					z = rs.getInt("z");
					worldName = rs.getString("world");
					//World world = Bukkit.getWorld(worldName);
					World world = Sponge.getServer().getWorld(worldName).get();
//					if (world == null && mPlugin != null) {
//						// Maybe world not loaded? Try call MV to load world.
//						mPlugin.getCore().getMVWorldManager().loadWorld(worldName);
//						world = Bukkit.getWorld(worldName);
//					}
					item = Util.deserialize(rs.getString("itemConfig"));
					owner = rs.getString("owner");
					ownerUUID = null;
					step = "Covert owner to UUID";
					try {
						ownerUUID = UUID.fromString(owner);
					} catch (IllegalArgumentException e) {
						// This could be old data to be converted... check if it's a player
						step = "Update owner to UUID";
						// Because need update database, so use crossed method, Set ignore.
						@SuppressWarnings("deprecation")
						OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
						if (player.hasPlayedBefore()) {
							ownerUUID = player.getUniqueId();
							DatabaseHelper.updateOwner2UUID(ownerUUID.toString(), x, y, z, worldName);
						} else {
							// Invalid shop owner
							DatabaseHelper.removeShop(database, x, y, z, worldName);
							continue;
						}
					}
					step = "Loading shop price";
					double price = rs.getDouble("price");
					step = "Createing Location object";
					Location loc = new Location(world, x, y, z);
					/* Skip invalid shops, if we know of any */
					step = "Checking InventoryHolder";
					if (world != null && Util.canBeShop(loc.getBlock(),null,true) == false) {
						step = "Removeing shop in world: Because it not a correct InventoryHolder";
						getLogger().info("Shop is not an InventoryHolder in " + rs.getString("world") + " at: " + x
								+ ", " + y + ", " + z + ".  Deleting.");
						DatabaseHelper.removeShop(database, x, y, z, worldName);
						continue;
					}
					step = "Loading shop type";
					int type = rs.getInt("type");
					step = "Loading shop in world";
					Shop shop = new ContainerShop(loc, price, item, ownerUUID);
					step = "Setting shop unlitmited status";
					shop.setUnlimited(rs.getBoolean("unlimited"));
					step = "Setting shop type";
					shop.setShopType(ShopType.fromID(type));
					step = "Loading shop to memory";
					shopManager.loadShop(rs.getString("world"), shop);
					if (loc.getWorld() != null && loc.getChunk().isLoaded()) {
						step = "Loading shop to memory >> Chunk loaded, Loaded to memory";
						shop.onLoad();
						shop.setSignText();
					} else {
						step = "Loading shop to memory >> Chunk not loaded, Skipping";
					}
					step = "Finish";
					count++;
				} catch (Exception e) {
					errors++;
					getLogger().error("Error loading a shop! Coords: Location[" + worldName + " (" + x + ", " + y
							+ ", " + z + ")] Item: " + item.getType().getName() + "...");
					getLogger().error("Are you deleted world included QuickShop shops? All shops will auto fixed.");

					getLogger().error("===========Error Reporting Start===========");
					getLogger().error("#Java throw >>");
					getLogger().error("StackTrace:");
					e.printStackTrace();
					getLogger().error("#Shop data >>");
					getLogger().error("Location: " + worldName + ";(X:" + x + ", Y:" + y + ", Z:" + z + ")");
					getLogger().error(
							"Item: " + item.getType().getName() + " MetaData: " + item.getItemMeta().spigot().toString());
					getLogger().error("Owner: " + owner + "(" + ownerUUID.toString() + ")");
					try {
						getLogger().error(
								"BukkitWorld: " + Sponge.getServer().getWorld(worldName).get().getName() + " [" + worldName + "]");
					} catch (Exception e2) {
						getLogger().error("BukkitWorld: WARNING:World not exist! [" + worldName + "]");
					}
					try {
						getLogger().error(
								"Target Block: " +  Sponge.getServer().getWorld(worldName).get().getBlock(x, y, z).getType().getName());
					} catch (Exception e2) {
						getLogger().error("Target Block: Can't get block!");
					}
					getLogger().error("#Database info >>");

					getLogger().error("Connected:" + !getDB().getConnection().isClosed());
					getLogger().error("Read Only:" + getDB().getConnection().isReadOnly());

					if (getDB().getConnection().getClientInfo() != null) {
						getLogger().error("Client Info: " + getDB().getConnection().getClientInfo().toString());
					} else {
						getLogger().error("Client Info: null");
					}
					getLogger().error("Read Only:" + getDB().getConnection().isReadOnly());

					getLogger().error("#Debuging >>");
					getLogger().error("Runnnig on step: " + step);

					getLogger().error("#Tips >>");
					getLogger().error("Please report this issues to author, And you database will auto backup!");

					if (!isBackuped) {
						File sqlfile = new File(QuickShop.instance.configuration.config.getAbsolutePath().toString() + "/shop.db");
						if (!sqlfile.exists()) {
							getLogger().error("Failed to backup! (File not found)");
						}
						String uuid = UUID.randomUUID().toString().replaceAll("_", "");
						File bksqlfile = new File(Bukkit.getPluginManager().getPlugin("QuickShop").getDataFolder()
								.getAbsolutePath().toString() + "/shop_backup_" + uuid + ".db");
						try {
							bksqlfile.createNewFile();
						} catch (IOException e1) {

						}
						FileChannel inputChannel = null;
						FileChannel outputChannel = null;
						try {
							inputChannel = new FileInputStream(sqlfile).getChannel();
							outputChannel = new FileOutputStream(bksqlfile).getChannel();
							outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
							inputChannel.close();
							outputChannel.close();
						} catch (Exception e3) {

							inputChannel = null;
							outputChannel = null;
						}
					}
					getLogger().error("===========Error Reporting End===========");

					if (errors < 3) {
						getLogger().info("Removeing shop from database...");
						DatabaseHelper.removeShop(database, x, y, z, worldName);
						getLogger().info("Trying keep loading...");
					} else {
						getLogger().error(
								"Multiple errors in shops - Something seems to be wrong with your shops database! Please check it out immediately!");
						getLogger().info("Removeing shop from database...");
						DatabaseHelper.removeShop(database, x, y, z, worldName);
						e.printStackTrace();
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().error("Could not load shops Because SQLException.");
		}
		getLogger().info("Loaded " + count + " shops.");
		
		
		if (getConfig().getBoolean("shop.lock")) {
			LockListener lockListener = new LockListener(this);
			Sponge.getEventManager().registerListeners(this,lockListener);
		}
		// Command handlers
		commandExecutor = new QS(this);
		//getCommand("qs").setExecutor(commandExecutor);
		//String childCommand;
//		CommandSpec qsCommandSpec = CommandSpec.builder()
//			    .description(Text.of("QuickShop Main Command"))
//			    .executor(commandExecutor)
//			    .build();
//
//	    Sponge.getCommandManager().register(this, qsCommandSpec, "qs", "qsshop", "qshop","quickshop","shop");
	    CommandManager cmdManager = Sponge.getCommandManager();
	    cmdManager.register(this, commandExecutor, "qs", "qsshop", "qshop","quickshop","shop");
		commandTabCompleter = new Tab(this);
		Sponge.getEventManager().registerListeners(this,commandTabCompleter);
		if (getConfig().getInt("shop.find-distance") > 100) {
			getLogger().error("Shop.find-distance is too high! It may cause lag! Pick a number under 100!");
		}
		getLogger().info("Registering Listeners");
		// Register events
		blockListener = new BlockListener(this);
		playerListener = new PlayerListener(this);
		worldListener = new WorldListener(this);
		chatListener = new ChatListener(this);
		chunkListener = new ChunkListener(this);
		inventoryListener = new DisplayProtectionListener(this);
		Sponge.getEventManager().registerListeners(this,blockListener);
		Sponge.getEventManager().registerListeners(this,playerListener);
		Sponge.getEventManager().registerListeners(this,chatListener);
		Sponge.getEventManager().registerListeners(this,inventoryListener);
		Sponge.getEventManager().registerListeners(this,chunkListener);
		Sponge.getEventManager().registerListeners(this,worldListener);
		
		
		if (display && displayItemCheckTicks > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Iterator<Shop> it = getShopManager().getShopIterator();
					while (it.hasNext()) {
						Shop shop = it.next();
						if (shop instanceof ContainerShop) {
							ContainerShop cShop = (ContainerShop) shop;
							if (cShop.checkDisplayMoved()) {
								log("Display item for " + shop
										+ " is not on the correct location and has been removed. Probably someone is trying to cheat.");
								for (Player player : getServer().getOnlinePlayers()) {
									if (player.hasPermission("quickshop.alerts")) {
										player.sendMessage(Color.RED + "[QuickShop] Display item for " + shop
												+ " is not on the correct location and has been removed. Probably someone is trying to cheat.");
									}
								}
								cShop.getDisplayItem().remove();
							}
						}
					}
				}
			}.runTaskTimer(this, 1L, displayItemCheckTicks);
		}
		MsgUtil.loadTransactionMessages();
		MsgUtil.clean();
		getLogger().info("QuickShop loaded!");

		if (getConfig().getBoolean("disabled-metrics") != true) {
			String serverVer = Bukkit.getVersion();
			String bukkitVer = Bukkit.getBukkitVersion();
			String serverName = Bukkit.getServer().getName();
			metrics = new Metrics(this);
			// Use internal Metric class not Maven for solve plugin name issues
			String display_Items;
			if (getConfig().getBoolean("shop.display-items")) { // Maybe mod server use this plugin more? Or have big
																// number items need disabled?
				display_Items = "Enabled";
			} else {
				display_Items = "Disabled";
			}
			String locks;
			if (getConfig().getBoolean("shop.lock")) {
				locks = "Enabled";
			} else {
				locks = "Disabled";
			}
			String sneak_action;
			if (getConfig().getBoolean("shop.sneak-to-create") || getConfig().getBoolean("shop.sneak-to-trade")) {
				sneak_action = "Enabled";
			} else {
				sneak_action = "Disabled";
			}
			String use_protect_minecart;
			if (getConfig().getBoolean("protect.minecart")) {
				use_protect_minecart = "Enabled";
			} else {
				use_protect_minecart = "Disabled";
			}
			String use_protect_entity;
			if (getConfig().getBoolean("protect.entity")) {
				use_protect_entity = "Enabled";
			} else {
				use_protect_entity = "Disabled";
			}
			String use_protect_redstone;
			if (getConfig().getBoolean("protect.redstone")) {
				use_protect_redstone = "Enabled";
			} else {
				use_protect_redstone = "Disabled";
			}
			String use_protect_structuregrow;
			if (getConfig().getBoolean("protect.structuregrow")) {
				use_protect_structuregrow = "Enabled";
			} else {
				use_protect_structuregrow = "Disabled";
			}
			String use_protect_explode;
			if (getConfig().getBoolean("protect.explode")) {
				use_protect_explode = "Enabled";
			} else {
				use_protect_explode = "Disabled";
			}
			String use_protect_hopper;
			if (getConfig().getBoolean("protect.hopper")) {
				use_protect_hopper = "Enabled";
			} else {
				use_protect_hopper = "Disabled";
			}
			String shop_find_distance = getConfig().getString("shop.find-distance");
			// Version
			metrics.addCustomChart(new Metrics.SimplePie("server_version", () -> serverVer));
			metrics.addCustomChart(new Metrics.SimplePie("bukkit_version", () -> bukkitVer));
			metrics.addCustomChart(new Metrics.SimplePie("server_name", () -> serverName));
			metrics.addCustomChart(new Metrics.SimplePie("use_display_items", () -> display_Items));
			metrics.addCustomChart(new Metrics.SimplePie("use_locks", () -> locks));
			metrics.addCustomChart(new Metrics.SimplePie("use_sneak_action", () -> sneak_action));
			metrics.addCustomChart(new Metrics.SimplePie("use_protect_minecart", () -> use_protect_minecart));
			metrics.addCustomChart(new Metrics.SimplePie("use_protect_entity", () -> use_protect_entity));
			metrics.addCustomChart(new Metrics.SimplePie("use_protect_redstone", () -> use_protect_redstone));
			metrics.addCustomChart(new Metrics.SimplePie("use_protect_structuregrow", () -> use_protect_structuregrow));
			metrics.addCustomChart(new Metrics.SimplePie("use_protect_explode", () -> use_protect_explode));
			metrics.addCustomChart(new Metrics.SimplePie("use_protect_hopper", () -> use_protect_hopper));
			metrics.addCustomChart(new Metrics.SimplePie("shop_find_distance", () -> shop_find_distance));
			// Exp for stats, maybe i need improve this, so i add this.
			metrics.submitData(); // Submit now!
			getLogger().info("Mertics submited.");
		} else {
			getLogger().info("You have disabled mertics, Skipping...");
		}

		UpdateWatcher.init();
	}
	

	private boolean setupDatabase() {
		try {
			ConfigurationSection dbCfg = getConfig().getConfigurationSection("database");
			DatabaseCore dbCore;
			if (dbCfg.getBoolean("mysql")) {
				// MySQL database - Required database be created first.
				dbPrefix = dbCfg.getString("prefix");
				if (dbPrefix==null || dbPrefix.equals("none"))
					dbPrefix = "";
				String user = dbCfg.getString("user");
				String pass = dbCfg.getString("password");
				String host = dbCfg.getString("host");
				String port = dbCfg.getString("port");
				String database = dbCfg.getString("database");
				dbCore = new MySQLCore(host, user, pass, database, port);
			} else {
				// SQLite database - Doing this handles file creation
				dbCore = new SQLiteCore(new File(getConfiguration().getDataFolder(), "shops.db"));
			}
			this.database = new Database(dbCore);
			// Make the database up to date
			DatabaseHelper.setup(getDB());
		} catch (ConnectionException e) {
			e.printStackTrace();
			if (setupDBonEnableding) {
				getLogger().error("Error connecting to database. Aborting plugin load.");
				Sponge.getServer().shutdown(Text.of("Error connecting to database. Aborting plugin load."));
				return false;
			} else {
				getLogger().error("Error connecting to database.");
			}
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			if (setupDBonEnableding) {
				getLogger().error("Error setting up database. Aborting plugin load.");
				Sponge.getServer().shutdown(Text.of("Error connecting to database. Aborting plugin load."));
				return false;
			} else {
				getLogger().error("Error setting up database.");
			}
			return false;
		}
		return true;
	}

	public void updateConfig(int selectedVersion) {
		if (selectedVersion == 1) {
			getConfig().set("disabled-metrics", false);
			getConfig().set("config-version", 2);
			selectedVersion = 2;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 2) {
			getConfig().set("protect.minecart", true);
			getConfig().set("protect.entity", true);
			getConfig().set("protect.redstone", true);
			getConfig().set("protect.structuregrow", true);
			getConfig().set("protect.explode", true);
			getConfig().set("protect.hopper", true);
			getConfig().set("config-version", 3);
			selectedVersion = 3;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 3) {
			getConfig().set("shop.alternate-currency-symbol", '$');
			getConfig().set("config-version", 4);
			selectedVersion = 4;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 4) {
			getConfig().set("updater", true);
			getConfig().set("config-version", 5);
			selectedVersion = 5;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 5) {
			getConfig().set("shop.display-item-use-name", true);
			getConfig().set("config-version", 6);
			selectedVersion = 6;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 6) {
			getConfig().set("shop.sneak-to-control", false);
			getConfig().set("config-version", 7);
			selectedVersion = 7;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 7) {
			getConfig().set("database.prefix", "none");
			getConfig().set("database.reconnect", false);
			getConfig().set("database.use-varchar", false);
			getConfig().set("config-version", 8);
			selectedVersion = 8;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 8) {
			getConfig().set("database.use-varchar", true);
			getConfig().set("limits.old-algorithm", false);
			getConfig().set("shop.pay-player-from-unlimited-shop-owner", false);
			getConfig().set("plugin.ProtocolLib", false);
			getConfig().set("plugin.Multiverse-Core", true);
			getConfig().set("shop.ignore-unlimited",false);
			getConfig().set("config-version", 9);
			selectedVersion = 9;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 9) {
			getConfig().set("shop.enable-enderchest",true);
			getConfig().set("config-version", 10);
			selectedVersion = 10;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 10) {
			getConfig().set("shop.pay-player-from-unlimited-shop-owner",null); //Removed
			getConfig().set("config-version", 11);
			selectedVersion = 11;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 11) {
			getConfig().set("shop.enable-enderchest",null); //Removed
			getConfig().set("plugin.OpenInv",true);
			List<String> shoppable = getConfig().getStringList("shop-blocks");
			shoppable.add("ENDER_CHEST");
			getConfig().set("shop-blocks",shoppable);
			getConfig().set("config-version", 12);
			selectedVersion = 12;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 12) {
			getConfig().set("plugin.ProtocolLib",null); //Removed
			getConfig().set("plugin.BKCommonLib",null); //Removed
			getConfig().set("plugin.BKCommonLib",null); //Removed
			getConfig().set("database.use-varchar",null); //Removed
			getConfig().set("database.reconnect",null); //Removed
			getConfig().set("anonymous-metrics", false);
			getConfig().set("display-items-check-ticks", 1200);
			getConfig().set("shop.bypass-owner-check", null); //Removed
			getConfig().set("config-version", 13);
			selectedVersion = 13;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 13) {
			getConfig().set("plugin.AreaShop",false);
			getConfig().set("shop.special-region-only", false);
			getConfig().set("config-version", 14);
			selectedVersion = 14;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 14) {
			getConfig().set("plugin.AreaShop",null);
			getConfig().set("shop.special-region-only", null);
			getConfig().set("config-version", 15);
			selectedVersion = 15;
			saveConfig();
			reloadConfig();
		}
		if (selectedVersion == 15) {
			getConfig().set("ongoingfee",null);
			getConfig().set("shop.display-item-use-name",null);
			getConfig().set("shop.display-item-show-name",false);
			getConfig().set("shop.auto-fetch-shop-messages",true);
			getConfig().set("config-version", 16);
			selectedVersion = 16;
			saveConfig();
			reloadConfig();
		}
	}

	/** Reloads QuickShops config */
	public void reloadQSConfig() {
		this.display = this.getConfig().getBoolean("shop.display-items");
		this.sneak = this.getConfig().getBoolean("shop.sneak-only");
		this.sneakCreate = this.getConfig().getBoolean("shop.sneak-to-create");
		this.sneakTrade = this.getConfig().getBoolean("shop.sneak-to-trade");
		this.priceChangeRequiresFee = this.getConfig().getBoolean("shop.price-change-requires-fee");
		this.displayItemCheckTicks = this.getConfig().getInt("shop.display-items-check-ticks");
		MsgUtil.loadCfgMessages();
	}

	/**
	 * Tries to load the economy and its core. If this fails, it will try to use
	 * vault. If that fails, it will return false.
	 * 
	 * @return true if successful, false if the core is invalid or is not found, and
	 *         vault cannot be used.
	 */
	public boolean loadEcon() {
		try {
			EconomyCore core = new Economy_Sponge();
			if (core == null || !core.isValid()) {
				// getLogger().error("Economy is not valid!");
				getLogger().error("QuickShop could not hook an economy/Not found Vault!");
				getLogger().error("QuickShop CANNOT start!");
				Sponge.getServer().shutdown(Text.of("QuickShop not found any economy plugin"));
				//this.getPluginLoader().disablePlugin(this);
				// if(econ.equals("Vault"))
				// getLogger().error("(Does Vault have an Economy to hook into?!)");
				return false;
			} else {
				this.economy = new Economy(core);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().error("QuickShop could not hook an economy/Not found Vault!");
			getLogger().error("QuickShop CANNOT start!");
			//this.getPluginLoader().disablePlugin(this);
			Sponge.getServer().shutdown(Text.of("QuickShop not found any economy plugin"));
			return false;
		}
	}

	public void onDisable() {
	    if (noopDisable)
	        return;
		if (itemWatcherTask != null) {
			itemWatcherTask.cancel();
		}
		if (logWatcher != null) {
			logWatcher.task.cancel();
			logWatcher.close(); // Closes the file
		}
		/* Unload UpdateWatcher */
		UpdateWatcher.uninit();
		/* Remove all display items, and any dupes we can find */
		shopManager.clear();
		/* Empty the buffer */
		database.close();
		/* Close Database */
		try {
			this.database.getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.warnings.clear();
		this.reloadConfig();
	}

	/**
	 * Returns the economy for moving currency around
	 * 
	 * @return The economy for moving currency around
	 */
	public EconomyCore getEcon() {
		return economy;
	}

	/**
	 * Logs the given string to qs.log, if QuickShop is configured to do so.
	 * 
	 * @param s The string to log. It will be prefixed with the date and time.
	 */
	public void log(String s) {
		if (this.logWatcher == null)
			return;
		Date date = Calendar.getInstance().getTime();
		Timestamp time = new Timestamp(date.getTime());
		this.logWatcher.add("[" + time.toString() + "] " + s);
	}

	/**
	 * @return Returns the database handler for queries etc.
	 */
	public Database getDB() {
		return this.database;
	}

	/**
	 * Returns the ShopManager. This is used for fetching, adding and removing
	 * shops.
	 * 
	 * @return The ShopManager.
	 */
	public ShopManager getShopManager() {
		return this.shopManager;
	}
	
	/**
	 * Returns QS version, this method only exist on QSRR forks If running other
	 * QSRR forks,, result may not is "Reremake x.x.x" If running QS offical, Will
	 * throw exception.
	 * 
	 * @return Plugin Version
	 */
	public static String getVersion() {
		return null;
	}
	
	public BlockListener getBlockListener() {
		return blockListener;
	}
	public ChatListener getChatListener() {
		return chatListener;
	}
	public ChunkListener getChunkListener() {
		return chunkListener;
	}
	public DisplayProtectionListener getInventoryListener() {
		return inventoryListener;
	}
	public PlayerListener getPlayerListener() {
		return playerListener;
	}
	public WorldListener getWorldListener() {
		return worldListener;
	}
//	public MultiverseCore getMVPlugin() {
//		return mPlugin;
//	}
	public Plugin getOpenInvPlugin() {
		return openInvPlugin;
	}
	public String getDbPrefix() {
		return dbPrefix;
	}
	public Tab getCommandTabCompleter() {
		return commandTabCompleter;
	}
	public QS getCommandExecutor() {
		return commandExecutor;
	}
    public Metrics getMetrics() {
		return metrics;
	}
    public YamlConfiguration getConfig() {
    	return configuration.getYamlConfiguration();
    }
    public void saveConfig() {
    	configuration.save();
    }
    public void reloadConfig() {
    	configuration.setupConfig();
    	reloadQSConfig();
    }
    public Configuration getConfiguration() {
		return configuration;
	}
}
