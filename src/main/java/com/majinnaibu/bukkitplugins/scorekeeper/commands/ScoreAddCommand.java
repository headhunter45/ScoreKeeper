package com.majinnaibu.bukkitplugins.scorekeeper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.majinnaibu.bukkitplugins.scorekeeper.ScoreKeeperPlugin;

public class ScoreAddCommand implements CommandExecutor {
	private final ScoreKeeperPlugin _plugin;
	
	public ScoreAddCommand(ScoreKeeperPlugin scoreKeeperPlugin) {
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
		int amount = 0;
		Player targetPlayer = null;
		
		if(split.length == 1){
			if(rcon){
				echoUsage(sender, rcon);
				return true;
			}else{
				targetPlayer = (Player)sender;
				try{
					amount = Integer.parseInt(split[0]);
				}catch(NumberFormatException ex){
					echoError(sender, rcon, "amount must be an integer");
					return true;
				}
			}
		}else if(split.length == 2){
			targetPlayer = _plugin.getServer().getPlayer(split[0]);
			if(targetPlayer == null){
				echoError(sender, rcon, "Can't find a player with that name");
				return true;
			}
			
			try{
				amount = Integer.parseInt(split[1]);
			}catch(NumberFormatException ex){
				echoError(sender, rcon, "amount must be an integer");
				return true;
			}
		}else{
			echoUsage(sender, rcon);
			return true;
		}
		
		_plugin.addScore(targetPlayer,  amount);
		return true;
	}

	private void echoError(CommandSender sender, boolean rcon, String string) {
		sender.sendMessage("[" + ChatColor.RED + "ScoreKeeper" + ChatColor.WHITE + "] " + string);
	}

	private void echoUsage(CommandSender sender, boolean rcon) {
		if(rcon){
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage" + ChatColor.WHITE + ": score-add " + ChatColor.GREEN + "<playername> <amount>");
		}else{
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage" + ChatColor.WHITE + ": /score-add " + ChatColor.YELLOW + "[playername] " + ChatColor.GREEN + "<amount>");
		}
	}
}
