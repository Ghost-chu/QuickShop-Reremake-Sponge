package org.maxgamer.quickshop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import com.google.inject.Inject;

public class Configuration {
	@Inject
	@DefaultConfig(sharedRoot = false)
	private Path defaultConfig;
	private YamlConfiguration yamlConfiguration;
	private boolean setuped = false;
	File config = null;
	public YamlConfiguration setupConfig() {
		config = Sponge.getConfigManager().getPluginConfig(QuickShop.instance).getConfigPath().toFile();
		if(!config.exists()) {
			 InputStream file = this.getClass().getResourceAsStream("/config.yml"); 
			 BufferedReader br=new BufferedReader(new InputStreamReader(file));
			 String s=null;
			 StringBuilder sb = new StringBuilder();
			 try {
				while((s=br.readLine())!=null) {
					 sb.append(s);
					 sb.append("\n");
				 }
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			 String defaultConfig = sb.toString();
		     try {
		    	 config.createNewFile();
				 FileWriter fw = new FileWriter(config);
				fw.write(defaultConfig);
				fw.flush();
			    fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		     
		     QuickShop.instance.getLogger().info("Writed default config.");
		}
		yamlConfiguration = YamlConfiguration.loadConfiguration(config);
		setuped=true;
		return yamlConfiguration;
	}
	public boolean isSetup() {
		return this.setuped;
	}
	public YamlConfiguration getYamlConfiguration() {
		return yamlConfiguration;
	}
	public void save() {
		try {
			yamlConfiguration.save(config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public File getConfig() {
		return config;
	}
	public Path getDefaultConfig() {
		return defaultConfig;
	}
	public File getDataFolder() {
		return config.getParentFile();
	}

	public void saveResource(String filename) {
		File save = Sponge.getConfigManager().getPluginConfig(QuickShop.instance).getConfigPath().toFile();
		if (!save.exists()) {
			InputStream savefile = this.getClass().getResourceAsStream("/" + filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(savefile));
			String s = null;
			StringBuilder sb = new StringBuilder();
			try {
				while ((s = br.readLine()) != null) {
					sb.append(s);
					sb.append("\n");
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String defaultConfig = sb.toString();
			try {
				config.createNewFile();
				FileWriter fw = new FileWriter(config);
				fw.write(defaultConfig);
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public InputStream getResource(String filename) {
			 InputStream savefile = this.getClass().getResourceAsStream("/"+filename); 
			 return savefile;
	}
}
