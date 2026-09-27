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

package com.majinnaibu.minecraft.plugins.scorekeeper.commands;

import java.util.Objects;

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreArchiveCommand implements CommandExecutor {
  private final ScoreKeeperPlugin _plugin;

  public ScoreArchiveCommand(ScoreKeeperPlugin scoreKeeperPlugin) {
    _plugin = scoreKeeperPlugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
    if (!sender.hasPermission("scorekeeper.admin")) {
      sendError(sender, "You do not have permission to archive scores");
      return true;
    }

    try {
      String archiveId;
      if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
        archiveId = _plugin.archiveAllScores();
      } else if (split.length == 2 && split[0].equalsIgnoreCase("bucket")) {
        archiveId = _plugin.archiveBucketScores(split[1]);
      } else if (split.length == 0 && sender instanceof Player player) {
        archiveId = _plugin.archivePlayerScore(player);
      } else if (split.length == 1) {
        Player target = _plugin.getServer().getPlayerExact(split[0]);
        if (target == null) {
          sendError(sender, "Can't find a player with that name");
          return true;
        }
        archiveId = _plugin.archivePlayerScore(target);
      } else {
        sendUsage(sender);
        return true;
      }
      _plugin.sendMessage(sender, Component.text("Saved score archive " + archiveId + "."));
    } catch (IllegalArgumentException ex) {
      sendError(sender, ex.getMessage());
    }
    return true;
  }

  private void sendError(CommandSender sender, String message) {
    _plugin.sendMessage(
        sender, Component.text(Objects.requireNonNull(message)).color(NamedTextColor.RED));
  }

  private void sendUsage(CommandSender sender) {
    _plugin.sendMessage(
        sender,
        Component.text("Usage: /score-archive [playerName|all] | /score-archive bucket <bucketId>")
            .color(NamedTextColor.YELLOW));
  }
}
