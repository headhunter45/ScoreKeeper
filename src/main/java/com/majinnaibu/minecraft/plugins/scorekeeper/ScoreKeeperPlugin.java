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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreAddCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreArchiveCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreBucketCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreGetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreResetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreSubtractCommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ScoreKeeperPlugin extends JavaPlugin {
  private final Map<String, ScoreBucket> _buckets = new LinkedHashMap<>();
  private final Map<String, Map<UUID, Integer>> _bucketScores = new LinkedHashMap<>();
  private final Map<UUID, String> _playerBuckets = new LinkedHashMap<>();
  private String _defaultBucketId = "points";
  public final String _logPrefix = "[ScoreKeeper] ";
  public final Component _messagePrefix =
      Component.text("[")
          .append(Component.text("ScoreKeeper").color(NamedTextColor.AQUA))
          .append(Component.text("] ").color(NamedTextColor.WHITE));

  @Override
  public void onDisable() {
    YamlConfiguration scores = new YamlConfiguration();
    scores.set("version", 2);
    scores.set("default-bucket", _defaultBucketId);
    for (ScoreBucket bucket : _buckets.values()) {
      String path = "buckets." + bucket.getId();
      scores.set(path + ".singular", bucket.getSingular());
      scores.set(path + ".plural", bucket.getPlural());
      scores.set(path + ".initial-value", bucket.getInitialValue());
      scores.set(path + ".reporting", bucket.getReporting().name().toLowerCase());
      for (var entry : _bucketScores.get(bucket.getId()).entrySet()) {
        scores.set(path + ".scores." + entry.getKey(), entry.getValue());
      }
    }
    for (var entry : _playerBuckets.entrySet()) {
      scores.set("player-buckets." + entry.getKey(), entry.getValue());
    }
    try {
      scores.save(new File(getDataFolder(), "scores.yml"));
    } catch (IOException ex) {
      logError(ex);
    }
  }

  @Override
  public void onEnable() {
    getCommand("score-get").setExecutor(new ScoreGetCommand(this));
    getCommand("score-add").setExecutor(new ScoreAddCommand(this));
    getCommand("score-subtract").setExecutor(new ScoreSubtractCommand(this));
    getCommand("score-reset").setExecutor(new ScoreResetCommand(this));
    getCommand("score-archive").setExecutor(new ScoreArchiveCommand(this));
    getCommand("score-bucket").setExecutor(new ScoreBucketCommand(this));

    _buckets.put("points", new ScoreBucket("points", "point", "points", 0));
    _bucketScores.put("points", new LinkedHashMap<>());
    loadScores();

    logInfo(
        getPluginMeta().getName() + " version " + getPluginMeta().getVersion() + " is enabled.");
  }

  // region Commands
  public void addScore(Player player, int amount) {
    addScore(player, getPlayerBucket(player), amount);
  }

  public void addScore(Player player, String bucketId, int amount) {
    setScore(player, bucketId, getScore(player, bucketId) + amount);
  }

  public void archiveScore(Player player) {
    logWarning("Unable to archive score for " + player.getName() + ".");
  }

  public int getScore(Player player) {
    return getScore(player, getPlayerBucket(player));
  }

  public int getScore(Player player, String bucketId) {
    ScoreBucket bucket = requireBucket(bucketId);
    return _bucketScores
        .get(bucketId)
        .computeIfAbsent(player.getUniqueId(), uuid -> bucket.getInitialValue());
  }

  public void resetScore(Player player) {
    resetScore(player, getPlayerBucket(player));
  }

  public void resetScore(Player player, String bucketId) {
    setScore(player, bucketId, requireBucket(bucketId).getInitialValue());
  }

  public void setScore(Player player, int score) {
    setScore(player, getPlayerBucket(player), score);
  }

  public void setScore(Player player, String bucketId, int score) {
    ScoreBucket bucket = requireBucket(bucketId);
    int oldScore = getScore(player, bucketId);
    _bucketScores.get(bucketId).put(player.getUniqueId(), score);
    if (oldScore != score) {
      reportScoreChange(player, bucket, score);
    }
  }

  public void subtractScore(Player player, int amount) {
    subtractScore(player, getPlayerBucket(player), amount);
  }

  public void subtractScore(Player player, String bucketId, int amount) {
    setScore(player, bucketId, getScore(player, bucketId) - amount);
  }

  public ScoreBucket createBucket(String id, String singular, String plural, int initialValue) {
    return createBucket(id, singular, plural, initialValue, ScoreReporting.NONE);
  }

  public ScoreBucket createBucket(
      String id, String singular, String plural, int initialValue, ScoreReporting reporting) {
    ScoreBucket bucket = new ScoreBucket(id, singular, plural, initialValue, reporting);
    if (_buckets.containsKey(id)) {
      throw new IllegalArgumentException("A score bucket with ID " + id + " already exists");
    }
    _buckets.put(id, bucket);
    _bucketScores.put(id, new LinkedHashMap<>());
    return bucket;
  }

  public ScoreBucket getBucket(String bucketId) {
    return _buckets.get(bucketId);
  }

  public Map<String, ScoreBucket> getBuckets() {
    return Map.copyOf(_buckets);
  }

  public String getPlayerBucket(Player player) {
    return _playerBuckets.getOrDefault(player.getUniqueId(), _defaultBucketId);
  }

  public void switchPlayerBucket(Player player, String bucketId) {
    requireBucket(bucketId);
    UUID playerId = player.getUniqueId();
    if (bucketId.equals(_defaultBucketId)) {
      _playerBuckets.remove(playerId);
    } else {
      _playerBuckets.put(playerId, bucketId);
    }
  }

  public void switchAllPlayers(String bucketId) {
    requireBucket(bucketId);
    _defaultBucketId = bucketId;
    _playerBuckets.clear();
  }

  // endregion

  // region Utiilty Methods
  public void sendMessage(CommandSender reciever, Component message) {
    reciever.sendMessage(_messagePrefix.append(message));
  }

  private ScoreBucket requireBucket(String bucketId) {
    ScoreBucket bucket = _buckets.get(bucketId);
    if (bucket == null) {
      throw new IllegalArgumentException("Unknown score bucket: " + bucketId);
    }
    return bucket;
  }

  private void loadScores() {
    File scoreFile = new File(getDataFolder(), "scores.yml");
    if (!scoreFile.isFile()) {
      return;
    }

    YamlConfiguration scores = YamlConfiguration.loadConfiguration(scoreFile);
    ConfigurationSection bucketSection = scores.getConfigurationSection("buckets");
    if (bucketSection == null) {
      loadLegacyScores(scores);
      return;
    }

    _buckets.clear();
    _bucketScores.clear();
    for (String id : bucketSection.getKeys(false)) {
      try {
        ScoreReporting reporting =
            parseReporting(bucketSection.getString(id + ".reporting", "none"), id);
        createBucket(
            id,
            bucketSection.getString(id + ".singular", id),
            bucketSection.getString(id + ".plural", id),
            bucketSection.getInt(id + ".initial-value"),
            reporting);
      } catch (IllegalArgumentException ex) {
        logWarning("Ignoring invalid score bucket " + id + ": " + ex.getMessage());
      }
    }
    if (!_buckets.containsKey("points")) {
      createBucket("points", "point", "points", 0);
    }
    _defaultBucketId = scores.getString("default-bucket", "points");
    if (!_buckets.containsKey(_defaultBucketId)) {
      logWarning("Unknown default score bucket; using points instead.");
      _defaultBucketId = "points";
    }

    for (String id : _buckets.keySet()) {
      ConfigurationSection bucketScores =
          scores.getConfigurationSection("buckets." + id + ".scores");
      if (bucketScores != null) {
        loadPlayerScores(bucketScores, _bucketScores.get(id));
      }
    }
    ConfigurationSection playerBuckets = scores.getConfigurationSection("player-buckets");
    if (playerBuckets != null) {
      for (String key : playerBuckets.getKeys(false)) {
        try {
          UUID playerId = UUID.fromString(key);
          String bucketId = playerBuckets.getString(key);
          if (_buckets.containsKey(bucketId) && !bucketId.equals(_defaultBucketId)) {
            _playerBuckets.put(playerId, bucketId);
          }
        } catch (IllegalArgumentException ex) {
          logWarning("Ignoring invalid player bucket UUID: " + key);
        }
      }
    }
  }

  private void loadLegacyScores(YamlConfiguration scores) {
    loadPlayerScores(scores, _bucketScores.get("points"));
  }

  private void loadPlayerScores(ConfigurationSection scores, Map<UUID, Integer> target) {
    for (String key : scores.getKeys(false)) {
      try {
        target.put(UUID.fromString(key), scores.getInt(key));
      } catch (IllegalArgumentException ex) {
        logWarning("Ignoring score with invalid player UUID: " + key);
      }
    }
  }

  private ScoreReporting parseReporting(String value, String bucketId) {
    try {
      return ScoreReporting.fromString(value);
    } catch (IllegalArgumentException ex) {
      logWarning("Ignoring invalid reporting policy for bucket " + bucketId + "; using none.");
      return ScoreReporting.NONE;
    }
  }

  private void reportScoreChange(Player player, ScoreBucket bucket, int score) {
    ScoreReporting reporting = bucket.getReporting();
    if (reporting == ScoreReporting.NONE) {
      return;
    }

    String unit = score == 1 ? bucket.getSingular() : bucket.getPlural();
    String text =
        player.getName() + "'s " + bucket.getId() + " score is now " + score + " " + unit + ".";
    Component message = Component.text(text);
    if (reporting == ScoreReporting.PLAYER) {
      sendMessage(player, message);
    } else if (reporting == ScoreReporting.GLOBAL) {
      for (Player recipient : getServer().getOnlinePlayers()) {
        sendMessage(recipient, message);
      }
    } else if (reporting == ScoreReporting.ADMIN) {
      logInfo(text);
      for (Player recipient : getServer().getOnlinePlayers()) {
        if (recipient.isOp() || recipient.hasPermission("scorekeeper.admin")) {
          sendMessage(recipient, message);
        }
      }
    }
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
