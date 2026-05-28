package kaiakk.foliaPerms.commands;

import kaiakk.foliaPerms.FoliaPerms;
import kaiakk.foliaPerms.permissions.PermissionService;
import kaiakk.foliaPerms.internal.ColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FpermCommand implements CommandExecutor {
    private final FoliaPerms plugin;
    private final PermissionService service;

    public FpermCommand(FoliaPerms plugin) {
        this.plugin = plugin;
        this.service = plugin.getPermissionService();
    }

    private void send(CommandSender sender, String text) {
        if (text == null) return;
        if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
            sender.sendMessage(ColorConverter.stripColor(text));
        } else {
            sender.sendMessage(text);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("folia.perms")) {
            send(sender, ColorConverter.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            send(sender, ColorConverter.colorize("&eFoliaPerms: simple permission manager. /fperm help"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (service == null) {
            plugin.getLogger().severe("PermissionService is not initialized; command disabled.");
            send(sender, ColorConverter.colorize("&cInternal error: permission service unavailable."));
            return true;
        }

        try {
            switch (sub) {
                case "help":
                    send(sender, ColorConverter.colorize("&6&l=== FoliaPerms Commands ==="));
                    send(sender, ColorConverter.colorize("&e/fperm help &7- Show this help menu"));
                    send(sender, ColorConverter.colorize("&e/fperm editor &7- Open Web permission editor"));
                    send(sender, ColorConverter.colorize("&e/fperm reload &7- Reload all permissions from configuration"));
                    send(sender, ColorConverter.colorize("&e/fperm gather &7- Gather registered permissions from plugins"));
                    send(sender, ColorConverter.colorize("&e/fperm refresh &7- Refresh all players' attachments"));
                    send(sender, ColorConverter.colorize("&e/fperm check <player> <perm> &7- Check if player has permission"));
                    send(sender, ColorConverter.colorize("&e/fperm listperms <player> &7- List player's allowed permissions"));
                    send(sender, ColorConverter.colorize("&e/fperm user addperm|removeperm <player> <perm> &7- Modify player's permissions"));
                    send(sender, ColorConverter.colorize("&e/fperm user addgroup|removegroup <player> <group> &7- Modify player's groups"));
                    send(sender, ColorConverter.colorize("&e/fperm group create <name> &7- Create a new group"));
                    send(sender, ColorConverter.colorize("&e/fperm group addperm|removeperm <name> <perm> &7- Modify group's permissions"));
                    send(sender, ColorConverter.colorize("&e/fperm group adduser|removeuser <name> <player> &7- Modify group members"));
                    break;
                case "editor":
                    if (!(sender instanceof org.bukkit.entity.Player)) {
                        send(sender, ColorConverter.colorize("&cThe editor can only be opened by a player in-game."));
                        break;
                    }
                    var webServer = plugin.getWebEditorServer();
                    if (webServer == null) {
                        send(sender, ColorConverter.colorize("&cWeb Editor is disabled or not running. Please check config.yml."));
                        break;
                    }
                    
                    String token = webServer.generateToken();
                    var config = plugin.getConfig();
                    String host = config.getString("web-editor.host", "localhost");
                    int port = config.getInt("web-editor.port", 8080);
                    
                    boolean isDefaultHost = host.equalsIgnoreCase("localhost") 
                            || host.equalsIgnoreCase("127.0.0.1") 
                            || host.equalsIgnoreCase("0.0.0.0") 
                            || host.isEmpty();
                    
                    String displayHost = isDefaultHost ? "localhost" : host;
                    String link = "http://" + displayHost + ":" + port + "/editor?token=" + token;
                    
                    org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                    
                    // Create an interactive, clickable and hoverable TextComponent
                    net.md_5.bungee.api.chat.TextComponent clickText = new net.md_5.bungee.api.chat.TextComponent(ColorConverter.colorize("&b&n" + link));
                    clickText.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, link));
                    clickText.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                            new net.md_5.bungee.api.chat.hover.content.Text(ColorConverter.colorize("&eClick to open in browser!"))));
                    
                    player.sendMessage(ColorConverter.colorize("&6&l=== FoliaPerms Web Editor ==="));
                    player.sendMessage(ColorConverter.colorize("&eA secure session has been generated for you!"));
                    player.sendMessage(ColorConverter.colorize("&eClick the link below to open the Web Editor in your browser:"));
                    player.spigot().sendMessage(clickText);
                    
                    if (isDefaultHost) {
                        // Add secondary machine IP if connecting remotely/internal LAN
                        String machineIp = "";
                        try {
                            machineIp = java.net.InetAddress.getLocalHost().getHostAddress();
                        } catch (Exception ignored) {}
                        
                        if (machineIp != null && !machineIp.isEmpty() && !machineIp.equals("127.0.0.1") && !machineIp.equals(displayHost)) {
                            String backupLink = "http://" + machineIp + ":" + port + "/editor?token=" + token;
                            net.md_5.bungee.api.chat.TextComponent backupText = new net.md_5.bungee.api.chat.TextComponent(ColorConverter.colorize("&3&n" + backupLink));
                            backupText.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, backupLink));
                            backupText.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                                    new net.md_5.bungee.api.chat.hover.content.Text(ColorConverter.colorize("&eClick here for LAN/Machine IP!"))));
                            
                            player.sendMessage(ColorConverter.colorize("&7Or if connecting via LAN/internal network:"));
                            player.spigot().sendMessage(backupText);
                        }
                        
                        player.sendMessage(ColorConverter.colorize("&7&oNote: This session link is private and expires in 10 minutes."));
                        player.sendMessage(ColorConverter.colorize("&7&oIf hosting on a remote VPS, replace 'localhost' with your server's public IP."));
                    } else {
                        player.sendMessage(ColorConverter.colorize("&7&oNote: This session link is private and expires in 10 minutes."));
                    }
                    break;
                case "gather":
                    try {
                        service.gatherRegisteredPermissions(plugin);
                        plugin.refreshAllAttachments();
                        int count = service.getRegisteredPermissions().size();
                        send(sender, ColorConverter.colorize("&aGathered " + count + " permissions from plugins."));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to run /fperm gather: " + e.getMessage());
                        send(sender, ColorConverter.colorize("&cFailed to gather permissions: " + e.getMessage()));
                    }
                    break;
                case "reload":
                    service.load();
                    send(sender, ColorConverter.colorize("&aPermissions reloaded."));
                    break;
                case "refresh":
                    try {
                        plugin.refreshAllAttachments();
                        send(sender, ColorConverter.colorize("&aRefreshed permission attachments."));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to refresh attachments: " + e.getMessage());
                        send(sender, ColorConverter.colorize("&cFailed to refresh attachments: " + e.getMessage()));
                    }
                    break;
                case "user":
                    if (args.length < 4) {
                        send(sender, ColorConverter.colorize("&eUsage: /fperm user addperm|removeperm|addgroup|removegroup <player> <perm|group>"));
                        break;
                    }
                    String action = args[1].toLowerCase();
                    String playerName = args[2];
                    String perm = args[3];
                    try {
                        var op = Bukkit.getOfflinePlayer(playerName);
                        if (op == null) {
                            send(sender, ColorConverter.colorize("&cCould not resolve player: " + playerName));
                            break;
                        }
                        var id = op.getUniqueId();
                        if (id == null) {
                            var online = Bukkit.getPlayerExact(playerName);
                            if (online != null) id = online.getUniqueId();
                        }
                        if (id == null) {
                            send(sender, ColorConverter.colorize("&cCould not determine UUID for player: " + playerName));
                            break;
                        }

                        if (action.equals("addperm")) {
                            service.addUserPermission(id, perm);
                            plugin.getPermissionService().saveAsync();
                            var onlineTarget = Bukkit.getPlayerExact(playerName);
                            if (onlineTarget != null) {
                                plugin.refreshPlayerAttachment(onlineTarget);
                                try {
                                    onlineTarget.recalculatePermissions();
                                } catch (Throwable ignored) {}
                                try {
                                    onlineTarget.updateCommands();
                                } catch (Throwable ignored) {}
                            }
                            send(sender, ColorConverter.colorize("&aAdded permission " + perm + " to " + playerName));
                        } else if (action.equals("removeperm")) {
                            service.removeUserPermission(id, perm);
                            plugin.getPermissionService().saveAsync();
                            var onlineTarget2 = Bukkit.getPlayerExact(playerName);
                            if (onlineTarget2 != null) plugin.refreshPlayerAttachment(onlineTarget2);
                            send(sender, ColorConverter.colorize("&aRemoved permission " + perm + " from " + playerName));
                        } else if (action.equals("addgroup")) {
                            service.addUserToGroup(id, perm);
                            plugin.getPermissionService().saveAsync();
                            var onlineTarget3 = Bukkit.getPlayerExact(playerName);
                            if (onlineTarget3 != null) plugin.refreshPlayerAttachment(onlineTarget3);
                            send(sender, ColorConverter.colorize("&aAdded " + playerName + " to group " + perm));
                        } else if (action.equals("removegroup")) {
                            service.removeUserFromGroup(id, perm);
                            plugin.getPermissionService().saveAsync();
                            var onlineTarget4 = Bukkit.getPlayerExact(playerName);
                            if (onlineTarget4 != null) plugin.refreshPlayerAttachment(onlineTarget4);
                            send(sender, ColorConverter.colorize("&aRemoved " + playerName + " from group " + perm));
                        } else {
                            send(sender, ColorConverter.colorize("&cUnknown user action: " + action));
                        }
                    } catch (Exception e) {
                        kaiakk.foliaPerms.internal.ErrorHandler.handle(plugin, "Exception handling /fperm user", e);
                        send(sender, ColorConverter.colorize("&cInternal error while processing user command."));
                    }
                    break;
                case "group":
                    if (args.length < 2) {
                        send(sender, ColorConverter.colorize("&eUsage: /fperm group create|addperm|removeperm|adduser|removeuser <args>"));
                        break;
                    }
                    try {
                        String gaction = args[1].toLowerCase();
                        if (gaction.equals("create")) {
                            if (args.length < 3) { send(sender, ColorConverter.colorize("Usage: /fperm group create <name>")); break; }
                            service.createGroup(args[2]);
                            plugin.getPermissionService().saveAsync();
                            plugin.refreshAllAttachments();
                            send(sender, ColorConverter.colorize("&aGroup created: " + args[2]));
                        } else if (gaction.equals("addperm")) {
                            if (args.length < 4) { send(sender, ColorConverter.colorize("&eUsage: /fperm group addperm <name> <perm>")); break; }
                            service.addGroupPermission(args[2], args[3]);
                            plugin.getPermissionService().saveAsync();
                            plugin.refreshAllAttachments();
                            send(sender, ColorConverter.colorize("&aAdded permission " + args[3] + " to group " + args[2]));
                        } else if (gaction.equals("removeperm")) {
                            if (args.length < 4) { send(sender, ColorConverter.colorize("&eUsage: /fperm group removeperm <name> <perm>")); break; }
                            service.removeGroupPermission(args[2], args[3]);
                            plugin.getPermissionService().saveAsync();
                            plugin.refreshAllAttachments();
                            send(sender, ColorConverter.colorize("&aRemoved permission " + args[3] + " from group " + args[2]));
                        } else if (gaction.equals("adduser")) {
                            if (args.length < 4) { send(sender, ColorConverter.colorize("&eUsage: /fperm group adduser <name> <player>")); break; }
                            String gname = args[2];
                            var target = Bukkit.getOfflinePlayer(args[3]);
                            if (target == null || target.getUniqueId() == null) {
                                send(sender, ColorConverter.colorize("&cCould not resolve player: " + args[3]));
                                break;
                            }
                            service.addUserToGroup(target.getUniqueId(), gname);
                            plugin.getPermissionService().saveAsync();
                            var ot = Bukkit.getPlayerExact(args[3]);
                            if (ot != null) plugin.refreshPlayerAttachment(ot);
                            send(sender, ColorConverter.colorize("&aAdded " + args[3] + " to group " + gname));
                        } else if (gaction.equals("removeuser")) {
                            if (args.length < 4) { send(sender, ColorConverter.colorize("&eUsage: /fperm group removeuser <name> <player>")); break; }
                            String gname2 = args[2];
                            var target2 = Bukkit.getOfflinePlayer(args[3]);
                            if (target2 == null || target2.getUniqueId() == null) {
                                send(sender, ColorConverter.colorize("&cCould not resolve player: " + args[3]));
                                break;
                            }
                            service.removeUserFromGroup(target2.getUniqueId(), gname2);
                            plugin.getPermissionService().saveAsync();
                            var ot2 = Bukkit.getPlayerExact(args[3]);
                            if (ot2 != null) plugin.refreshPlayerAttachment(ot2);
                            send(sender, ColorConverter.colorize("&aRemoved " + args[3] + " from group " + gname2));
                        } else {
                            send(sender, ColorConverter.colorize("&cUnknown group action: " + gaction));
                        }
                    } catch (Exception e) {
                        kaiakk.foliaPerms.internal.ErrorHandler.handle(plugin, "Exception handling /fperm group", e);
                        send(sender, ColorConverter.colorize("&cInternal error while processing group command."));
                    }
                    break;
                case "check":
                    if (args.length < 3) { send(sender, ColorConverter.colorize("&eUsage: /fperm check <player> <perm>")); break; }
                    OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                    boolean ok = service.hasPermission(t.getUniqueId(), args[2]);
                    send(sender, ColorConverter.colorize(args[1] + (ok ? " &aHAS " : " &cDOES NOT HAVE ") + args[2]));
                    break;
                case "listperms":
                    if (args.length < 2) { send(sender, ColorConverter.colorize("&eUsage: /fperm listperms <player>")); break; }
                    try {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                        if (target == null || target.getUniqueId() == null) {
                            send(sender, ColorConverter.colorize("&cCould not resolve player: " + args[1]));
                            break;
                        }
                        var perms = service.getAllowedPermissions(target.getUniqueId());
                        if (perms.isEmpty()) {
                            send(sender, ColorConverter.colorize("&e" + args[1] + " has no registered permissions (or none gathered)."));
                        } else {
                            send(sender, ColorConverter.colorize("&ePermissions for " + args[1] + ":"));
                            for (String p : perms) send(sender, ColorConverter.colorize(" - " + p));
                        }
                    } catch (Exception e) {
                        kaiakk.foliaPerms.internal.ErrorHandler.handle(plugin, "Exception during listperms", e);
                        send(sender, ColorConverter.colorize("&cInternal error while listing permissions."));
                    }
                    break;
                default:
                    send(sender, ColorConverter.colorize("&eUnknown subcommand. Use /fperm help"));
            }
        } catch (Exception ex) {
            kaiakk.foliaPerms.internal.ErrorHandler.handle(plugin, "Unhandled exception while executing /fperm", ex);
            send(sender, ColorConverter.colorize("&cInternal error while executing command."));
        }

        return true;
    }
}