package de.xite.scoreboard.modules.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import de.xite.scoreboard.main.PowerBoard;
import de.xite.scoreboard.utils.Teams;
import de.xite.scoreboard.utils.Version;

public class ScoreboardPlayer {
	static PowerBoard pl = PowerBoard.pl;
	
	// All registered scoreboards
	public static HashMap<String, ScoreboardManager> scoreboards = new HashMap<>();
	
	// All players with scoreboards
	public static HashMap<Player, String> players = new HashMap<>(); // Player; Scoreboard config file name
	
	@SuppressWarnings("deprecation")
	public static void setScoreboard(Player p) {
		Scoreboard board = p.getScoreboard();
		
		// ---- Scoreboard ---- //
		removeScoreboard(p, false);
		Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
		if(obj == null) {
			// board = Bukkit.getScoreboardManager().getNewScoreboard();
			
			if(PowerBoard.getBukkitVersion().compareTo(new Version("1.13")) == 1 || PowerBoard.getBukkitVersion().equals(new Version("1.13"))) { // only for version 1.13+
				obj = board.registerNewObjective("aaa", "bbb", "SBPlugin");
			}else
				obj = board.registerNewObjective("aaa", "bbb");
		}
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		ScoreboardManager sm = getMatchingScoreboard(p);
		if(sm == null)
			return;
		sm.addPlayer(p);
		ScoreTitleUtils.setTitle(p, board, sm.getCurrentTitle(), true, sm); // Get the current title and set it
		ScoreTitleUtils.setScores(p, board, sm.getCurrentScores(), true, sm);
		
		// ---- Set the scoreboard ---- //
		p.setScoreboard(board);
		
		// Debug
		if(PowerBoard.debug)
			PowerBoard.pl.getLogger().info("Scoreboard set for player "+p.getName());
	}
	public static void updateScoreboard(Player p) {
		/* Config syntax: 
		conditions:
		  - world:world AND permission:some.permission
		  - world:world AND permission:some.other.permission
		  - world:world AND gamemode:creative
		  - world:world_nether
		*/
		if(!players.containsKey(p))
			return;

		ScoreboardManager newScoreboard = getMatchingScoreboard(p);
		if(newScoreboard == null)
			return;
		if(PowerBoard.debug)
			pl.getLogger().info("Changing "+p.getName()+"'s scoreboard to "+newScoreboard.getName());
		// Check if update is required
		if(!players.get(p).equals(newScoreboard.getName())) {
			ScoreboardPlayer.removeScoreboard(p, true);
			setScoreboard(p);
		}
	}
	public static ScoreboardManager getMatchingScoreboard(Player p) {
		for(Entry<String, ScoreboardManager> e : scoreboards.entrySet()) {
			ScoreboardManager sm = e.getValue();
			if(sm == null) {
				pl.getLogger().severe("Could not set scoreboard '"+sm+"'! File does not exists!");
				return null;
			}
			for(String condition : sm.conditions) { // For all "OR" conditions (lines)
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
					return sm;
			}
		}
		return ScoreboardManager.get(pl.getConfig().getString("scoreboard-default"));
	}
	public static void removeScoreboard(Player p, boolean removeTeams) {
		if(!players.containsKey(p))
			return;
		ScoreboardManager.get(players.get(p)).removePlayer(p);
		players.remove(p);
		
		if(removeTeams) {
			for(Team t : p.getScoreboard().getTeams())
				t.unregister();
			Teams.removePlayer(p);
		}
		Objective obj = p.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
		if(obj != null)
			obj.unregister();
		
		if(PowerBoard.debug)
			pl.getLogger().info("Removed "+p.getName()+"'s scoreboard");
	}
}
