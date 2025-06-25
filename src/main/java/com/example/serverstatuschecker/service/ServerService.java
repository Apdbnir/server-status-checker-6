package com.example.serverstatuschecker.service;

import com.example.serverstatuschecker.cache.CommonCache;
import com.example.serverstatuschecker.model.Server;
import com.example.serverstatuschecker.repository.ServerRepository;
import com.example.serverstatuschecker.repository.ServerStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServerService extends BaseService {

    private final ServerRepository serverRepository;
    private final ServerStatusRepository serverStatusRepository;

    public ServerService(ServerRepository serverRepository, ServerStatusRepository serverStatusRepository,
                         CommonCache cache, RequestCounterService counterService) {
        super(counterService, cache);
        this.serverRepository = serverRepository;
        this.serverStatusRepository = serverStatusRepository;
    }

    @Transactional
    public Server createServer(Server server) {
        return executeWithCache(server.getId(), "server_" + server.getId(), () -> {
            Server savedServer = serverRepository.save(server);
            updateRelatedCache(serverStatusRepository, savedServer, "all_servers");
            return savedServer;
        }, "Кэш найден для сервера с id: {}");
    }

    @Transactional
    public List<Server> getAllServers() {
        return executeWithCacheForList("all_servers", serverRepository::findAll, "Кэш найден для всех серверов");
    }

    @Transactional
    public Server getServerById(Long id) {
        return executeWithCache(id, "server_" + id, () ->
                        serverRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Сервер не найден с id: " + id)),
                "Кэш найден для сервера с id: {}");
    }

    @Transactional
    public Server updateServer(Long id, Server updatedServer) {
        return executeWithCache(id, "server_" + id, () -> {
            Server server = serverRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Сервер не найден с id: " + id));
            server.setName(updatedServer.getName());
            Server savedServer = serverRepository.save(server);
            updateRelatedCache(serverStatusRepository, savedServer, "all_servers");
            return savedServer;
        }, "Кэш найден для сервера с id: {}");
    }

    @Transactional
    public void deleteServer(Long id) {
        executeWithCacheClear("server_" + id, () -> serverRepository.deleteById(id),
                "Кэш найден для сервера с id: {}", id);
    }
}