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

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreBucket;
import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;
import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreReporting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreBucketCommand implements CommandExecutor {
  private final ScoreKeeperPlugin _plugin;

  public ScoreBucketCommand(ScoreKeeperPlugin plugin) {
    _plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      if (sender instanceof Player player) {
        String bucketId = _plugin.getPlayerBucket(player);
        ScoreBucket bucket = _plugin.getBucket(bucketId);
        _plugin.sendMessage(
            sender,
            Component.text(
                "You are tracking " + bucket.getId() + " (" + bucket.getPlural() + ")."));
      } else {
        sendUsage(sender);
      }
      return true;
    }

    if ((args.length == 5 || args.length == 6) && args[0].equalsIgnoreCase("create")) {
      if (!requireAdmin(sender)) {
        return true;
      }
      try {
        ScoreReporting reporting =
            args.length == 6 ? ScoreReporting.fromString(args[5]) : ScoreReporting.NONE;
        _plugin.createBucket(args[1], args[2], args[3], Integer.parseInt(args[4]), reporting);
        _plugin.sendMessage(sender, Component.text("Created score bucket " + args[1] + "."));
      } catch (IllegalArgumentException ex) {
        sendError(sender, ex.getMessage());
      }
      return true;
    }

    if (args.length == 1 && sender instanceof Player player) {
      switchPlayerBucket(sender, player, args[0]);
      return true;
    }

    if (args.length == 2) {
      if (!requireAdmin(sender)) {
        return true;
      }
      if (args[0].equalsIgnoreCase("all")) {
        try {
          _plugin.switchAllPlayers(args[1]);
          _plugin.sendMessage(
              sender, Component.text("All players are now tracking " + args[1] + "."));
        } catch (IllegalArgumentException ex) {
          sendError(sender, ex.getMessage());
        }
        return true;
      }

      Player target = _plugin.getServer().getPlayerExact(args[0]);
      if (target == null) {
        sendError(sender, "Can't find a player with that name");
      } else {
        switchPlayerBucket(sender, target, args[1]);
      }
      return true;
    }

    sendUsage(sender);
    return true;
  }

  private void switchPlayerBucket(CommandSender sender, Player target, String bucketId) {
    try {
      _plugin.switchPlayerBucket(target, bucketId);
      if (sender != target) {
        _plugin.sendMessage(
            sender, Component.text(target.getName() + " is now tracking " + bucketId + "."));
      }
    } catch (IllegalArgumentException ex) {
      sendError(sender, ex.getMessage());
    }
  }

  private boolean requireAdmin(CommandSender sender) {
    if (sender.hasPermission("scorekeeper.admin")) {
      return true;
    }
    sendError(sender, "You do not have permission to manage score buckets");
    return false;
  }

  private void sendError(CommandSender sender, String message) {
    _plugin.sendMessage(sender, Component.text(Objects.requireNonNull(message)).color(NamedTextColor.RED));
  }

  private void sendUsage(CommandSender sender) {
    _plugin.sendMessage(
        sender,
        Component.text(
                "Usage: /score-bucket [bucketId] | create <id> <singular> <plural> <initialValue>"
                    + " | <player|all> <bucketId>")
            .color(NamedTextColor.YELLOW));
  }
}
