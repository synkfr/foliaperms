package kaiakk.foliaPerms.permissions;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GroupData {
    private final String name;
    private final Set<String> permissions = ConcurrentHashMap.newKeySet();
    private final Set<String> members = ConcurrentHashMap.newKeySet();
    private int weight = 0;
    private String prefix = "";

    public GroupData(String name) {
        this.name = name.toLowerCase();
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void addPermission(String node) {
        permissions.add(node.toLowerCase());
    }

    public void removePermission(String node) {
        permissions.remove(node.toLowerCase());
    }

    public void addMember(String uuid) {
        members.add(uuid);
    }

    public void removeMember(String uuid) {
        members.remove(uuid);
    }
}