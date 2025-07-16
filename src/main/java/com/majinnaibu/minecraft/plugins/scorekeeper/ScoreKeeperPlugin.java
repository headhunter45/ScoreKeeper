/*
This file is part of ScoreKeeper.

ScoreKeeper is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ScoreKeeper is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with ScoreKeeper. If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
*/

package com.majinnaibu.minecraft.plugins.scorekeeper;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreAddCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreArchiveCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreGetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreResetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreSubtractCommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ScoreKeeperPlugin extends JavaPlugin {
  private final HashMap<UUID, Integer> _playerScores = new HashMap<UUID, Integer>();
  public final String _logPrefix = "[ScoreKeeper] ";
  public final Component _messagePrefix =
      Component.text("[")
          .append(Component.text("ScoreKeeper").color(NamedTextColor.AQUA))
          .append(Component.text("] ").color(NamedTextColor.WHITE));

  @Override
  public void onDisable() {
    // TODO: save score data to file
    logWarning("Unable to save scores to file. This feature is not implemented yet.");
  }

  @Override
  public void onEnable() {
    getCommand("score-get").setExecutor(new ScoreGetCommand(this));
    getCommand("score-add").setExecutor(new ScoreAddCommand(this));
    getCommand("score-subtract").setExecutor(new ScoreSubtractCommand(this));
    getCommand("score-reset").setExecutor(new ScoreResetCommand(this));
    getCommand("score-archive").setExecutor(new ScoreArchiveCommand(this));

    // TODO: load score data from file
    logWarning("Unable to load scores from file. This feature is not " + "implemented yet.");

    logInfo(
        getPluginMeta().getName() + " version " + getPluginMeta().getVersion() + " is enabled.");
  }

  // region Commands
  public void addScore(Player player, int amount) {
    int oldScore = getPlayerScore(player);
    setPlayerScore(player, oldScore + amount);
  }

  public void archiveScore(Player player) {
    logWarning("Unable to archive score for " + player.getName() + ".");
  }

  public int getScore(Player player) {
    return getPlayerScore(player);
  }

  public void resetScore(Player player) {
    setPlayerScore(player, 0);
  }

  public void setScore(Player player, int score) {
    setPlayerScore(player, score);
  }

  public void subtractScore(Player player, int amount) {
    int oldScore = getPlayerScore(player);
    setPlayerScore(player, oldScore - amount);
  }

  // endregion

  // region Utiilty Methods
  public void sendMessage(CommandSender reciever, Component message) {
    reciever.sendMessage(_messagePrefix.append(message));
  }

  private int getPlayerScore(Player player) {
    UUID uuid = player.getUniqueId();
    if (!_playerScores.containsKey(uuid)) {
      _playerScores.put(uuid, 0);
    }
    return _playerScores.get(uuid);
  }

  private void setPlayerScore(Player player, int score) {
    _playerScores.put(player.getUniqueId(), score);
  }

  // endregion

  // region Logging
  public void logError(Exception ex) {
    getLogger().log(Level.SEVERE, _logPrefix + ex.toString());
  }

  public void logInfo(String message) {
    getLogger().info(_logPrefix + message);
  }

  public void logWarning(String message) {
    getLogger().warning(_logPrefix + message);
  }
  // endregion
}
