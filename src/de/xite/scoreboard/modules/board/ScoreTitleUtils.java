package de.xite.scoreboard.modules.board;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import de.xite.scoreboard.main.PowerBoard;
import de.xite.scoreboard.utils.Placeholders;

public class ScoreTitleUtils {
	// ---- Set the scoreboard title ---- //
	public static boolean setTitle(Player p, String title, boolean usePlaceholders, ScoreboardManager sm) {
		Scoreboard board = p.getScoreboard();
		Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
		if(obj == null)
			return false;
		if(usePlaceholders)
			title = Placeholders.replace(p, title);
		
		//try {
			obj.setDisplayName(title);
		/*}catch (IllegalArgumentException e) {
			if(PowerBoard.aboveMC_1_13) { // In version 1.13 you can use up to 128 chars in the title
				obj.setDisplayName(ChatColor.RED+"Error: too long - see console");
				PowerBoard.pl.getLogger().warning(" ");
				PowerBoard.pl.getLogger().warning("-> The scoreboard title is too long! The limit is 128 chars!");
				if(sm != null)
					PowerBoard.pl.getLogger().warning("-> Scoreboard: "+sm.getName());
				PowerBoard.pl.getLogger().warning("-> Title: "+title);
				PowerBoard.pl.getLogger().warning("-> Player: "+p.getName());
				PowerBoard.pl.getLogger().warning(" ");
			}else {
				obj.setDisplayName(ChatColor.RED+"| too long |");
				PowerBoard.pl.getLogger().warning(" ");
				PowerBoard.pl.getLogger().warning("-> The scoreboard title is too long! The limit is 16 chars!");
				if(sm != null)
					PowerBoard.pl.getLogger().warning("-> Scoreboard: "+sm.getName());
				PowerBoard.pl.getLogger().warning("-> Title: "+title);
				PowerBoard.pl.getLogger().warning("-> Player: "+p.getName());
				PowerBoard.pl.getLogger().warning(" ");
			}
		}*/
		
		//if(sm != null)
			//sm.addPlayer(p);
		return true;
	}
	// ---- Set scores ---- //
	public static boolean setScores(Player p, ArrayList<String> scores, boolean usePlaceholders, ScoreboardManager sm) {
		Scoreboard board = p.getScoreboard();
		Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
		if(obj == null)
			return false;
		for(int i = 0; i < scores.size(); i++) {
			int id = scores.size()-i-1;
			setScore(p, scores.get(id), i, usePlaceholders, sm);
		}
		return true;
	}
	public static boolean setScore(Player p, String score, int ScoreID, boolean usePlaceholders, ScoreboardManager sm) {
		Scoreboard board = p.getScoreboard();
		Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
		if(obj == null)
			return false;
		String colorcode = "§"+ScoreID;
		if(ScoreID > 9) {
			if(ScoreID == 10)
				colorcode = "§a";
			if(ScoreID == 11)
				colorcode = "§b";
			if(ScoreID == 12)
				colorcode = "§c";
			if(ScoreID == 13)
				colorcode = "§d";
			if(ScoreID == 14)
				colorcode = "§e";
		}
		// If the scoreboard switches too fast (especially blacklisted) sometimes there will this error in the console: IllegalStateException: Unregistered scoreboard component
		// We can just ignore it because it doesn't seems like it has no effect other than that this error is beeing displayed.
		try {
			Team team = board.getTeam("score-"+ScoreID);
			if(team == null) {
				team = board.registerNewTeam("score-"+ScoreID);
				team.addEntry(colorcode);
				obj.getScore(colorcode).setScore(ScoreID);
			}
			if(score.length() == 0) // If lenght == 0 set to " " for free space in scoreboard
				score = " ";
			if(!score.equals(" ") && usePlaceholders)
				score = Placeholders.replace(p, score);
			
			// ---- Set all scores ---- //
			if(PowerBoard.aboveMC_1_13) { // In version 1.13 you can use up to 64 chars in prefix and suffix
				// Set the score for 1.13+
				String[] s = getScorePrefixSuffix(score, 64, 999);
				try {
					team.setPrefix(s[0]);
					team.setSuffix(s[1]);
				}catch (IllegalArgumentException e) {
					team.setPrefix(ChatColor.RED+"-too long-");
					PowerBoard.pl.getLogger().warning(" ");
					PowerBoard.pl.getLogger().warning("-> The scoreboard-score is too long! The limit is 128 chars!");
					if(sm != null)
						PowerBoard.pl.getLogger().warning("-> Scoreboard: "+sm.getName());
					PowerBoard.pl.getLogger().warning("-> Score: "+score);
					PowerBoard.pl.getLogger().warning("-> Player: "+p.getName());
					PowerBoard.pl.getLogger().warning(" ");
				}
			}else {
				// Set the score for 1.12-
				String[] s = getScorePrefixSuffix(score, 16, 30);
				if(s == null) {
					team.setPrefix(ChatColor.RED+"-too long-");
					PowerBoard.pl.getLogger().warning(" ");
					PowerBoard.pl.getLogger().warning("-> The scoreboard-score is too long! The limit is 30 chars!");
					if(sm != null)
						PowerBoard.pl.getLogger().warning("-> Scoreboard: "+sm.getName());
					PowerBoard.pl.getLogger().warning("-> Score: \""+score+"\", chars: "+score.length());
					PowerBoard.pl.getLogger().warning("-> Player: "+p.getName());
					PowerBoard.pl.getLogger().warning(" ");
				}else {
					team.setPrefix(s[0]);
					team.setSuffix(s[1]);
				}
			}
		}catch (IllegalStateException e) { }
		//if(sm != null)
			//sm.addPlayer(p);
		return true;
	}
	public static String[] getScorePrefixSuffix(String score, int split, int maxchars) {
		String[] s = new String[2];
		if(score.length() > maxchars)
			return null;
		
		if(score.length() > split) { // Check if suffix is needed
			s[0] = score.substring(0, split); // Set the prefix
			s[1] = ChatColor.getLastColors(s[0])+score.substring(split); // Get last color + everything in the string after the split
		}else {
			s[0] = score; // Set prefix
			s[1] = "";
		}
			
		return s;
	}
}
