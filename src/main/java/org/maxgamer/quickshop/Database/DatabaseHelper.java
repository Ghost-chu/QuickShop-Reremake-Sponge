package org.maxgamer.quickshop.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Util.Util;
import org.spongepowered.api.item.inventory.ItemStack;

public class DatabaseHelper {
	public static void setup(Database db) throws SQLException {
		if (!db.hasTable(QuickShop.instance.getDbPrefix()+"shops")) {
			createShopsTable(db);
		}
		if (!db.hasTable(QuickShop.instance.getDbPrefix()+"messages")) {
			createMessagesTable(db);
		}
		checkColumns(db);
	}

	/**
	 * Verifies that all required columns exist.
	 */
	public static void checkColumns(Database db) {
		PreparedStatement ps = null;
		try {
			// V3.4.2
			ps = db.getConnection().prepareStatement("ALTER TABLE "+QuickShop.instance.getDbPrefix()+"shops MODIFY COLUMN price double(32,2) NOT NULL AFTER owner");
			ps.execute();
			ps.close();
		} catch (SQLException e) {
		}
		try {
			// V3.4.3
			ps = db.getConnection().prepareStatement("ALTER TABLE "+QuickShop.instance.getDbPrefix()+"messages MODIFY COLUMN time BIGINT(32) NOT NULL AFTER message");
			ps.execute();
			ps.close();
		} catch (SQLException e) {
		}
	}

	/**
	 * Creates the database table 'shops'.
	 * 
	 * @throws SQLException
	 *             If the connection is invalid.
	 */
	public static void createShopsTable(Database db) throws SQLException {
		Statement st = db.getConnection().createStatement();
		String createTable = null;
		createTable = "CREATE TABLE "+QuickShop.instance.getDbPrefix()+"shops (owner  VARCHAR(255) NOT NULL, price  double(32, 2) NOT NULL, itemConfig TEXT CHARSET utf8 NOT NULL, x  INTEGER(32) NOT NULL, y  INTEGER(32) NOT NULL, z  INTEGER(32) NOT NULL, world VARCHAR(32) NOT NULL, unlimited  boolean, type  boolean, PRIMARY KEY (x, y, z, world) );";
		st.execute(createTable);
	}

	/**
	 * Creates the database table 'messages'
	 * 
	 * @throws SQLException If the connection is invalid
	 */
	public static void createMessagesTable(Database db) throws SQLException {
		Statement st = db.getConnection().createStatement();
		String createTable = null;
		createTable = "CREATE TABLE " + QuickShop.instance.getDbPrefix()
				+ "messages (owner  VARCHAR(255) NOT NULL, message  TEXT(25) NOT NULL, time  BIGINT(32) NOT NULL );";
		st.execute(createTable);
	}
	
	public static ResultSet selectAllShops(Database db) throws SQLException {
		PreparedStatement ps = db.getConnection()
				.prepareStatement("SELECT * FROM " + QuickShop.instance.getDbPrefix() + "shops");
		return ps.executeQuery();
	}
	public static ResultSet selectAllMessages(Database db) throws SQLException {
		PreparedStatement ps =  db.getConnection().prepareStatement("SELECT * FROM "+QuickShop.instance.getDbPrefix()+"messages");
		return ps.executeQuery();
	}

	public static void removeShop(Database db, int x, int y, int z, String worldName) throws SQLException {
		db.getConnection().createStatement()
				.executeUpdate("DELETE FROM " + QuickShop.instance.getDbPrefix() + "shops WHERE x = " + x + " AND y = " + y
						+ " AND z = " + z + " AND world = \"" + worldName + "\""
						+ (db.getCore() instanceof MySQLCore ? " LIMIT 1" : ""));
	}
	public static void updateOwner2UUID(String ownerUUID, int x, int y, int z, String worldName) throws SQLException {
		QuickShop.instance.getDB().getConnection().createStatement()
		.executeUpdate("UPDATE "+QuickShop.instance.getDbPrefix()+"shops SET owner = \"" + ownerUUID.toString()
		+ "\" WHERE x = " + x + " AND y = " + y + " AND z = " + z
		+ " AND world = \"" + worldName + "\" LIMIT 1");
	}

	public static void updateShop(Database db, String owner, ItemStack item, int unlimited, int shopType,
			double price, int x, int y, int z, String world) {
		String q = "UPDATE "+QuickShop.instance.getDbPrefix()+"shops SET owner = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
		db.execute(q, owner, Util.serialize(item), unlimited, shopType, price, x, y, z, world);
	}
	
	public static void createShop(String owner, double price, ItemStack item, int unlimited, int shopType, String world, int x, int y, int z) {
		String q = "INSERT INTO "+QuickShop.instance.getDbPrefix()+"shops (owner, price, itemConfig, x, y, z, world, unlimited, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		QuickShop.instance.getDB().execute(q, owner, price, Util.serialize(item), x, y, z, world, unlimited, shopType);
	}
	public static void sendMessage(UUID player,String message,long time) {
		String q = "INSERT INTO "+QuickShop.instance.getDbPrefix()+"messages (owner, message, time) VALUES (?, ?, ?)";
		QuickShop.instance.getDB().execute(q, player.toString(), message, System.currentTimeMillis());
	}
	
	public static void cleanMessage(long weekAgo) {
		QuickShop.instance.getDB().execute("DELETE FROM "+QuickShop.instance.getDbPrefix()+"messages WHERE time < ?", weekAgo);
	}
	
}