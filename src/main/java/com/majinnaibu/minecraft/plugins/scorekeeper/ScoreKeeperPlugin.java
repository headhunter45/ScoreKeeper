package com.majinnaibu.minecraft.plugins.scorekeeper;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreAddCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreArchiveCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreGetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreResetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreSubtractCommand;

public class ScoreKeeperPlugin extends JavaPlugin {
	private final HashMap<Player, Integer> _playerScores = new HashMap<Player, Integer>();
	
	public final Logger log = Logger.getLogger("Minecraft");
	
	
	@Override
	public void onDisable() {
		//TODO: save score data to file
	}

	@Override
	public void onEnable() {
		//PluginManager pm = getServer().getPluginManager();
		//pm.registerEvent(Event.Type.BLOCK_PHYSICS, new BlockListener(){}, Priority.Normal, this);
		getCommand("score-get").setExecutor(new ScoreGetCommand(this));
		getCommand("score-add").setExecutor(new ScoreAddCommand(this));
		getCommand("score-subtract").setExecutor(new ScoreSubtractCommand(this));
		getCommand("score-reset").setExecutor(new ScoreResetCommand(this));
		getCommand("score-archive").setExecutor(new ScoreArchiveCommand(this));

		//TODO: load score data from file
		
		PluginDescriptionFile pdFile = this.getDescription();
		log.info(pdFile.getName() + " version " + pdFile.getVersion() + " is enabled!");
	}

	public int getPlayerScore(Player player) {
		if(!_playerScores.containsKey(player)){
			_playerScores.put(player, 0);
		}
		
		return _playerScores.get(player);
	}

	public void addScore(Player player, int amount) {
		int score = getPlayerScore(player);
		_playerScores.put(player,  score + amount);
	}

	public void subtractScore(Player player, int amount) {
		int score = getPlayerScore(player);
		_playerScores.put(player, score - amount);
	}

	public void resetPlayerScore(Player targetPlayer) {
		// TODO Auto-generated method stub
		
	}
}
