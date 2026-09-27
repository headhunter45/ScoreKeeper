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

package com.majinnaibu.minecraft.plugins.scorekeeper.commands;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreBucket;
import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ScoreArchiveListCommand implements CommandExecutor {
  private final ScoreKeeperPlugin _plugin;

  public ScoreArchiveListCommand(ScoreKeeperPlugin plugin) {
    _plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      var archives = _plugin.getScoreArchives();
      if (archives.isEmpty()) {
        _plugin.sendMessage(sender, Component.text("No score archives have been saved."));
      } else {
        archives.stream()
            .sorted(
                Comparator.comparingLong(
                        (ScoreKeeperPlugin.ScoreArchive archive) -> archive.getCreatedAtMillis())
                    .reversed())
            .forEach(
                archive ->
                    _plugin.sendMessage(
                        sender,
                        Component.text(
                            archive.getId()
                                + " | "
                                + archive.getScope()
                                + " | "
                                + Instant.ofEpochMilli(archive.getCreatedAtMillis()))));
      }
      return true;
    }

    if (args.length != 1) {
      sendUsage(sender);
      return true;
    }
    ScoreKeeperPlugin.ScoreArchive archive = _plugin.getScoreArchive(args[0]);
    if (archive == null) {
      _plugin.sendMessage(
          sender,
          Component.text("No score archive exists with ID " + args[0] + ".")
              .color(NamedTextColor.RED));
      return true;
    }

    _plugin.sendMessage(
        sender,
        Component.text(
            "Archive "
                + archive.getId()
                + " ("
                + archive.getScope()
                + ", "
                + Instant.ofEpochMilli(archive.getCreatedAtMillis())
                + ")"));
    for (var bucketEntry : archive.getBucketScores().entrySet()) {
      ScoreBucket bucket = _plugin.getBucket(bucketEntry.getKey());
      _plugin.sendMessage(sender, Component.text("Bucket " + bucketEntry.getKey() + ":"));
      bucketEntry.getValue().entrySet().stream()
          .sorted(
              Map.Entry.<UUID, Integer>comparingByValue()
                  .reversed()
                  .thenComparing(entry -> entry.getKey().toString()))
          .forEach(
              entry ->
                  _plugin.sendMessage(
                      sender,
                      Component.text(
                          displayName(entry.getKey())
                              + " - "
                              + entry.getValue()
                              + " "
                              + scoreUnit(bucket, entry.getValue()))));
    }
    return true;
  }

  private String displayName(UUID playerId) {
    OfflinePlayer player = _plugin.getServer().getOfflinePlayer(playerId);
    return player.getName() == null ? playerId.toString() : player.getName();
  }

  private String scoreUnit(ScoreBucket bucket, int score) {
    if (bucket == null) {
      return "scores";
    }
    return score == 1 ? bucket.getSingular() : bucket.getPlural();
  }

  private void sendUsage(CommandSender sender) {
    _plugin.sendMessage(
        sender,
        Component.text("Usage: /score-archive-list [archiveId]").color(NamedTextColor.YELLOW));
  }
}
