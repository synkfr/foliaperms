package kaiakk.foliaPerms.internal;

import kaiakk.foliaPerms.FoliaPerms;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;

/**
 * Custom Permissible implementation for FoliaPerms to intercept all Bukkit permission queries dynamically.
 * Version: 1.15.0
 */
public class FoliaPermissible extends PermissibleBase {
    private final Player player;
    private final FoliaPerms plugin;

    public FoliaPermissible(Player player, FoliaPerms plugin) {
        super(player);
        this.player = player;
        this.plugin = plugin;
    }

    @Override
    public boolean isPermissionSet(String name) {
        if (name == null) return false;
        var service = plugin.getPermissionService();
        if (service != null && service.hasPermission(player.getUniqueId(), name)) {
            return true;
        }
        return super.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        if (perm == null) return false;
        return isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(String inName) {
        if (inName == null) return false;
        
        // 1. Query FoliaPerms in-memory permission service directly
        var service = plugin.getPermissionService();
        if (service != null && service.hasPermission(player.getUniqueId(), inName)) {
            return true;
        }
        
        // 2. Fall back to standard PermissibleBase (which checks active attachments)
        return super.hasPermission(inName);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        if (perm == null) return false;
        return hasPermission(perm.getName());
    }

    /**
     * Injects this custom permissible dynamically into the player entity using reflection.
     */
    public static void inject(Player player, FoliaPerms plugin) {
        try {
            Class<?> clazz = player.getClass();
            java.lang.reflect.Field field = null;
            while (clazz != null && field == null) {
                for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                    if (org.bukkit.permissions.Permissible.class.isAssignableFrom(f.getType())) {
                        field = f;
                        break;
                    }
                }
                clazz = clazz.getSuperclass();
            }
            
            if (field != null) {
                field.setAccessible(true);
                FoliaPermissible newPermissible = new FoliaPermissible(player, plugin);
                field.set(player, newPermissible);
                plugin.getLogger().fine("Successfully injected FoliaPermissible for " + player.getName());
            } else {
                plugin.getLogger().warning("Could not locate Permissible field on player object " + player.getClass().getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to inject custom permissible for " + player.getName() + ": " + e.getMessage());
        }
    }
}
