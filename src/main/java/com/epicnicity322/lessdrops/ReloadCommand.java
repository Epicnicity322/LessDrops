package com.epicnicity322.lessdrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (LessDrops.reload()) {
            sender.sendMessage(ChatColor.GREEN + "LessDrops reloaded successfully.");
        } else {
            sender.sendMessage(ChatColor.RED + "Something went wrong while reloading LessDrops. Please check console.");
        }
        return false;
    }
}
