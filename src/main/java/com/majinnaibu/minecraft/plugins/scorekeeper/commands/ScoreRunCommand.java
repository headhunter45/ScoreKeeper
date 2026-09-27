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

import java.time.Duration;

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ScoreRunCommand implements CommandExecutor {
  private final ScoreKeeperPlugin _plugin;

  public ScoreRunCommand(ScoreKeeperPlugin plugin) {
    _plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("scorekeeper.admin")) {
      sendError(sender, "You do not have permission to manage high-score runs");
      return true;
    }

    if (args.length == 2 && args[0].equalsIgnoreCase("stop")) {
      try {
        _plugin.stopHighScoreRun(args[1]);
      } catch (IllegalArgumentException ex) {
        sendError(sender, ex.getMessage());
      }
      return true;
    }

    if (args.length < 5 || !args[0].equalsIgnoreCase("start")) {
      sendUsage(sender);
      return true;
    }

    String bucketId = args[1];
    try {
      int topN = Integer.parseInt(args[2]);
      boolean resetScores = parseBoolean(args[3]);
      String lifetime = args[4].toLowerCase();
      if (lifetime.equals("target") && args.length == 6) {
        _plugin.startTargetScoreRun(bucketId, topN, resetScores, Integer.parseInt(args[5]));
      } else if (lifetime.equals("duration") && args.length == 6) {
        _plugin.startTimedScoreRun(bucketId, topN, resetScores, parseDuration(args[5]));
      } else if (lifetime.equals("until-disabled") && args.length == 5) {
        _plugin.startUntilDisabledScoreRun(bucketId, topN, resetScores);
      } else {
        sendUsage(sender);
        return true;
      }
      _plugin.sendMessage(sender, Component.text("Started a high-score run for " + bucketId + "."));
    } catch (IllegalArgumentException ex) {
      sendError(sender, ex.getMessage());
    }
    return true;
  }

  private boolean parseBoolean(String value) {
    if (value.equalsIgnoreCase("true")) {
      return true;
    }
    if (value.equalsIgnoreCase("false")) {
      return false;
    }
    throw new IllegalArgumentException("Reset scores must be true or false");
  }

  private Duration parseDuration(String value) {
    if (!value.matches("[0-9]+[smhdw]")) {
      throw new IllegalArgumentException(
          "Duration must use seconds, minutes, hours, days or weeks (e.g. 4h)");
    }
    long amount = Long.parseLong(value.substring(0, value.length() - 1));
    try {
      return switch (value.charAt(value.length() - 1)) {
        case 's' -> Duration.ofSeconds(amount);
        case 'm' -> Duration.ofMinutes(amount);
        case 'h' -> Duration.ofHours(amount);
        case 'd' -> Duration.ofDays(amount);
        case 'w' -> Duration.ofDays(Math.multiplyExact(amount, 7));
        default -> throw new IllegalArgumentException("Unsupported duration unit");
      };
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("Duration is too large", ex);
    }
  }

  private void sendError(CommandSender sender, String message) {
    _plugin.sendMessage(sender, Component.text(message).color(NamedTextColor.RED));
  }

  private void sendUsage(CommandSender sender) {
    _plugin.sendMessage(
        sender,
        Component.text(
                "Usage: /score-run start <bucket> <topN> <reset:true|false> "
                    + "<target <score>|duration <time>|until-disabled> | /score-run stop <bucket>")
            .color(NamedTextColor.YELLOW));
  }
}
