package com.majinnaibu.minecraft.plugins.scorekeeper.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;

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
		_plugin.sendMessage(sender, Component.text("archive command unimplemented"));
		return true;
	}
}
