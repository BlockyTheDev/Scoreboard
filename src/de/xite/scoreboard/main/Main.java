package de.xite.scoreboard.main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import de.xite.scoreboard.api.CustomPlaceholders;
import de.xite.scoreboard.commands.ScoreboardCommand;
import de.xite.scoreboard.files.Config;
import de.xite.scoreboard.files.TabConfig;
import de.xite.scoreboard.listeners.Chat;
import de.xite.scoreboard.listeners.EventListener;
import de.xite.scoreboard.listeners.LuckPermsEvent;
import de.xite.scoreboard.manager.ScoreboardManager;
import de.xite.scoreboard.manager.ScoreboardPlayer;
import de.xite.scoreboard.utils.BStatsMetrics;
import de.xite.scoreboard.utils.SelfCheck;
import de.xite.scoreboard.utils.Updater;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin implements Listener{
	public static Main pl;
	
	// APIs
	public static Economy econ = null;
	public LuckPerms luckPerms = null;
	// Supported Plugins
	public static boolean hasVault = false;
	public static boolean hasPex = false;
	public static boolean hasPapi = false;
	public static boolean hasLuckPerms = false;
	
	// prefix & plugin folder
	public static String pluginfolder = "plugins/Scoreboard";
	public static String pr = "§7[§eScoreboard§7] ";
	
	// All registered scoreboards
	public static HashMap<String, ScoreboardManager> scoreboards = new HashMap<>();
	
	// All registered scoreboards
	public static HashMap<Player, String> players = new HashMap<>(); // Player; Scoreboard config file name
	
	// All registered custom placeholders
	public static ArrayList<CustomPlaceholders> ph = new ArrayList<>();
	
	// Debug enabled/disabled
	public static boolean debug = false;
	
	@Override
	public void onEnable() {
		pl = this;
		
		// Register commands and events
		getCommand("sb").setExecutor(new ScoreboardCommand());
		getCommand("scoreboard").setExecutor(new ScoreboardCommand());
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new EventListener(), this);
		pm.registerEvents(new Chat(), this);
		pm.registerEvents(this, this);
		
		Config.loadConfig(); // load the config.yml
		
	    // Check if the debug is enabled in the config.yml
	    if(pl.getConfig().getBoolean("debug"))
	    	debug = true;
	    
	    registerScoreboards();
		
		// ---- Check for compatible plugins ---- //
		if(Bukkit.getPluginManager().isPluginEnabled("Vault")) {
			if(debug)
				pl.getLogger().info("Loading Vault...");
			try{
				if(setupEconomy()) {
					hasVault = true;
					if(debug)
						pl.getLogger().info("Successfully loaded Vault!");
				}else
					pl.getLogger().severe("There was an error while loading Vault! Make sure that you have a money Plugin on your server that also supports Vault.");
			}catch (NoClassDefFoundError  e) {
				pl.getLogger().severe("There was an error while loading Vault! Make sure that you have a money Plugin on your server that also supports Vault.");
			}
		}
	    if(Bukkit.getPluginManager().isPluginEnabled("PermissionsEx"))
	        hasPex = true;
	    if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
	    	hasPapi = true;
	    if(Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
	    	hasLuckPerms = true;
			RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
			if(provider != null) {
				luckPerms = provider.getProvider();
			}
			new LuckPermsEvent(pl, luckPerms);
	    }
	    
	    // start the self check
	    if(SelfCheck.check()) {
	    	pl.getLogger().severe("self-check -> Fatal errors were found! Please fix you config! Disabling Plugin...");
	    	Bukkit.getPluginManager().disablePlugin(pl);
	    	return;
	    }
	    
	    // Updater
	    if(Updater.checkVersion()) {
	    	pl.getLogger().info("-> A new version (v."+Updater.version+") is available! Your version: "+pl.getDescription().getVersion());
			pl.getLogger().info("-> Update me! :)");
	    }
	    
	    // Set the scoreboard and prefixes for all online players
		Bukkit.getScheduler().runTaskLater(pl, new Runnable() {
			@Override
			public void run() {
				for(Player all : Bukkit.getOnlinePlayers())
					if(!players.containsKey(all))
						ScoreboardPlayer.setScoreboard(all, pl.getConfig().getString("scoreboard-default"));
				if(pl.getConfig().getBoolean("tablist.text")) {
					TabConfig tab = new TabConfig();
					tab.register();
				}
			}
		}, 30);
		// BStats analytics
		try {
			int pluginId = 6722; // <-- Replace with the id of your plugin!
	        BStatsMetrics metrics = new BStatsMetrics(this, pluginId);
	        //Costom charts
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("update_auto_update", () -> pl.getConfig().getBoolean("update.autoupdater") ? "Aktiviert" : "Deaktiviert"));
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("update_notifications", () -> pl.getConfig().getBoolean("update.notification") ? "Aktiviert" : "Deaktiviert"));
	        
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_scoreboard", () -> pl.getConfig().getBoolean("scoreboard") ? "Aktiviert" : "Deaktiviert"));
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_tablist_text", () -> pl.getConfig().getBoolean("tablist.text") ? "Aktiviert" : "Deaktiviert"));
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_tablist_ranks", () -> pl.getConfig().getBoolean("tablist.ranks") ? "Aktiviert" : "Deaktiviert"));
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_use_chat", () -> pl.getConfig().getBoolean("chat.ranks") ? "Atktiviert" : "Deaktiviert"));
	        metrics.addCustomChart(new BStatsMetrics.SimplePie("setting_permsystem", () -> pl.getConfig().getString("ranks.permissionsystem").toLowerCase()));
	        if(Main.debug)
	        	pl.getLogger().info("Analytics sent to BStats");
		} catch (Exception e) {
			pl.getLogger().warning("Could not send analytics to BStats!");
		}
	}
	@Override
	public void onDisable() {
		if(pl.getConfig().getBoolean("update.autoupdater"))
			if(Updater.checkVersion())
				Updater.downloadFile();
		Main.unregisterScoreboards();
		if(pl.getConfig().getBoolean("scoreboard"))
			for(Entry<Player, String> all : Main.players.entrySet())
				ScoreboardPlayer.removeScoreboard(all.getKey(), true);
		ph.clear();
	}

	public static void registerScoreboards() {
		ArrayList<String> boards = new ArrayList<>();
		boards.add("scoreboard");
		
		if(pl.getConfig().getBoolean("scoreboard")) { // register the scoreboard if enabled
			new ScoreboardPlayer(); // prepare the scoreboard
			for(String board : boards)
				ScoreboardManager.get(board);
		}
	}
	public static void unregisterScoreboards() {
		for(Entry<String, ScoreboardManager> sm : Main.scoreboards.entrySet()) {
			String name = sm.getKey();
			ScoreboardManager.unregister(name);
		}
	}
	

    // ---- Utils ---- //
	public static Integer getBukkitVersion() {
		String s = Bukkit.getBukkitVersion();
		String v = "";
		boolean pointCounter = true;
		while(s.length() > 1) {
			if(v.endsWith(".") || v.endsWith("-")) {// Allow only one point. example: from '1.8.8-R0.1-SNAPSHOT' extract to '1.8'. The pointcounter is needed for version 1.10+ because of more decimales.
				if(pointCounter) {
					pointCounter = false;
				}else {
					s = "";
					try {
						return Integer.parseInt(v.substring(0, v.length()-1).replace(".", ""));//decimals are removed. example: 1.8 ->  18 | example 2: 1.12 -> 112
					}catch (Exception e) {
						Main.pl.getLogger().severe("There was a problem whilst checking your minecraft server version! Have you something like a special version? Detected version: "+v);
						Main.pl.getLogger().severe("If you don't know why you get this error and you are using one of the official supported server softwares, please report this bug in our discord!");
						Bukkit.getPluginManager().disablePlugin(pl);
						return 0;
					}
				}
			}
			v += s.substring(0, 1);//add the first char from s to v
			s = s.substring(1);//remove the first char from s
		}
		return 0;
	}
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null)
            return false;
        econ = rsp.getProvider();
        return econ != null;
    }
    public static double round(double value, int places) {
    	if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    public final static char COLOR_CHAR = ChatColor.COLOR_CHAR;
    public static String translateHexColor(String message) {
    	String startTag = "#";
    	String endTag = "";
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
                    );
        }
        return matcher.appendTail(buffer).toString();
    }
}
