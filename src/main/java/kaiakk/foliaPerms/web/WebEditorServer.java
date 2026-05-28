package kaiakk.foliaPerms.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import kaiakk.foliaPerms.FoliaPerms;
import kaiakk.foliaPerms.permissions.GroupData;
import kaiakk.foliaPerms.permissions.PermissionService;
import kaiakk.foliaPerms.permissions.UserData;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedded HTTP server for serving the FoliaPerms Web Editor.
 * Uses built-in sun HttpServer for maximum compatibility and zero external dependencies.
 * Version: 1.14.0
 */
public class WebEditorServer {
    private final FoliaPerms plugin;
    private final PermissionService service;
    private final String host;
    private final int port;
    private HttpServer server;

    // Tokens expire after 3 hours (System.currentTimeMillis() + 180 * 60 * 1000)
    private final Map<String, Long> activeTokens = new ConcurrentHashMap<>();

    public WebEditorServer(FoliaPerms plugin, String host, int port) {
        this.plugin = plugin;
        this.service = plugin.getPermissionService();
        this.host = host;
        this.port = port;
        loadTokens();
    }

    public synchronized void start() {
        if (server != null) return;
        try {
            // To prevent BindException on remote servers/VPS where the public IP is not locally bindable,
            // we will bind to all interfaces (0.0.0.0) if the host is not loopback!
            boolean isLoopback = host.equalsIgnoreCase("localhost") || host.equalsIgnoreCase("127.0.0.1");
            String bindHost = isLoopback ? host : "0.0.0.0";

            server = HttpServer.create(new InetSocketAddress(bindHost, port), 0);
            server.createContext("/editor", new HtmlHandler());
            server.createContext("/api/data", new ApiDataHandler());
            server.createContext("/api/save", new ApiSaveHandler());
            server.setExecutor(null); // default executor
            server.start();
            plugin.getLogger().info("Web Editor Server started and listening on http://" + bindHost + ":" + port + "/");
            plugin.getLogger().info("URL address generated for browser: http://" + host + ":" + port + "/");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start Web Editor Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void stop() {
        if (server == null) return;
        try {
            server.stop(1); // stop instantly
            server = null;
            plugin.getLogger().info("Web Editor Server stopped.");
        } catch (Exception e) {
            plugin.getLogger().warning("Error stopping Web Editor Server: " + e.getMessage());
        }
    }

    /**
     * Generates a new session token valid for 3 hours.
     */
    public String generateToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        activeTokens.put(token, System.currentTimeMillis() + 180L * 60L * 1000L);
        saveTokens();
        return token;
    }

    private boolean isValidToken(String token) {
        if (token == null) return false;
        Long expiry = activeTokens.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            activeTokens.remove(token);
            saveTokens();
            return false;
        }
        return true;
    }

    private void loadTokens() {
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "sessions.yml");
            if (!file.exists()) return;
            org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            if (cfg.isConfigurationSection("sessions")) {
                for (String token : cfg.getConfigurationSection("sessions").getKeys(false)) {
                    long expiry = cfg.getLong("sessions." + token);
                    if (expiry > System.currentTimeMillis()) {
                        activeTokens.put(token, expiry);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load sessions: " + e.getMessage());
        }
    }

    private void saveTokens() {
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "sessions.yml");
            org.bukkit.configuration.file.YamlConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
            for (Map.Entry<String, Long> entry : activeTokens.entrySet()) {
                if (entry.getValue() > System.currentTimeMillis()) {
                    cfg.set("sessions." + entry.getKey(), entry.getValue());
                }
            }
            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save sessions: " + e.getMessage());
        }
    }

    private String getQueryParam(String query, String paramName) {
        if (query == null) return null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equalsIgnoreCase(paramName)) {
                return kv[1];
            }
        }
        return null;
    }

    private void sendResponse(HttpExchange exchange, int status, String mimeType, byte[] content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private class HtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;
            
            String token = getQueryParam(exchange.getRequestURI().getQuery(), "token");
            if (!isValidToken(token)) {
                byte[] response = "<h1>403 Forbidden</h1><p>Invalid or expired session token. Please run /fperm editor in-game to generate a new session.</p>"
                        .getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 403, "text/html; charset=utf-8", response);
                return;
            }

            try (java.io.InputStream is = getClass().getResourceAsStream("/web/editor.html")) {
                if (is == null) {
                    byte[] err = "<h1>500 Internal Error</h1><p>Web editor UI resources are missing.</p>".getBytes(StandardCharsets.UTF_8);
                    sendResponse(exchange, 500, "text/html; charset=utf-8", err);
                    return;
                }
                byte[] html = is.readAllBytes();
                sendResponse(exchange, 200, "text/html; charset=utf-8", html);
            }
        }
    }

    private class ApiDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            String token = getQueryParam(exchange.getRequestURI().getQuery(), "token");
            if (!isValidToken(token)) {
                byte[] response = "{\"error\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 403, "application/json", response);
                return;
            }

            try {
                JsonObject json = new JsonObject();
                
                // Serialize groups
                JsonObject groupsJson = new JsonObject();
                for (Map.Entry<String, GroupData> e : service.getGroups().entrySet()) {
                    JsonObject g = new JsonObject();
                    JsonArray perms = new JsonArray();
                    for (String p : e.getValue().getPermissions()) perms.add(p);
                    g.add("permissions", perms);

                    JsonArray mems = new JsonArray();
                    for (String m : e.getValue().getMembers()) mems.add(m);
                    g.add("members", mems);
                    
                    g.addProperty("weight", e.getValue().getWeight());
                    g.addProperty("prefix", e.getValue().getPrefix());

                    groupsJson.add(e.getKey(), g);
                }
                json.add("groups", groupsJson);

                // Serialize users
                JsonObject usersJson = new JsonObject();
                for (Map.Entry<UUID, UserData> e : service.getUsers().entrySet()) {
                    JsonObject u = new JsonObject();
                    JsonArray perms = new JsonArray();
                    for (String p : e.getValue().getPermissions()) perms.add(p);
                    u.add("permissions", perms);

                    JsonArray grps = new JsonArray();
                    for (String g : e.getValue().getGroups()) grps.add(g);
                    u.add("groups", grps);

                    // Resolve offline player name dynamically from local cache
                    String name = "OfflinePlayer";
                    try {
                        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(e.getKey());
                        if (op != null && op.getName() != null) {
                            name = op.getName();
                        }
                    } catch (Throwable ignored) {}
                    u.addProperty("name", name);

                    usersJson.add(e.getKey().toString(), u);
                }
                json.add("users", usersJson);

                // Serialize registered permissions
                JsonArray regArray = new JsonArray();
                for (String p : service.getRegisteredPermissionsSorted()) {
                    regArray.add(p);
                }
                json.add("registeredPermissions", regArray);

                byte[] content = json.toString().getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 200, "application/json", content);
            } catch (Throwable e) {
                plugin.getLogger().severe("Failed to serialize permissions payload: " + e.getMessage());
                e.printStackTrace();
                byte[] response = ("{\"error\":\"Internal Server Error\",\"message\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 500, "application/json", response);
            }
        }
    }

    private class ApiSaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handlePreflight(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                byte[] response = "{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 450, "application/json", response);
                return;
            }

            String token = getQueryParam(exchange.getRequestURI().getQuery(), "token");
            if (!isValidToken(token)) {
                byte[] response = "{\"error\":\"Forbidden\"}".getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 403, "application/json", response);
                return;
            }

            try {
                byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
                String body = new String(bodyBytes, StandardCharsets.UTF_8);

                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                Map<String, GroupData> newGroups = new ConcurrentHashMap<>();
                if (json.has("groups")) {
                    JsonObject grps = json.getAsJsonObject("groups");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : grps.entrySet()) {
                        String groupName = entry.getKey().toLowerCase();
                        GroupData gd = new GroupData(groupName);
                        JsonObject gObj = entry.getValue().getAsJsonObject();
                        if (gObj.has("permissions")) {
                            for (com.google.gson.JsonElement p : gObj.getAsJsonArray("permissions")) {
                                gd.addPermission(p.getAsString());
                            }
                        }
                        if (gObj.has("members")) {
                            for (com.google.gson.JsonElement m : gObj.getAsJsonArray("members")) {
                                gd.addMember(m.getAsString());
                            }
                        }
                        if (gObj.has("weight")) {
                            gd.setWeight(gObj.get("weight").getAsInt());
                        }
                        if (gObj.has("prefix")) {
                            gd.setPrefix(gObj.get("prefix").getAsString());
                        }
                        newGroups.put(groupName, gd);
                    }
                }

                Map<UUID, UserData> newUsers = new ConcurrentHashMap<>();
                if (json.has("users")) {
                    JsonObject usrs = json.getAsJsonObject("users");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : usrs.entrySet()) {
                        UUID id = UUID.fromString(entry.getKey());
                        UserData ud = new UserData(id);
                        JsonObject uObj = entry.getValue().getAsJsonObject();
                        if (uObj.has("permissions")) {
                            for (com.google.gson.JsonElement p : uObj.getAsJsonArray("permissions")) {
                                ud.addPermission(p.getAsString());
                            }
                        }
                        if (uObj.has("groups")) {
                            for (com.google.gson.JsonElement g : uObj.getAsJsonArray("groups")) {
                                ud.addGroup(g.getAsString());
                            }
                        }
                        newUsers.put(id, ud);
                    }
                }

                // Swap maps in memory safely
                service.getGroups().clear();
                service.getGroups().putAll(newGroups);
                service.getUsers().clear();
                service.getUsers().putAll(newUsers);

                // Save to active storage
                service.saveAsync();

                // Refresh all player attachments (must execute on main thread in Folia/Paper)
                org.bukkit.Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
                    plugin.refreshAllAttachments();
                    plugin.getLogger().info("Successfully re-applied permission modifications from Web Editor.");
                });

                byte[] success = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 200, "application/json", success);
            } catch (Throwable e) {
                plugin.getLogger().severe("Failed to parse and save Web Editor payloads: " + e.getMessage());
                e.printStackTrace();
                byte[] error = ("{\"error\":\"Internal Server Error\",\"message\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 500, "application/json", error);
            }
        }
    }
}
