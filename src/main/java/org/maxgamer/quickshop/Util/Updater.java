package org.maxgamer.quickshop.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.maxgamer.quickshop.QuickShop;
import org.spongepowered.api.util.Color;


public class Updater {
	/**
	 * Check new update
	 * 
	 * @param 
	 * @return True=Have a new update; False=No new update or check update failed.
	 */
	public static boolean checkUpdate() {
		if(!QuickShop.instance.getConfig().getBoolean("updater")) {
			return false;
		}
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=62575").openConnection();
            int timed_out = 300000;
            connection.setConnectTimeout(timed_out);
            connection.setReadTimeout(timed_out);
            String localPluginVersion = QuickShop.getVersion();
            String spigotPluginVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
            if (!spigotPluginVersion.equals(localPluginVersion) && spigotPluginVersion != null) {
//                QuickShgetLogger().info("New QuickShop release now updated on SpigotMC.org! ");
//                getLogger().info("Update plugin in there:https://www.spigotmc.org/resources/59134/");
            	connection.disconnect();
            	return true;
            }
            connection.disconnect();
            return false;
        } catch (IOException e) {
           System.out.println(Color.RED + "[QuickShop] Failed to check for an update on SpigotMC.org! Maybe internet issue or SpigotMC host down. If you want disable update checker, you can disable in config.yml, but we still high-recommand check update on SpigotMC.org.");
            return false;
        }
    }
}
