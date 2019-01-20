package org.maxgamer.quickshop.Command;

import java.util.ArrayList;
import java.util.List;

import org.maxgamer.quickshop.QuickShop;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.TabCompleteEvent;

public class Tab {
	QuickShop plugin;

	public Tab(QuickShop plugin) {
		this.plugin = plugin;
	}

	@Listener
	public void onTabComplete(TabCompleteEvent.Command e) {
		List<String> commands = new ArrayList<>();
		String[] args = e.getArguments().split(" ");
		commands.add("unlimited");
		commands.add("buy");
		commands.add("sell");
		commands.add("create");
		commands.add("price");
		commands.add("clean");
		commands.add("range");
		commands.add("refill");
		commands.add("empty");
		commands.add("setowner");
		commands.add("fetchmessage");
		if (args != null && args.length == 1) {
			List<String> list = new ArrayList<>();
			for (String s : commands) {
				if (s.startsWith(args[0])) {
					list.add(s);
				}
			}
			e.getTabCompletions().clear();
			for (String string : list) {
				e.getTabCompletions().add(string);
			}

		}
	}  
}
