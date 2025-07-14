package com.majinnaibu.minecraft.plugins.scorekeeper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;

public class ScoreResetCommand implements CommandExecutor {
	private final ScoreKeeperPlugin _plugin;
	
	public ScoreResetCommand(ScoreKeeperPlugin scoreKeeperPlugin) {
		_plugin = scoreKeeperPlugin;
	}

	@Override
	public boolean onCommand(
			CommandSender sender,
			Command command,
			String label,
			String[] split
	) {
		sender.sendMessage("[" + ChatColor.AQUA + "ScoreKeeper" + ChatColor.WHITE + "] archive command unimplemented ");
	
		boolean rcon = !(sender instanceof Player);
		Player targetPlayer = null;
		
		if(split.length == 1){
			if(rcon){
				echoUsage(sender, rcon);
				return true;
			}else{
				targetPlayer = (Player)sender;
				try{
					
				}catch(NumberFormatException ex){
					echoError(sender, rcon, "amount must be an integer");
					return true;
				}
			}
		}else if(split.length == 2){
			targetPlayer = _plugin.getServer().getPlayerExact(split[0]);
			if(targetPlayer == null){
				echoError(sender, rcon, "Can't find a player with that name");
				return true;
			}
			
			try{
				
			}catch(NumberFormatException ex){
				echoError(sender, rcon, "amount must be an integer");
				return true;
			}
		}else{
			echoUsage(sender, rcon);
			return true;
		}
		
		_plugin.resetPlayerScore(targetPlayer);
		return true;
	}
	
	private void echoError(CommandSender sender, boolean rcon, String string) {
		sender.sendMessage("[" + ChatColor.RED + "ScoreKeeper" + ChatColor.WHITE + "] " + string);
	}

	private void echoUsage(CommandSender sender, boolean rcon) {
		if(rcon){
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage" + ChatColor.WHITE + ": score-reset " + ChatColor.GREEN + "<playername>");
		}else{
			sender.sendMessage(ChatColor.DARK_PURPLE + "Usage" + ChatColor.WHITE + ": /score-reset " + ChatColor.YELLOW + "[playername]");
		}
	}
}
