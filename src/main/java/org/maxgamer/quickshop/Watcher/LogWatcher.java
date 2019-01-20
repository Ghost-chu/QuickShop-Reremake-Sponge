package org.maxgamer.quickshop.Watcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.maxgamer.quickshop.QuickShop;
import org.spongepowered.api.scheduler.Task;

public class LogWatcher implements Runnable {
	private PrintStream ps;
	private ArrayList<String> logs = new ArrayList<String>(5);
	Task.Builder taskBuilder = Task.builder();

	public LogWatcher(QuickShop plugin, File log) {
		try {
			if (!log.exists()) {
				log.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(log, true);
			this.ps = new PrintStream(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			plugin.getLogger().error("Log file not found!");
		} catch (IOException e) {
			e.printStackTrace();
			plugin.getLogger().error("Could not create log file!");
		}
	}

	@Override
	public void run() {
		synchronized (logs) {
			for (String s : logs) {
				ps.println(s);
			}
			logs.clear();
		}
	}

	public void add(String s) {
		synchronized (logs) {
			logs.add(s);
		}
	}

	public void close() {
		this.ps.close();
	}
}