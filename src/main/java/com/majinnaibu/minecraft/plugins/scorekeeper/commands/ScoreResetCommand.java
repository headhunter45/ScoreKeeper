package com.majinnaibu.minecraft.plugins.scorekeeper.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
		boolean rcon = !(sender instanceof Player);
		Player targetPlayer = null;
		
		if(split.length == 0){
			if(rcon){
				echoUsage(sender, rcon);
				return true;
			}else{
				targetPlayer = (Player)sender;
			}
		}else if(split.length == 1){
			targetPlayer = _plugin.getServer().getPlayerExact(split[0]);
			if(targetPlayer == null){
				echoError(sender, rcon, "Can't find a player with that name");
				return true;
			}
		}else{
			echoUsage(sender, rcon);
			return true;
		}
		
		_plugin.resetScore(targetPlayer);
		return true;
	}
	
	private void echoError(CommandSender sender, boolean rcon, String string) {
		_plugin.sendMessage(sender, Component.text(string).color(NamedTextColor.RED));
	}

	private void echoUsage(CommandSender sender, boolean rcon) {
		if(rcon){
			Component message = 
				Component.text("Usage").color(NamedTextColor.DARK_PURPLE)
				.append(Component.text(": score-reset ").color(NamedTextColor.WHITE))
				.append(Component.text("<playerName>").color(NamedTextColor.GREEN));
			_plugin.sendMessage(sender, message);
		}else{
			Component message = 
				Component.text("Usage").color(NamedTextColor.DARK_PURPLE)
				.append(Component.text(": /score-reset ").color(NamedTextColor.WHITE))
				.append(Component.text("[playerName]").color(NamedTextColor.YELLOW));
			_plugin.sendMessage(sender, message);
		}
	}
}
