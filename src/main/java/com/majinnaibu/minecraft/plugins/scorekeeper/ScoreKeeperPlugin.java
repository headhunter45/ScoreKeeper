package com.majinnaibu.minecraft.plugins.scorekeeper;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreAddCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreArchiveCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreGetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreResetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreSubtractCommand;

public class ScoreKeeperPlugin extends JavaPlugin {
	private final HashMap<UUID, Integer> _playerScores = new HashMap<UUID, Integer>();

	@Override
	public void onDisable() {
		//TODO: save score data to file
	}

	@Override
	public void onEnable() {
		getCommand("score-get").setExecutor(new ScoreGetCommand(this));
		getCommand("score-add").setExecutor(new ScoreAddCommand(this));
		getCommand("score-subtract").setExecutor(new ScoreSubtractCommand(this));
		getCommand("score-reset").setExecutor(new ScoreResetCommand(this));
		getCommand("score-archive").setExecutor(new ScoreArchiveCommand(this));

		//TODO: load score data from file
		
		PluginDescriptionFile pdFile = this.getDescription();
		getLogger().info(pdFile.getName() + " version " + pdFile.getVersion() + " is enabled!");
	}

	public int getPlayerScore(Player player) {
		UUID uuid = player.getUniqueId();
		if(!_playerScores.containsKey(uuid)){
			_playerScores.put(uuid, 0);
		}
		
		return _playerScores.get(uuid);
	}

	public void addScore(Player player, int amount) {
		UUID uuid = player.getUniqueId();
		int score = getPlayerScore(player);
		_playerScores.put(uuid,  score + amount);
	}

	public void subtractScore(Player player, int amount) {
		UUID uuid = player.getUniqueId();
		int score = getPlayerScore(player);
		_playerScores.put(uuid, score - amount);
	}

	public void resetPlayerScore(Player targetPlayer) {
		// TODO Auto-generated method stub
		
	}
}
