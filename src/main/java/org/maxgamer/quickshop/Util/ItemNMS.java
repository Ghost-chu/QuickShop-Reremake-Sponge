package org.maxgamer.quickshop.Util;

import org.maxgamer.quickshop.QuickShop;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

public abstract class ItemNMS { 
    private static Method craftItemStack_asNMSCopyMethod;
    private static Class<?> nbtTagCompoundClass;
    private static Method itemStack_saveMethod;
    
    static {
        String name = ((MinecraftServer)(Sponge.getServer())).getClass().getPackage().getName();
        String nmsVersion = name.substring(name.lastIndexOf('.') + 1);
        /**@TODO There need upgrade to Sponge API**/
        try {
            craftItemStack_asNMSCopyMethod = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".inventory.CraftItemStack")
                    .getDeclaredMethod("asNMSCopy", ItemStack.class);
            
            nbtTagCompoundClass = Class.forName("net.minecraft.server." + nmsVersion + ".NBTTagCompound");
            
            itemStack_saveMethod = Class.forName("net.minecraft.server." + nmsVersion + ".ItemStack")
                    .getDeclaredMethod("save", nbtTagCompoundClass);
            
        } catch (Throwable t) {
            QuickShop.instance.getLogger().info("A error happend:");
            t.printStackTrace();
            QuickShop.instance.getLogger().info("Try update QSRR. And feedback this bug on issue tracker.");
        }
    }
	
	public static String saveJsonfromNMS(ItemStack bStack) throws Throwable {
	    if (bStack.getType() == ItemTypes.AIR)
	        return null;
        Object mcStack = craftItemStack_asNMSCopyMethod.invoke(null, bStack);
        Object nbtTagCompound = nbtTagCompoundClass.newInstance();
        
        itemStack_saveMethod.invoke(mcStack, nbtTagCompound);
        return nbtTagCompound.toString();
	}
}
