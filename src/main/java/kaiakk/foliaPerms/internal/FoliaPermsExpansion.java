package kaiakk.foliaPerms.internal;

import kaiakk.foliaPerms.FoliaPerms;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Custom PlaceholderAPI Expansion for FoliaPerms.
 * Exposes:
 * - %foliaperms_prefix% : Exposes the prioritized group prefix based on weight hierarchy.
 * - %foliaperms_primary_group% : Exposes the player's primary group name.
 * 
 * Version: 1.16.0
 */
public class FoliaPermsExpansion extends PlaceholderExpansion {
    private final FoliaPerms plugin;

    public FoliaPermsExpansion(FoliaPerms plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "synkfr";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "foliaperms";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.20.0";
    }

    @Override
    public boolean persist() {
        return true; // Keeps expansion registered through server reloads
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        
        if (params.equalsIgnoreCase("prefix")) {
            var service = plugin.getPermissionService();
            if (service != null) {
                return service.getPlayerPrefix(player.getUniqueId());
            }
        }
        
        if (params.equalsIgnoreCase("primary_group") || params.equalsIgnoreCase("group")) {
            var service = plugin.getPermissionService();
            if (service != null) {
                return service.getPlayerPrimaryGroup(player.getUniqueId());
            }
        }
        
        return null;
    }
}
