/*
This file is part of Score Keeper.

Score Keeper is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Score Keeper is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Score Keeper. If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
*/

package com.majinnaibu.minecraft.plugins.scorekeeper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreAddCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreArchiveCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreArchiveListCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreBucketCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreGetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreResetCommand;
import com.majinnaibu.minecraft.plugins.scorekeeper.commands.ScoreRunCommand;
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
  private final Map<String, HighScoreRun> _highScoreRuns = new LinkedHashMap<>();
  private final Map<String, ScoreArchive> _scoreArchives = new LinkedHashMap<>();
  private String _defaultBucketId = "points";
  public final String _logPrefix = "[ScoreKeeper] ";
  public final Component _messagePrefix =
      Component.text("[")
          .append(Component.text("ScoreKeeper").color(NamedTextColor.AQUA))
          .append(Component.text("] ").color(NamedTextColor.WHITE));

  @Override
  public void onDisable() {
    YamlConfiguration scores = new YamlConfiguration();
    scores.set("version", 3);
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
    for (HighScoreRun run : _highScoreRuns.values()) {
      String path = "high-score-runs." + run.bucketId;
      scores.set(path + ".top-n", run.topN);
      scores.set(path + ".reset-scores", run.resetScores);
      scores.set(path + ".lifetime", run.lifetime.name().toLowerCase(Locale.ROOT));
      scores.set(path + ".target-score", run.targetScore);
      scores.set(path + ".expires-at", run.expiresAtMillis);
      for (var entry : run.previousScores.entrySet()) {
        scores.set(path + ".previous-scores." + entry.getKey(), entry.getValue());
      }
    }
    try {
      scores.save(new File(getDataFolder(), "scores.yml"));
    } catch (IOException ex) {
      logError(ex);
    }
    saveArchives();
  }

  @Override
  public void onEnable() {
    Objects.requireNonNull(getCommand("score-get")).setExecutor(new ScoreGetCommand(this));
    Objects.requireNonNull(getCommand("score-add")).setExecutor(new ScoreAddCommand(this));
    Objects.requireNonNull(getCommand("score-subtract"))
        .setExecutor(new ScoreSubtractCommand(this));
    Objects.requireNonNull(getCommand("score-reset")).setExecutor(new ScoreResetCommand(this));
    Objects.requireNonNull(getCommand("score-archive")).setExecutor(new ScoreArchiveCommand(this));
    Objects.requireNonNull(getCommand("score-archive-list"))
        .setExecutor(new ScoreArchiveListCommand(this));
    Objects.requireNonNull(getCommand("score-bucket")).setExecutor(new ScoreBucketCommand(this));
    Objects.requireNonNull(getCommand("score-run")).setExecutor(new ScoreRunCommand(this));

    _buckets.put("points", new ScoreBucket("points", "point", "points", 0));
    _bucketScores.put("points", new LinkedHashMap<>());
    loadScores();
    loadArchives();
    checkHighScoreRuns();
    getServer().getScheduler().runTaskTimer(this, this::checkHighScoreRuns, 20L, 20L);

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
    archivePlayerScore(player);
  }

  public String archivePlayerScore(Player player) {
    String bucketId = getPlayerBucket(player);
    return createScoreArchive(
        "player " + player.getName() + " in " + bucketId, List.of(bucketId), player);
  }

  public String archiveBucketScores(String bucketId) {
    requireBucket(bucketId);
    return createScoreArchive("bucket " + bucketId, List.of(bucketId), null);
  }

  public String archiveAllScores() {
    return createScoreArchive("all buckets", new ArrayList<>(_buckets.keySet()), null);
  }

  public List<ScoreArchive> getScoreArchives() {
    return List.copyOf(_scoreArchives.values());
  }

  public ScoreArchive getScoreArchive(String archiveId) {
    return _scoreArchives.get(archiveId);
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
      HighScoreRun run = _highScoreRuns.get(bucketId);
      if (run == null) {
        reportScoreChange(player, bucket, score);
      } else {
        reportHighScoreRunChange(player, bucket, run, score);
        if (run.lifetime == HighScoreLifetime.TARGET_SCORE && score >= run.targetScore) {
          finishHighScoreRun(bucketId);
        }
      }
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
    ScoreBucket bucket = requireBucket(bucketId);
    UUID playerId = player.getUniqueId();
    String previousBucketId = getPlayerBucket(player);
    if (bucketId.equals(_defaultBucketId)) {
      _playerBuckets.remove(playerId);
    } else {
      _playerBuckets.put(playerId, bucketId);
    }
    if (!previousBucketId.equals(bucketId)) {
      sendBucketChangeMessage(player, bucket);
    }
  }

  public void switchAllPlayers(String bucketId) {
    requireBucket(bucketId);
    Map<UUID, String> previousBuckets = new LinkedHashMap<>();
    for (Player player : getServer().getOnlinePlayers()) {
      previousBuckets.put(Objects.requireNonNull(player).getUniqueId(), getPlayerBucket(player));
    }
    _defaultBucketId = bucketId;
    _playerBuckets.clear();
    ScoreBucket bucket = requireBucket(bucketId);
    for (Player player : getServer().getOnlinePlayers()) {
      if (!bucketId.equals(previousBuckets.get(Objects.requireNonNull(player).getUniqueId()))) {
        sendBucketChangeMessage(player, bucket);
      }
    }
  }

  public void startTargetScoreRun(String bucketId, int topN, boolean resetScores, int targetScore) {
    ScoreBucket bucket = requireBucket(bucketId);
    if (resetScores && targetScore <= bucket.getInitialValue()) {
      throw new IllegalArgumentException("Target score must exceed the bucket's initial value");
    }
    startHighScoreRun(bucketId, topN, resetScores, HighScoreLifetime.TARGET_SCORE, targetScore, 0L);
  }

  public void startTimedScoreRun(
      String bucketId, int topN, boolean resetScores, Duration duration) {
    if (duration == null || duration.isNegative() || duration.isZero()) {
      throw new IllegalArgumentException("Run duration must be positive");
    }
    long durationMillis;
    long expiresAtMillis;
    try {
      durationMillis = duration.toMillis();
      expiresAtMillis = Math.addExact(System.currentTimeMillis(), durationMillis);
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("Run duration is too large", ex);
    }
    if (durationMillis <= 0) {
      throw new IllegalArgumentException("Run duration must be at least one millisecond");
    }
    startHighScoreRun(bucketId, topN, resetScores, HighScoreLifetime.DURATION, 0, expiresAtMillis);
  }

  public void startUntilDisabledScoreRun(String bucketId, int topN, boolean resetScores) {
    startHighScoreRun(bucketId, topN, resetScores, HighScoreLifetime.UNTIL_DISABLED, 0, 0L);
  }

  public void stopHighScoreRun(String bucketId) {
    if (!_highScoreRuns.containsKey(bucketId)) {
      throw new IllegalArgumentException("No high-score run is active for bucket " + bucketId);
    }
    finishHighScoreRun(bucketId);
  }

  public boolean hasActiveHighScoreRun(String bucketId) {
    return _highScoreRuns.containsKey(bucketId);
  }

  // endregion

  // region Utiilty Methods
  public void sendMessage(CommandSender reciever, Component message) {
    reciever.sendMessage(_messagePrefix.append(Objects.requireNonNull(message)));
  }

  private ScoreBucket requireBucket(String bucketId) {
    ScoreBucket bucket = _buckets.get(bucketId);
    if (bucket == null) {
      throw new IllegalArgumentException("Unknown score bucket: " + bucketId);
    }
    return bucket;
  }

  private void sendBucketChangeMessage(Player player, ScoreBucket bucket) {
    int score = getScore(player, bucket.getId());
    String unit = score == 1 ? bucket.getSingular() : bucket.getPlural();
    sendMessage(
        player,
        Component.text(
            "You are now tracking " + bucket.getId() + ". You have " + score + " " + unit + "."));
  }

  private String createScoreArchive(String scope, List<String> bucketIds, Player player) {
    for (String bucketId : bucketIds) {
      if (_highScoreRuns.containsKey(bucketId)) {
        throw new IllegalArgumentException(
            "Stop the active high-score run for " + bucketId + " before archiving it");
      }
    }

    Map<String, Map<UUID, Integer>> archivedScores = new LinkedHashMap<>();
    UUID playerId = player == null ? null : player.getUniqueId();
    for (String bucketId : bucketIds) {
      ScoreBucket bucket = requireBucket(bucketId);
      Map<UUID, Integer> bucketScores = _bucketScores.get(bucketId);
      if (playerId == null) {
        archivedScores.put(bucketId, new LinkedHashMap<>(bucketScores));
        bucketScores.replaceAll((uuid, score) -> bucket.getInitialValue());
      } else {
        int score = getScore(player, bucketId);
        archivedScores.put(bucketId, Map.of(playerId, score));
        bucketScores.put(playerId, bucket.getInitialValue());
      }
    }

    String archiveId = UUID.randomUUID().toString();
    _scoreArchives.put(
        archiveId, new ScoreArchive(archiveId, scope, System.currentTimeMillis(), archivedScores));
    return archiveId;
  }

  private void startHighScoreRun(
      String bucketId,
      int topN,
      boolean resetScores,
      HighScoreLifetime lifetime,
      int targetScore,
      long expiresAtMillis) {
    ScoreBucket bucket = requireBucket(bucketId);
    if (topN < 1) {
      throw new IllegalArgumentException("Top N must be at least 1");
    }
    if (_highScoreRuns.containsKey(bucketId)) {
      throw new IllegalArgumentException("A high-score run is already active for " + bucketId);
    }

    Map<UUID, Integer> bucketScores = _bucketScores.get(bucketId);
    Map<UUID, Integer> previousScores = resetScores ? new LinkedHashMap<>(bucketScores) : Map.of();
    if (resetScores) {
      bucketScores.replaceAll((playerId, score) -> bucket.getInitialValue());
    }
    _highScoreRuns.put(
        bucketId,
        new HighScoreRun(
            bucketId, topN, resetScores, lifetime, targetScore, expiresAtMillis, previousScores));
    checkHighScoreRuns();
  }

  private void checkHighScoreRuns() {
    long now = System.currentTimeMillis();
    List<String> finishedRuns = new ArrayList<>();
    for (HighScoreRun run : _highScoreRuns.values()) {
      boolean expired = run.lifetime == HighScoreLifetime.DURATION && now >= run.expiresAtMillis;
      boolean targetReached =
          run.lifetime == HighScoreLifetime.TARGET_SCORE
              && _bucketScores.get(run.bucketId).values().stream()
                  .anyMatch(score -> score >= run.targetScore);
      if (expired || targetReached) {
        finishedRuns.add(run.bucketId);
      }
    }
    for (String bucketId : finishedRuns) {
      finishHighScoreRun(bucketId);
    }
  }

  private void reportHighScoreRunChange(
      Player player, ScoreBucket bucket, HighScoreRun run, int score) {
    String message = scoreMessage(player.getName(), bucket, score);
    sendMessage(player, Component.text(Objects.requireNonNull(message)));
    List<ScoreEntry> standings = getRankedScores(bucket.getId());
    for (int place = 0; place < Math.min(run.topN, standings.size()); place++) {
      if (standings.get(place).playerId.equals(player.getUniqueId())) {
        for (Player recipient : getServer().getOnlinePlayers()) {
          if (recipient != player) {
            sendMessage(recipient, Component.text(Objects.requireNonNull(message)));
          }
        }
        return;
      }
    }
  }

  private void finishHighScoreRun(String bucketId) {
    HighScoreRun run = _highScoreRuns.remove(bucketId);
    if (run == null) {
      return;
    }

    ScoreBucket bucket = requireBucket(bucketId);
    List<ScoreEntry> standings = getRankedScores(bucketId);
    StringBuilder table =
        new StringBuilder("High-score run for ").append(bucketId).append(" finished.");
    int places = Math.min(run.topN, standings.size());
    if (places == 0) {
      table.append("\nNo scores were recorded.");
    }
    for (int place = 0; place < places; place++) {
      ScoreEntry entry = standings.get(place);
      table
          .append("\n")
          .append(place + 1)
          .append(". ")
          .append(entry.playerName)
          .append(" - ")
          .append(entry.score)
          .append(" ")
          .append(scoreUnit(bucket, entry.score));
    }

    if (run.resetScores) {
      Map<UUID, Integer> bucketScores = _bucketScores.get(bucketId);
      bucketScores.clear();
      bucketScores.putAll(run.previousScores);
    }
    logInfo(table.toString().replace('\n', ' '));
    for (Player player : getServer().getOnlinePlayers()) {
      ScoreEntry personalEntry = null;
      int personalPlace = -1;
      for (int place = 0; place < standings.size(); place++) {
        if (standings.get(place).playerId.equals(Objects.requireNonNull(player).getUniqueId())) {
          personalEntry = standings.get(place);
          personalPlace = place + 1;
          break;
        }
      }
      int personalScore = personalEntry == null ? bucket.getInitialValue() : personalEntry.score;
      String personalResult =
          personalPlace < 0
              ? "Your place: unranked; score: "
                  + personalScore
                  + " "
                  + scoreUnit(bucket, personalScore)
              : "Your place: "
                  + personalPlace
                  + "; score: "
                  + personalScore
                  + " "
                  + scoreUnit(bucket, personalScore);
      sendMessage(player, Component.text(table + "\n" + personalResult));
    }
  }

  private List<ScoreEntry> getRankedScores(String bucketId) {
    List<ScoreEntry> standings = new ArrayList<>();
    for (var entry : _bucketScores.get(bucketId).entrySet()) {
      String playerName = getServer().getOfflinePlayer(entry.getKey()).getName();
      if (playerName == null || playerName.isBlank()) {
        playerName = entry.getKey().toString();
      }
      standings.add(new ScoreEntry(entry.getKey(), playerName, entry.getValue()));
    }
    standings.sort(
        Comparator.comparingInt((ScoreEntry entry) -> entry.score)
            .reversed()
            .thenComparing(entry -> entry.playerName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.playerId.toString()));
    return standings;
  }

  private String scoreMessage(String playerName, ScoreBucket bucket, int score) {
    return playerName
        + "'s "
        + bucket.getId()
        + " score is now "
        + score
        + " "
        + scoreUnit(bucket, score)
        + ".";
  }

  private String scoreUnit(ScoreBucket bucket, int score) {
    return score == 1 ? bucket.getSingular() : bucket.getPlural();
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
    loadHighScoreRuns(scores);
  }

  private void loadArchives() {
    File archiveFile = new File(getDataFolder(), "archives.yml");
    if (!archiveFile.isFile()) {
      return;
    }
    YamlConfiguration archives = YamlConfiguration.loadConfiguration(archiveFile);
    ConfigurationSection archiveSection = archives.getConfigurationSection("archives");
    if (archiveSection == null) {
      return;
    }
    for (String archiveId : archiveSection.getKeys(false)) {
      String path = "archives." + archiveId;
      Map<String, Map<UUID, Integer>> bucketScores = new LinkedHashMap<>();
      ConfigurationSection buckets = archives.getConfigurationSection(path + ".buckets");
      if (buckets != null) {
        for (String bucketId : buckets.getKeys(false)) {
          Map<UUID, Integer> playerScores = new LinkedHashMap<>();
          ConfigurationSection savedScores =
              archives.getConfigurationSection(path + ".buckets." + bucketId + ".scores");
          if (savedScores != null) {
            loadPlayerScores(savedScores, playerScores);
          }
          bucketScores.put(bucketId, playerScores);
        }
      }
      _scoreArchives.put(
          archiveId,
          new ScoreArchive(
              archiveId,
              archives.getString(path + ".scope", "unknown"),
              archives.getLong(path + ".created-at"),
              bucketScores));
    }
  }

  private void saveArchives() {
    YamlConfiguration archives = new YamlConfiguration();
    for (ScoreArchive archive : _scoreArchives.values()) {
      String path = "archives." + archive.getId();
      archives.set(path + ".scope", archive.getScope());
      archives.set(path + ".created-at", archive.getCreatedAtMillis());
      for (var bucketEntry : archive.getBucketScores().entrySet()) {
        for (var scoreEntry : bucketEntry.getValue().entrySet()) {
          archives.set(
              path + ".buckets." + bucketEntry.getKey() + ".scores." + scoreEntry.getKey(),
              scoreEntry.getValue());
        }
      }
    }
    try {
      archives.save(new File(getDataFolder(), "archives.yml"));
    } catch (IOException ex) {
      logError(ex);
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

  private void loadHighScoreRuns(YamlConfiguration scores) {
    ConfigurationSection runs = scores.getConfigurationSection("high-score-runs");
    if (runs == null) {
      return;
    }
    for (String bucketId : runs.getKeys(false)) {
      if (!_buckets.containsKey(bucketId)) {
        logWarning("Ignoring high-score run for unknown bucket " + bucketId + ".");
        continue;
      }
      try {
        String path = "high-score-runs." + bucketId;
        int topN = scores.getInt(path + ".top-n");
        if (topN < 1) {
          throw new IllegalArgumentException("Top N must be at least 1");
        }
        HighScoreLifetime lifetime =
            HighScoreLifetime.valueOf(
                scores.getString(path + ".lifetime", "until_disabled").toUpperCase(Locale.ROOT));
        Map<UUID, Integer> previousScores = new LinkedHashMap<>();
        ConfigurationSection savedScores =
            scores.getConfigurationSection(path + ".previous-scores");
        if (savedScores != null) {
          loadPlayerScores(savedScores, previousScores);
        }
        _highScoreRuns.put(
            bucketId,
            new HighScoreRun(
                bucketId,
                topN,
                scores.getBoolean(path + ".reset-scores"),
                lifetime,
                scores.getInt(path + ".target-score"),
                scores.getLong(path + ".expires-at"),
                previousScores));
      } catch (IllegalArgumentException ex) {
        logWarning(
            "Ignoring invalid high-score run for bucket " + bucketId + ": " + ex.getMessage());
      }
    }
  }

  private void reportScoreChange(Player player, ScoreBucket bucket, int score) {
    ScoreReporting reporting = bucket.getReporting();
    if (reporting == ScoreReporting.NONE) {
      return;
    }

    String text = scoreMessage(player.getName(), bucket, score);
    Component message = Component.text(Objects.requireNonNull(text));
    if (reporting == ScoreReporting.PLAYER) {
      sendMessage(player, message);
    } else if (reporting == ScoreReporting.GLOBAL) {
      for (Player recipient : getServer().getOnlinePlayers()) {
        sendMessage(recipient, message);
      }
    } else if (reporting == ScoreReporting.ADMIN) {
      logInfo(text);
      for (Player recipient : getServer().getOnlinePlayers()) {
        if (Objects.requireNonNull(recipient).isOp()
            || recipient.hasPermission("scorekeeper.admin")) {
          sendMessage(recipient, message);
        }
      }
    }
  }

  private enum HighScoreLifetime {
    TARGET_SCORE,
    DURATION,
    UNTIL_DISABLED
  }

  private static final class HighScoreRun {
    private final String bucketId;
    private final int topN;
    private final boolean resetScores;
    private final HighScoreLifetime lifetime;
    private final int targetScore;
    private final long expiresAtMillis;
    private final Map<UUID, Integer> previousScores;

    private HighScoreRun(
        String bucketId,
        int topN,
        boolean resetScores,
        HighScoreLifetime lifetime,
        int targetScore,
        long expiresAtMillis,
        Map<UUID, Integer> previousScores) {
      this.bucketId = bucketId;
      this.topN = topN;
      this.resetScores = resetScores;
      this.lifetime = lifetime;
      this.targetScore = targetScore;
      this.expiresAtMillis = expiresAtMillis;
      this.previousScores = new LinkedHashMap<>(previousScores);
    }
  }

  private record ScoreEntry(UUID playerId, String playerName, int score) {}

  public static final class ScoreArchive {
    private final String _id;
    private final String _scope;
    private final long _createdAtMillis;
    private final Map<String, Map<UUID, Integer>> _bucketScores;

    private ScoreArchive(
        String id,
        String scope,
        long createdAtMillis,
        Map<String, Map<UUID, Integer>> bucketScores) {
      _id = id;
      _scope = scope;
      _createdAtMillis = createdAtMillis;
      Map<String, Map<UUID, Integer>> copiedScores = new LinkedHashMap<>();
      for (var entry : bucketScores.entrySet()) {
        copiedScores.put(entry.getKey(), Map.copyOf(entry.getValue()));
      }
      _bucketScores = Collections.unmodifiableMap(copiedScores);
    }

    public String getId() {
      return _id;
    }

    public String getScope() {
      return _scope;
    }

    public long getCreatedAtMillis() {
      return _createdAtMillis;
    }

    public Map<String, Map<UUID, Integer>> getBucketScores() {
      return _bucketScores;
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
