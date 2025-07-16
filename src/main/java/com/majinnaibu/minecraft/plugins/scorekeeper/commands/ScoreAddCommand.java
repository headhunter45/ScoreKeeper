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

import com.majinnaibu.minecraft.plugins.scorekeeper.ScoreKeeperPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreAddCommand implements CommandExecutor {
  private final ScoreKeeperPlugin _plugin;

  public ScoreAddCommand(ScoreKeeperPlugin scoreKeeperPlugin) {
    _plugin = scoreKeeperPlugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
    boolean rcon = !(sender instanceof Player);
    int amount = 0;
    Player targetPlayer = null;

    if (split.length == 1) {
      if (rcon) {
        echoUsage(sender, rcon);
        return true;
      } else {
        targetPlayer = (Player) sender;
        try {
          amount = Integer.parseInt(split[0]);
        } catch (NumberFormatException ex) {
          echoError(sender, rcon, "amount must be an integer");
          return true;
        }
      }
    } else if (split.length == 2) {
      targetPlayer = _plugin.getServer().getPlayerExact(split[0]);
      if (targetPlayer == null) {
        echoError(sender, rcon, "Can't find a player with that name");
        return true;
      }

      try {
        amount = Integer.parseInt(split[1]);
      } catch (NumberFormatException ex) {
        echoError(sender, rcon, "amount must be an integer");
        return true;
      }
    } else {
      echoUsage(sender, rcon);
      return true;
    }

    _plugin.addScore(targetPlayer, amount);
    return true;
  }

  private void echoError(CommandSender sender, boolean rcon, String string) {
    _plugin.sendMessage(sender, Component.text(string).color(NamedTextColor.RED));
  }

  private void echoUsage(CommandSender sender, boolean rcon) {
    if (rcon) {
      Component message =
          Component.text("Usage")
              .color(NamedTextColor.DARK_PURPLE)
              .append(Component.text(": score-add ").color(NamedTextColor.WHITE))
              .append(Component.text("<playerName> <amount>").color(NamedTextColor.GREEN));
      _plugin.sendMessage(sender, message);
    } else {
      Component message =
          Component.text("Usage")
              .color(NamedTextColor.DARK_PURPLE)
              .append(Component.text(": /score-add ").color(NamedTextColor.WHITE))
              .append(Component.text("[playerName] ").color(NamedTextColor.YELLOW))
              .append(Component.text("<amount>").color(NamedTextColor.GREEN));
      _plugin.sendMessage(sender, message);
    }
  }
}
