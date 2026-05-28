package kaiakk.foliaPerms.permissions;

import java.util.Map;
import java.util.UUID;

/**
 * Storage interface for managing user and group permission profiles.
 * Version: 1.14.0
 */
public interface PermStorage {
    
    /**
     * Initializes connections, directories, or tables required for storage.
     */
    void init() throws Exception;
    
    /**
     * Loads all user permission profiles from the storage system.
     */
    Map<UUID, UserData> loadUsers() throws Exception;
    
    /**
     * Loads all group permission profiles from the storage system.
     */
    Map<String, GroupData> loadGroups() throws Exception;
    
    /**
     * Saves user and group configurations back to the storage system.
     */
    void save(Map<UUID, UserData> users, Map<String, GroupData> groups) throws Exception;
    
    /**
     * Closes connections or file resources gracefully.
     */
    void close() throws Exception;
}
