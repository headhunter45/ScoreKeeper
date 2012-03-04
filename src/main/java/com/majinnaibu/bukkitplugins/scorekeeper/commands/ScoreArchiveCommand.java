package com.majinnaibu.bukkitplugins.scorekeeper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.majinnaibu.bukkitplugins.scorekeeper.ScoreKeeperPlugin;

public class ScoreArchiveCommand implements CommandExecutor {
	private final ScoreKeeperPlugin _plugin;
	
	public ScoreArchiveCommand(ScoreKeeperPlugin scoreKeeperPlugin) {
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
		
		if(sender instanceof Player){
			Player player = (Player) sender;
			int score = _plugin.getPlayerScore(player);
			player.sendMessage("[" + ChatColor.AQUA + "ScoreKeeper" + ChatColor.WHITE + "] Your score is " + ChatColor.GREEN + score);
			return true;
		}else{
			return false;
		}
	}
}
