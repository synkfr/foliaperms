package kaiakk.foliaPerms.permissions;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SQL-based permissions storage supporting SQLite and MySQL backends.
 * Version: 1.14.0
 */
public class SqlPermStorage implements PermStorage {
    private final JavaPlugin plugin;
    private final String type;
    
    // Connection settings
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final String sqliteFile;

    private Connection conn;

    public SqlPermStorage(JavaPlugin plugin, String type, String host, int port, String database,
                          String username, String password, boolean useSSL, String sqliteFile) {
        this.plugin = plugin;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useSSL = useSSL;
        this.sqliteFile = sqliteFile;
    }

    private synchronized Connection getConnection() throws Exception {
        if (conn == null || conn.isClosed()) {
            if ("sqlite".equalsIgnoreCase(type)) {
                Class.forName("org.sqlite.JDBC");
                File dbFile = new File(plugin.getDataFolder(), sqliteFile);
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            } else {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    Class.forName("com.mysql.jdbc.Driver");
                }
                String connUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + 
                        "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true";
                conn = DriverManager.getConnection(connUrl, username, password);
            }
        }
        return conn;
    }

    @Override
    public void init() throws Exception {
        Connection c = getConnection();
        try (Statement st = c.createStatement()) {
            // Create user tables
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foliaperms_users (" +
                    "uuid VARCHAR(36) PRIMARY KEY" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foliaperms_user_permissions (" +
                    "uuid VARCHAR(36), " +
                    "permission VARCHAR(255), " +
                    "PRIMARY KEY(uuid, permission)" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foliaperms_user_groups (" +
                    "uuid VARCHAR(36), " +
                    "group_name VARCHAR(64), " +
                    "PRIMARY KEY(uuid, group_name)" +
                    ")");

            // Create group tables
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foliaperms_groups (" +
                    "name VARCHAR(64) PRIMARY KEY" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foliaperms_group_permissions (" +
                    "group_name VARCHAR(64), " +
                    "permission VARCHAR(255), " +
                    "PRIMARY KEY(group_name, permission)" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foliaperms_group_members (" +
                    "group_name VARCHAR(64), " +
                    "uuid VARCHAR(36), " +
                    "PRIMARY KEY(group_name, uuid)" +
                    ")");
        }
    }

    @Override
    public Map<UUID, UserData> loadUsers() throws Exception {
        Map<UUID, UserData> map = new HashMap<>();
        Connection c = getConnection();

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid FROM foliaperms_users")) {
            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    UUID id = UUID.fromString(uuidStr);
                    map.put(id, new UserData(id));
                }
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, permission FROM foliaperms_user_permissions")) {
            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    UUID id = UUID.fromString(uuidStr);
                    UserData ud = map.get(id);
                    if (ud != null) {
                        ud.addPermission(rs.getString("permission"));
                    }
                }
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, group_name FROM foliaperms_user_groups")) {
            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    UUID id = UUID.fromString(uuidStr);
                    UserData ud = map.get(id);
                    if (ud != null) {
                        ud.addGroup(rs.getString("group_name"));
                    }
                }
            }
        }

        return map;
    }

    @Override
    public Map<String, GroupData> loadGroups() throws Exception {
        Map<String, GroupData> map = new HashMap<>();
        Connection c = getConnection();

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM foliaperms_groups")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    map.put(name.toLowerCase(), new GroupData(name));
                }
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT group_name, permission FROM foliaperms_group_permissions")) {
            while (rs.next()) {
                String name = rs.getString("group_name");
                if (name != null) {
                    GroupData gd = map.get(name.toLowerCase());
                    if (gd != null) {
                        gd.addPermission(rs.getString("permission"));
                    }
                }
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT group_name, uuid FROM foliaperms_group_members")) {
            while (rs.next()) {
                String name = rs.getString("group_name");
                if (name != null) {
                    GroupData gd = map.get(name.toLowerCase());
                    if (gd != null) {
                        gd.addMember(rs.getString("uuid"));
                    }
                }
            }
        }

        return map;
    }

    @Override
    public void save(Map<UUID, UserData> users, Map<String, GroupData> groups) throws Exception {
        Connection c = getConnection();
        c.setAutoCommit(false);
        try {
            // Wipe existing records in transaction to rewrite exactly
            try (Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM foliaperms_user_permissions");
                st.executeUpdate("DELETE FROM foliaperms_user_groups");
                st.executeUpdate("DELETE FROM foliaperms_users");
                st.executeUpdate("DELETE FROM foliaperms_group_permissions");
                st.executeUpdate("DELETE FROM foliaperms_group_members");
                st.executeUpdate("DELETE FROM foliaperms_groups");
            }

            // Save groups
            try (PreparedStatement insertGroup = c.prepareStatement("INSERT INTO foliaperms_groups (name) VALUES (?)");
                 PreparedStatement insertGroupPerm = c.prepareStatement("INSERT INTO foliaperms_group_permissions (group_name, permission) VALUES (?, ?)");
                 PreparedStatement insertGroupMember = c.prepareStatement("INSERT INTO foliaperms_group_members (group_name, uuid) VALUES (?, ?)")) {
                 
                for (GroupData g : groups.values()) {
                    insertGroup.setString(1, g.getName());
                    insertGroup.addBatch();

                    for (String perm : g.getPermissions()) {
                        insertGroupPerm.setString(1, g.getName());
                        insertGroupPerm.setString(2, perm);
                        insertGroupPerm.addBatch();
                    }

                    for (String m : g.getMembers()) {
                        insertGroupMember.setString(1, g.getName());
                        insertGroupMember.setString(2, m);
                        insertGroupMember.addBatch();
                    }
                }
                insertGroup.executeBatch();
                insertGroupPerm.executeBatch();
                insertGroupMember.executeBatch();
            }

            // Save users
            try (PreparedStatement insertUser = c.prepareStatement("INSERT INTO foliaperms_users (uuid) VALUES (?)");
                 PreparedStatement insertUserPerm = c.prepareStatement("INSERT INTO foliaperms_user_permissions (uuid, permission) VALUES (?, ?)");
                 PreparedStatement insertUserGroup = c.prepareStatement("INSERT INTO foliaperms_user_groups (uuid, group_name) VALUES (?, ?)")) {
                 
                for (UserData u : users.values()) {
                    insertUser.setString(1, u.getId().toString());
                    insertUser.addBatch();

                    for (String perm : u.getPermissions()) {
                        insertUserPerm.setString(1, u.getId().toString());
                        insertUserPerm.setString(2, perm);
                        insertUserPerm.addBatch();
                    }

                    for (String g : u.getGroups()) {
                        insertUserGroup.setString(1, u.getId().toString());
                        insertUserGroup.setString(2, g);
                        insertUserGroup.addBatch();
                    }
                }
                insertUser.executeBatch();
                insertUserPerm.executeBatch();
                insertUserGroup.executeBatch();
            }

            c.commit();
        } catch (Exception ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(true);
        }
    }

    @Override
    public void close() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
