package de.xite.scoreboard.modules.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import de.xite.scoreboard.main.Config;
import de.xite.scoreboard.main.PowerBoard;

public class ScoreboardPlayer {
	static PowerBoard pl = PowerBoard.pl;
	
	// All players with scoreboards
	public static HashMap<Player, String> players = new HashMap<>(); // Player; Scoreboard config file name
	
	@SuppressWarnings("deprecation")
	public static void setScoreboard(Player p, boolean API) {
		Scoreboard board = p.getScoreboard();
		
		// ---- Scoreboard ---- //
		ScoreboardManager sm = null;
		if(checkConditions(p, Config.scoreboardBlacklistConditions)) {
			removeScoreboard(p, false);
			
			players.put(p, "blacklisted");
			if(PowerBoard.debug)
				pl.getLogger().info("Removed "+p.getName()+"'s scoreboard because blacklist-conditions match.");
			return;
		}
		if(!API) {
			sm = getMatchingScoreboard(p);
			if(sm == null)
				return;
			if(players.containsKey(p))
				if(players.get(p) == sm.getName()) {
					if(PowerBoard.debug)
						pl.getLogger().info("Did not set/update "+p.getName()+"'s scoreboard because he already have the same scoreboard (Current: "+players.get(p)+"; New: "+sm.getName()+").");
					return;
				}else {
					pl.getLogger().info("Changing "+p.getName()+"'s scoreboard to "+sm.getName());
					removeScoreboard(p, true);
				}
		}else {
			removeScoreboard(p, false);
		}
		
		Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
		if(obj == null) {
			if(PowerBoard.aboveMC_1_13) {
				obj = board.registerNewObjective("aaa", "bbb", "PowerBoard");
			}else
				obj = board.registerNewObjective("aaa", "bbb");
		}
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		if(!API) {
			if(sm == null)
				sm = getMatchingScoreboard(p);
			if(sm == null)
				return;
			
			if(players.containsKey(p)) {
				if(players.get(p).equals("blacklisted")) {
					players.remove(p);
				}else {
					
					ScoreboardManager.get(players.get(p)).removePlayer(p);
				}
			}
			sm.addPlayer(p);
			ScoreTitleUtils.setTitle(p, board, sm.getCurrentTitle(), true, sm);
			ScoreTitleUtils.setScores(p, board, sm.getCurrentScores(), true, sm);
		}
		
		// ---- Set the scoreboard ---- //
		p.setScoreboard(board);
		
		// Debug
		if(PowerBoard.debug)
			PowerBoard.pl.getLogger().info("Scoreboard set for player "+p.getName());
	}
	public static void updateScoreboard(Player p) {
		if(!players.containsKey(p))
			return;
		setScoreboard(p, false);
	}
	public static ScoreboardManager getMatchingScoreboard(Player p) {
		/* Config syntax: 
		conditions:
		  - world:world AND permission:some.permission
		  - world:world AND permission:some.other.permission
		  - world:world AND gamemode:creative
		  - world:world_nether
		*/
		for(Entry<String, ScoreboardManager> e : ScoreboardManager.scoreboards.entrySet()) {
			ScoreboardManager sm = e.getValue();
			if(sm == null) {
				pl.getLogger().severe("There was a error loading a scoreboard. Please check your configs.");
				return null;
			}
			if(sm.conditions == null) {
				pl.getLogger().severe("Could not get scoreboard '"+sm.getName()+"'! Probably a config error.");
				return null;
			}
			if(checkConditions(p, sm.conditions))
				return sm;
		}
		return ScoreboardManager.get(pl.getConfig().getString("scoreboard-default"));
	}
	public static boolean checkConditions(Player p, List<String> conditions) {
		for(String condition : conditions) { // For all "OR" conditions (lines)
			ArrayList<String> andConditions = new ArrayList<>();
			if(condition.contains(" AND ")) {
				for(String s : condition.split(" AND "))
					andConditions.add(s);
			}else
				andConditions.add(condition);
			
			boolean match = true;
			for(String s : andConditions) {
				if(s.startsWith("world:")) {
					String value = s.split("world:")[1];
					if(!(p.getLocation().getWorld().getName().equalsIgnoreCase(value)))
						match = false;
				}
				if(s.startsWith("permission:")) {
					String value = s.split("permission:")[1];
					if(!(p.hasPermission(value)))
						match = false;
				}
				if(s.startsWith("gamemode:")) {
					String value = s.split("gamemode:")[1];
					if(!(p.getGameMode().name().equalsIgnoreCase(value)))
						match = false;
				}
			}
			
			if(match == true)
				return true;
		}
		return false;
	}
	public static void removeScoreboard(Player p, boolean removeFromSBManager) {
		if(removeFromSBManager) {
			if(players.containsKey(p) && !players.get(p).equals("blacklisted"))
				ScoreboardManager.get(players.get(p)).removePlayer(p);
		}
		
		for(Team t : p.getScoreboard().getTeams()) {
			if(t.getName().startsWith("score-"))
				t.unregister();
		}
		
		Objective obj = p.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
		if(obj != null)
			obj.unregister();
		if(PowerBoard.debug)
			pl.getLogger().info("Removed "+p.getName()+"'s (old) scoreboard");
	}
}
