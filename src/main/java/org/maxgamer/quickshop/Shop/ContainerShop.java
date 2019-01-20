package org.maxgamer.quickshop.Shop;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Database.DatabaseHelper;
import org.maxgamer.quickshop.Util.MsgUtil;
import org.maxgamer.quickshop.Util.Util;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class ContainerShop implements Shop {
	private Location<World> loc;
	private double price;
	private UUID owner;
	private ItemStack item;
	private DisplayItem displayItem;
	private boolean unlimited;
	private ShopType shopType;
	private QuickShop plugin;

	/**
	 * Returns a clone of this shop. References to the same display item,
	 * itemstack, location and owner as this shop does. Do not modify them or
	 * you will modify this shop.
	 * 
	 * **NOT A DEEP CLONE**
	 */
	public ContainerShop clone() {
		return new ContainerShop(this);
	}

	private ContainerShop(ContainerShop s) {
		this.displayItem = s.displayItem;
		this.shopType = s.shopType;
		this.item = s.item;
		this.loc = s.loc;
		this.plugin = s.plugin;
		this.unlimited = s.unlimited;
		this.owner = s.owner;
		this.price = s.price;
	}

	/**
	 * Adds a new shop.
	 * 
	 * @param loc
	 *            The location of the chest block
	 * @param price
	 *            The cost per item
	 * @param item
	 *            The itemstack with the properties we want. This is .cloned, no
	 *            need to worry about references
	 * @param owner
	 *            The player who owns this shop.
	 */
	public ContainerShop(Location<World> loc, double price, ItemStack item, UUID owner) {
		this.loc = loc;
		this.price = price;
		this.owner = owner;
		this.item = item.copy();
		this.plugin = QuickShop.instance;
		this.item.setAmount(1);
		if (plugin.display) {
			this.displayItem = new DisplayItem(this, this.item);
		}
		this.shopType = ShopType.SELLING;
	}

	/**
	 * Returns the number of items this shop has in stock.
	 * 
	 * @return The number of items available for purchase.
	 */
	public int getRemainingStock() {
		if (this.unlimited)
			return -1;
		return Util.countItems(this.getInventory(), this.getItem());
	}

	/**
	 * Returns the number of free spots in the chest for the particular item.
	 * 
	 * @param stackSize
	 * @return
	 */
	public int getRemainingSpace() {
		if (this.unlimited)
			return -1;
		return Util.countSpace(this.getInventory(), this.getItem());
	}

	/**
	 * Returns true if the ItemStack matches what this shop is selling/buying
	 * 
	 * @param item
	 *            The ItemStack
	 * @return True if the ItemStack is the same (Excludes amounts)
	 */
	public boolean matches(ItemStack item) {
		return Util.matches(this.item, item);
	}

	/**
	 * Returns the shop that shares it's inventory with this one.
	 * 
	 * @return the shop that shares it's inventory with this one. Will return
	 *         null if this shop is not attached to another.
	 */
	public ContainerShop getAttachedShop() {
		BlockState c = Util.getSecondHalf(this.getLocation().getBlock());
		if (c == null)
			return null;
		Shop shop = plugin.getShopManager().getShop(c.getLocation());
		return shop == null ? null : (ContainerShop) shop;
	}

	/**
	 * Returns true if this shop is a double chest, and the other half is
	 * selling/buying the same as this is buying/selling.
	 * 
	 * @return true if this shop is a double chest, and the other half is
	 *         selling/buying the same as this is buying/selling.
	 */
	public boolean isDoubleShop() {
		ContainerShop nextTo = this.getAttachedShop();
		if (nextTo == null) {
			return false;
		}
		if (nextTo.matches(this.getItem())) {
			// They're both trading the same item
			if (this.getShopType() == nextTo.getShopType()) {
				// They're both buying or both selling => Not a double shop,
				// just two shops.
				return false;
			} else {
				// One is buying, one is selling.
				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * @return The location of the shops chest
	 */
	public Location<World> getLocation() {
		return this.loc;
	}

	/**
	 * @return The price per item this shop is selling
	 */
	public double getPrice() {
		return this.price;
	}

	/**
	 * Sets the price of the shop. Does not update it in the database. Use
	 * shop.update() for that.
	 * 
	 * @param price
	 *            The new price of the shop.
	 */
	public void setPrice(double price) {
		this.price = price;
	}

	/**
	 * @return The ItemStack type of this shop
	 */
	public ItemType getMaterial() {
		return this.item.getType();
	}

	/**
	 * Upates the shop into the database.
	 */
	public void update() {
		int x = this.getLocation().getBlockX();
		int y = this.getLocation().getBlockY();
		int z = this.getLocation().getBlockZ();
		String world = this.getLocation().getExtent().getName();
		int unlimited = this.isUnlimited() ? 1 : 0;
		//String q = "UPDATE shops SET owner = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
		try {
			//plugin.getDB().execute(q, this.getOwner().toString(), Util.serialize(this.getItem()), unlimited, shopType.toID(), this.getPrice(), x, y, z, world);
			DatabaseHelper.updateShop(plugin.getDB(), this.getOwner().toString(), this.getItem(), unlimited, shopType.toID(), this.getPrice(), x, y, z, world);
		} catch (Exception e) {
			e.printStackTrace();
			plugin.getLogger().log(Level.WARNING, "Could not update shop in database! Changes will revert after a reboot!");
		}
	}

	/**
	 * @return The durability of the item
	 */
	public short getDurability() {
		return (short) ((Damageable)this.item.getItemMeta()).getDamage();
	}

	/**
	 * @return The chest this shop is based on.
	 */
	public Inventory getInventory() throws IllegalStateException {
//		try {
//		if(loc.getBlock().getState().getType()==Material.ENDER_CHEST && plugin.openInvPlugin!=null) {
//			OpenInv openInv = ((OpenInv)plugin.openInvPlugin);
//			 return openInv.getSpecialEnderChest(openInv.loadPlayer(Bukkit.getOfflinePlayer(this.owner)), Bukkit.getOfflinePlayer(this.owner).isOnline()).getBukkitInventory();
//		}
//		}catch(Exception e){
//			Util.debugLog(e.getMessage());
//			return null;
//		}
		InventoryHolder container;
		try {
			container = (InventoryHolder) this.loc.getBlock().getState();
			return container.getInventory();
		} catch (Exception e) {
			throw new IllegalStateException("Inventory doesn't exist anymore: "+this);
		}
	}

	/**
	 * @return The name of the player who owns the shop.
	 */
	public UUID getOwner() {
		return this.owner;
	}

	/**
	 * @return The enchantments the shop has on its items.
	 */
	public Map<Enchantment, Integer> getEnchants() {
		return this.item.getItemMeta().getEnchants();
	}

	/**
	 * @return Returns a dummy itemstack of the item this shop is selling.
	 */
	public ItemStack getItem() {
		return item;
	}

	/**
	 * Removes an item from the shop.
	 * 
	 * @param item
	 *            The itemstack. The amount does not matter, just everything
	 *            else
	 * @param amount
	 *            The amount to remove from the shop.
	 */
	public void remove(ItemStack item, int amount) {
		if (this.unlimited)
			return;
		Inventory inv = this.getInventory();
		int remains = amount;
		while (remains > 0) {
			int stackSize = Math.min(remains, item.getMaxStackQuantity());
			item.setQuantity(stackSize);
			inv.removeItem(item); /**How use Sponge to do this?**/
			remains = remains - stackSize;
		}
	}

	/**
	 * Add an item to shops chest.
	 * 
	 * @param item
	 *            The itemstack. The amount does not matter, just everything
	 *            else
	 * @param amount
	 *            The amount to add to the shop.
	 */
	public void add(ItemStack item, int amount) {
		if (this.unlimited)
			return;
		Inventory inv = this.getInventory();
		int remains = amount;
		while (remains > 0) {
			int stackSize = Math.min(remains, item.getMaxStackQuantity());
			item.setQuantity(stackSize);
			inv.addItem(item);
			remains = remains - stackSize;
		}
	}

	/**
	 * Sells amount of item to Player p. Does NOT check our inventory, or
	 * balances
	 * 
	 * @param p
	 *            The player to sell to
	 * @param amount
	 *            The amount to sell
	 */
	public void sell(Player p, int amount) {
		if (amount < 0)
			this.buy(p, -amount);
		// Items to drop on floor
		ArrayList<ItemStack> floor = new ArrayList<ItemStack>(5);
		Inventory pInv = p.getInventory();
		if (this.isUnlimited()) {
			ItemStack item = this.item.copy();
			while (amount > 0) {
				int stackSize = Math.min(amount, this.item.getMaxStackQuantity());
				item.setQuantity(stackSize);
				pInv.addItem(item);
				amount -= stackSize;
			}
		} else {
			ItemStack[] chestContents = this.getInventory().getContents();
			for (int i = 0; amount > 0 && i < chestContents.length; i++) {
				// Can't clone it here, it could be null
				ItemStack item = chestContents[i];
				if (item != null && this.matches(item)) {
					// Copy it, we don't want to interfere
					item = item.copy();
					// Amount = total, item.getAmount() = how many items in the
					// stack
					int stackSize = Math.min(amount, item.getQuantity());
					// If Amount is item.getAmount(), then this sets the amount
					// to 0
					// Else it sets it to the remainder
					chestContents[i].setAmount(chestContents[i].getQuantity() - stackSize);
					// We can modify this, it is a copy.
					item.setQuantity(stackSize);
					// Add the items to the players inventory
					floor.addAll(pInv.addItem(item).values());
					amount -= stackSize;
				}
			}
			// We now have to update the chests inventory manually.
			this.getInventory().setContents(chestContents);
		}
		for (int i = 0; i < floor.size(); i++) {
			p.getWorld().dropItem(p.getLocation(), floor.get(i));
		}
	}

	/**
	 * Buys amount of item from Player p. Does NOT check our inventory, or
	 * balances
	 * 
	 * @param p
	 *            The player to buy from
	 * @param item
	 *            The itemStack to buy
	 * @param amount
	 *            The amount to buy
	 */
	public void buy(Player p, int amount) {
		if (amount < 0)
			this.sell(p, -amount);
		if (this.isUnlimited()) {
			ItemStack[] contents = p.getInventory().getContents();
			for (int i = 0; amount > 0 && i < contents.length; i++) {
				ItemStack stack = contents[i];
				if (stack == null)
					continue; // No item
				if (matches(stack)) {
					int stackSize = Math.min(amount, stack.getQuantity());
					stack.setQuantity(stack.getQuantity() - stackSize);
					amount -= stackSize;
				}
			}
			// Send the players new inventory to them
			p.getInventory().setContents(contents);
			// This should not happen.
			if (amount > 0) {
				plugin.getLogger().warn("Could not take all items from a players inventory on purchase! " + p.getName() + ", missing: " + amount + ", item: " + this.getDataName() + "!");
			}
		} else {
			ItemStack[] playerContents = p.getInventory().getContents();
			Inventory chestInv = this.getInventory();
			for (int i = 0; amount > 0 && i < playerContents.length; i++) {
				ItemStack item = playerContents[i];
				if (item != null && this.matches(item)) {
					// Copy it, we don't want to interfere
					item = item.copy();
					// Amount = total, item.getAmount() = how many items in the
					// stack
					int stackSize = Math.min(amount, item.getQuantity());
					// If Amount is item.getAmount(), then this sets the amount
					// to 0
					// Else it sets it to the remainder
					playerContents[i].setAmount(playerContents[i].getQuantity() - stackSize);
					// We can modify this, it is a copy.
					item.setQuantity(stackSize);
					// Add the items to the players inventory
					chestInv.addItem(item);
					amount -= stackSize;
				}
			}
			// Now update the players inventory.
			p.getInventory().setContents(playerContents);
		}
	}

	/**
	 * Changes the owner of this shop to the given player.
	 * 
	 * @param owner
	 *            The name of the owner. You must do shop.update() after to save
	 *            it after a reboot.
	 */
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	/**
	 * Returns the display item associated with this shop.
	 * 
	 * @return The display item associated with this shop.
	 */
	public DisplayItem getDisplayItem() {
		return this.displayItem;
	}

	public void setUnlimited(boolean unlimited) {
		this.unlimited = unlimited;
	}

	public boolean isUnlimited() {
		return this.unlimited;
	}

	public ShopType getShopType() {
		return this.shopType;
	}

	public boolean isBuying() {
		return this.shopType == ShopType.BUYING;
	}

	public boolean isSelling() {
		return this.shopType == ShopType.SELLING;
	}

	/**
	 * Changes a shop type to Buying or Selling. Also updates the signs nearby.
	 * 
	 * @param shopType
	 *            The new type (ShopType.BUYING or ShopType.SELLING)
	 */
	public void setShopType(ShopType shopType) {
		this.shopType = shopType;
		this.setSignText();
	}

	/**
	 * Updates signs attached to the shop
	 */
	public void setSignText() {
		if (Util.isLoaded(this.getLocation()) == false)
			return;
		String[] lines = new String[4];
		lines[0] = MsgUtil.getMessage("signs.header", this.ownerName());
		if (this.isSelling()) {
			if(this.getRemainingStock()==-1) {
				lines[1] = MsgUtil.getMessage("signs.selling", "" + MsgUtil.getMessage("signs.unlimited"));
			}else {
				lines[1] = MsgUtil.getMessage("signs.selling", "" + this.getRemainingStock());
			}
			
		} else if (this.isBuying()) {
			if(this.getRemainingSpace()==-1) {
				lines[1] = MsgUtil.getMessage("signs.buying", "" + MsgUtil.getMessage("signs.unlimited"));
				
			}else {
				lines[1] = MsgUtil.getMessage("signs.buying", "" + this.getRemainingSpace());
			}
			
		}
		lines[2] = MsgUtil.getMessage("signs.item", Util.getNameForSign(this.item));
		lines[3] = MsgUtil.getMessage("signs.price", Util.format(this.getPrice()));
		this.setSignText(lines);
	}

	/**
	 * Changes all lines of text on a sign near the shop
	 * 
	 * @param lines The array of lines to change. Index is line number.
	 */
	public void setSignText(String[] lines) {
		if (!Util.isLoaded(this.getLocation()))
			return;
		for (TileEntity entity : this.getSigns()) {
			if (entity.supports(SignData.class)) {
				for (int i = 0; i < lines.length; i++) {
					SignData sign = entity.getOrCreate(SignData.class).get();
					sign.set(sign.lines().set(i,
							Text.of(lines[i].length() < 16 ? lines[i] : lines[i].substring(0, 15))));
					entity.offer(sign);
				}
			}
		}
	}

	/**
	 * Returns a list of signs that are attached to this shop (QuickShop and
	 * blank signs only)
	 * 
	 * @return a list of signs that are attached to this shop (QuickShop and
	 *         blank signs only)
	 */
	public List<TileEntity> getSigns() {
		ArrayList<TileEntity> signs = new ArrayList<TileEntity>();
		if (this.getLocation().getExtent() == null)
			return signs;
		@SuppressWarnings("unchecked")
		Location<World>[] blocks = new Location[4];
		blocks[0] = loc.getRelative(Direction.EAST);
		blocks[1] = loc.getRelative(Direction.NORTH);
		blocks[2] = loc.getRelative(Direction.SOUTH);
		blocks[3] = loc.getRelative(Direction.WEST);
		final String signHeader = MsgUtil.getMessage("signs.header", "");
		for (Location<World> b : blocks) {
			if (b.getBlock().getType() != BlockTypes.WALL_SIGN)
				continue;
			if (!isAttached(b.getBlock()))
				continue;

			TileEntity tileEntity = b.getTileEntity().get();
			if(!tileEntity.supports(SignData.class)) {
				continue;
			}
			SignData sign = tileEntity.getOrCreate(SignData.class).get();
			if (sign.get(0).get().toPlain().contains(signHeader)) {
				signs.add(tileEntity);
			} else {
				boolean text = false;
				for (Text s : sign.asList()) {
					if (!s.isEmpty()) {
						text = true;
						break;
					}
				}
				if (!text) {
					signs.add(tileEntity);
				}
			}
		}
		return signs;
	}

	public boolean isAttached(BlockState b) {
		if (b.getType() != BlockTypes.WALL_SIGN)
			new IllegalArgumentException(b + " Is not a sign!").printStackTrace();
		return this.getLocation().getBlock().equals(Util.getAttached(b));
	}

	/**
	 * Convenience method. Equivilant to
	 * org.maxgamer.quickshop.Util.getName(shop.getItem()).
	 * 
	 * @return The name of this shops item
	 */
	public String getDataName() {
		return Util.getName(this.getItem());
	}

	/**
	 * Deletes the shop from the list of shops and queues it for database
	 * deletion *DOES* delete it from memory
	 */
	public void delete() {
		delete(true);
	}

	/**
	 * Deletes the shop from the list of shops and queues it for database
	 * deletion
	 * 
	 * @param fromMemory
	 *            True if you are *NOT* iterating over this currently, *false if
	 *            you are iterating*
	 */
	public void delete(boolean fromMemory) {
		// Delete the display item
		if (this.getDisplayItem() != null) {
			this.getDisplayItem().remove();
		}
		// Delete the signs around it
		for (TileEntity s : this.getSigns()) {
			s.getLocation().setBlockType(BlockTypes.AIR);
		}
		// Delete it from the database
		int x = this.getLocation().getBlockX();
		int y = this.getLocation().getBlockY();
		int z = this.getLocation().getBlockZ();
		String world = this.getLocation().getExtent().getName();
		try {
			DatabaseHelper.removeShop(plugin.getDB(), x, y, z, world);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// Refund if necessary
		if (plugin.getConfig().getBoolean("shop.refund")) {
			plugin.getEcon().deposit(this.getOwner(), plugin.getConfig().getDouble("shop.cost"));
		}
		if (fromMemory) {
			// Delete it from memory
			plugin.getShopManager().removeShop(this);
		}
	}

	public boolean isValid() {
		checkDisplay();
		return Util.canBeShop(this.getLocation().getBlock(),null,true);
	}

	private void checkDisplay() {
		if (plugin.display == false)
			return;
		if (getLocation().getExtent() == null)
			return; // not loaded
		boolean trans = Util.isTransparent(getLocation().copy().add(0.5, 1.2, 0.5).getBlock().getType());
		if (trans && this.getDisplayItem() == null) {
			this.displayItem = new DisplayItem(this, this.getItem());
			this.getDisplayItem().spawn();
		}
		if (this.getDisplayItem() != null) {
			if (!trans) { // We have a display item in a block... delete it
				this.getDisplayItem().remove();
				this.displayItem = null;
				return;
			}
			DisplayItem disItem = this.getDisplayItem();
			Location dispLoc = disItem.getDisplayLocation();
			if (dispLoc.getBlock() != null && dispLoc.getBlock().getType() == BlockTypes.WATER) {
				disItem.remove();
				return;
			}
			if (disItem.getItem() == null) {
				disItem.removeDupe();
				disItem.spawn();
				return;
			}
			Item item = disItem.getItem();
			/**Need upgrade to Sponge API**/
			if (item.getTicksLived() > 5000 || !item.isValid() || item.isDead()) {
				disItem.respawn();
				disItem.removeDupe();
			} else if (item.getLocation().distanceSquared(dispLoc) > 1) {
				item.teleport(dispLoc, TeleportCause.PLUGIN);
			}
		}
	}
	
	public boolean checkDisplayMoved() {
		// don't check if the plugin doesn't know about the object
		if (this.getDisplayItem() == null) {
			return false;
		}
		
		Item item = this.getDisplayItem().getItem();
		if (item == null) {
			return false;
		}
		
		if (item.isDead()) {
			return false;
		}
		
		// don't check if the chunk is not loaded
		if (!item.getLocation().getExtent().isChunkLoaded(item.getLocation().getChunk())) { 
			return false;
		}

		return this.getDisplayItem().getDisplayLocation().distanceSquared(item.getLocation()) > 0.2;
	}

	public void onUnload() {
		if (this.getDisplayItem() != null) {
			this.getDisplayItem().remove();
			this.displayItem = null;
		}
	}

	public void onLoad() {
		checkDisplay();
		
		//Clear the chest?
		
		//this.setSignText();

		// check price restriction
		Entry<Double,Double> priceRestriction = Util.getPriceRestriction(this.getMaterial());
		if (priceRestriction!=null) {
			if (price<priceRestriction.getKey()) {
				price=priceRestriction.getKey();
				this.update();
			} else if (price>priceRestriction.getValue()) {
				price=priceRestriction.getValue();
				this.update();
			}
		}
	}

	public void onClick() {
		this.setSignText();
	}
	
	public String ownerName() {
		if (this.isUnlimited()) {
			return MsgUtil.getMessage("admin-shop");
		}
		
		if (this.getOwner() == null) {
			return MsgUtil.getMessage("unknown-owner");
		}
		
		final String name = Util.getOfflinePlayer(this.getOwner()).get().getName();
		if (name == null) {
			return MsgUtil.getMessage("unknown-owner");
		}
		
		return name;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Shop " + (loc.getExtent() == null ? "unloaded world" : loc.getExtent().getName()) + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
		sb.append(" Owner: " + this.ownerName() + " - " + getOwner().toString());
		if (isUnlimited())
			sb.append(" Unlimited: true");
		sb.append(" Price: " + getPrice());
		sb.append(" Item: " + getItem().toString());
		return sb.toString();
	}

}