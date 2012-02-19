package com.majinnaibu.bukkitplugins.scorekeeper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.majinnaibu.bukkitplugins.scorekeeper.ScoreKeeperPlugin;

public class ScoreGetCommand implements CommandExecutor {
	private final ScoreKeeperPlugin _plugin;
	
	public ScoreGetCommand(ScoreKeeperPlugin scoreKeeperPlugin) {
		_plugin = scoreKeeperPlugin;
	}

	@Override
	public boolean onCommand(
			CommandSender sender,
			Command command,
			String label,
			String[] split
	) {
		boolean rcon = !(sender instanceof Player);
		Player targetPlayer = null;
		
		if(split.length == 0){
			if(!rcon){
				targetPlayer = (Player)sender;
			}else{
				echoUsage(sender, rcon);
				return true; //false;
			}
		}else if(split.length == 1){
			targetPlayer = _plugin.getServer().getPlayer(split[0]);
		}else if(split.length > 1){
			echoUsage(sender, rcon);
			return true; //false;
		}
		
		if(targetPlayer == null){
			echoError(sender, rcon, "Can't find a player with that name");
			return true;
		}
		
		int score = _plugin.getPlayerScore(targetPlayer);
		
		if(!rcon){
			if(sender == targetPlayer){
				targetPlayer.sendMessage("[" + ChatColor.AQUA + "ScoreKeeper" + ChatColor.WHITE + "] Your score is " + ChatColor.GREEN + score);
			}else{
				sender.sendMessage("[" + ChatColor.AQUA + "ScoreKeeper" + ChatColor.WHITE + "] " + targetPlayer.getName() + "'s score is " + ChatColor.GREEN + score);
			}
			
			return true;
		}else{
			sender.sendMessage("[" + ChatColor.AQUA + "ScoreKeeper" + ChatColor.WHITE + "] " + targetPlayer.getName() + "'s score is " + ChatColor.GREEN + score);
			return true;
		}
	}

	private void echoError(CommandSender sender, boolean rcon, String string) {
		sender.sendMessage("[" + ChatColor.RED + "ScoreKeeper" + ChatColor.WHITE + "] " + string);
	}

	private void echoUsage(CommandSender sender, boolean rcon) {
		if(rcon){
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage" + ChatColor.WHITE + ": score-get " + ChatColor.GREEN + "<playername>");
		}else{
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage" + ChatColor.WHITE + ": /score-get " + ChatColor.YELLOW + "[playername]");
		}
		
	}	
}
