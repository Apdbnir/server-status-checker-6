package com.example.serverstatuschecker.cache;

import com.example.serverstatuschecker.model.Server;
import com.example.serverstatuschecker.model.ServerStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommonCache {
    private final Map<String, Object> cache = new HashMap<>();


    public <T> T getById(Long id, String key) {
        if (id == null || key == null) {
            return null;
        }
        return (T) cache.get(key);
    }

    public <T> List<T> getAll(String key) {
        if (key == null) {
            return null;
        }
        return (List<T>) cache.get(key);
    }

    public void put(Object entity, String key) {
        if (entity != null && key != null) {
            cache.put(key, entity);
        }
    }

    public void putAll(List<?> entities, String key) {
        if (entities != null && key != null) {
            cache.put(key, entities);
        }
    }

    public void clearCache(String prefix) {
        if (prefix != null) {
            cache.keySet().removeIf(key -> key.startsWith(prefix) || key.equals(prefix));
        }
    }


    public Server getServerById(Long id) {
        return getById(id, "server_" + id);
    }

    public List<Server> getAllServers() {
        return getAll("all_servers");
    }

    public void putServer(Server server) {
        if (server != null && server.getId() != null) {
            put(server, "server_" + server.getId());
        }
    }

    public void putAllServers(List<Server> servers) {
        putAll(servers, "all_servers");
    }


    public ServerStatus getServerStatusById(Long id) {
        return getById(id, "status_" + id);
    }

    public List<ServerStatus> getAllServerStatuses() {
        return getAll("all_statuses");
    }

    public List<ServerStatus> getStatusesByServerName(String serverName) {
        return getAll("statuses_by_server_" + serverName);
    }

    public void putServerStatus(ServerStatus status) {
        if (status != null && status.getId() != null) {
            put(status, "status_" + status.getId());
        }
    }

    public void putAllServerStatuses(List<ServerStatus> statuses) {
        putAll(statuses, "all_statuses");
    }

    public void putStatusesByServerName(String serverName, List<ServerStatus> statuses) {
        if (serverName != null) {
            putAll(statuses, "statuses_by_server_" + serverName);
        }
    }


    public void clearServerCache() {
        clearCache("server_");
        cache.remove("all_servers");
    }

    public void clearServerStatusCache() {
        clearCache("status_");
        clearCache("statuses_by_server_");
        cache.remove("all_statuses");
    }

    public void clearAllCache() {
        cache.clear();
    }
}