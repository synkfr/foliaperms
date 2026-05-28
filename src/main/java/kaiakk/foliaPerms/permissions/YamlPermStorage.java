package kaiakk.foliaPerms.permissions;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;
import java.util.UUID;

/**
 * YamlPermStorage wraps the existing YamlStorage implementation.
 * Version: 1.14.0
 */
public class YamlPermStorage implements PermStorage {
    private final YamlStorage yamlStorage;

    public YamlPermStorage(JavaPlugin plugin) {
        this.yamlStorage = new YamlStorage(plugin);
    }

    @Override
    public void init() throws Exception {
        // Handled internally in YamlStorage constructors and configuration sections
    }

    @Override
    public Map<UUID, UserData> loadUsers() throws Exception {
        return yamlStorage.loadUsers();
    }

    @Override
    public Map<String, GroupData> loadGroups() throws Exception {
        return yamlStorage.loadGroups();
    }

    @Override
    public void save(Map<UUID, UserData> users, Map<String, GroupData> groups) throws Exception {
        yamlStorage.save(users, groups);
    }

    @Override
    public void close() throws Exception {
        // Graceful close: no external connections required for yaml storage
    }
}
