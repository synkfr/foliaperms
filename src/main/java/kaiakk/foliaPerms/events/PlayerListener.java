package kaiakk.foliaPerms.events;

import kaiakk.foliaPerms.FoliaPerms;
import kaiakk.foliaPerms.internal.ColorConverter;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

/**
 * Handles player-specific events for FoliaPerms.
 * Version: 1.13.0
 */
public class PlayerListener implements Listener {
    private final FoliaPerms plugin;

    public PlayerListener(FoliaPerms plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onLogin(org.bukkit.event.player.PlayerLoginEvent event) {
        Player player = event.getPlayer();
        try {
            var service = plugin.getPermissionService();
            if (service != null) {
                var userData = service.getUser(player.getUniqueId());
                if (userData == null || userData.getGroups().isEmpty()) {
                    service.addUserToGroup(player.getUniqueId(), "default");
                    service.saveAsync();
                    plugin.getLogger().info("Automatically assigned default group to first-time/groupless player " + player.getName() + " on login.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to automatically assign default group to " + player.getName() + " on login: " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Welcome message for admins
        if (player.hasPermission("folia.perms")) {
            String welcome = ColorConverter.colorize("&eFoliaPerms active!");
            player.sendMessage(welcome);
        }
        
        // Apply default group if first-time or groupless player joins (double-check fallback)
        boolean assignedDefault = false;
        try {
            var service = plugin.getPermissionService();
            if (service != null) {
                var userData = service.getUser(player.getUniqueId());
                if (userData == null || userData.getGroups().isEmpty()) {
                    service.addUserToGroup(player.getUniqueId(), "default");
                    service.saveAsync();
                    assignedDefault = true;
                    plugin.getLogger().info("Automatically assigned default group to first-time/groupless player " + player.getName() + " on join.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to automatically assign default group to " + player.getName() + " on join: " + e.getMessage());
        }
        
        // Apply permission attachment if not already applied via addUserToGroup
        if (!assignedDefault) {
            try {
                plugin.refreshPlayerAttachment(player);
                plugin.getLogger().fine("Applied permission attachment for " + player.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply permissions to " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up permission attachment
        try {
            plugin.removePlayerAttachment(player.getUniqueId());
            plugin.getLogger().fine("Cleaned up permission attachment for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up attachment for " + player.getName() + ": " + e.getMessage());
        }
    }
}