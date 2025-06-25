package com.example.serverstatuschecker.service;

import com.example.serverstatuschecker.cache.CommonCache;
import com.example.serverstatuschecker.dto.ServerStatusDto;
import com.example.serverstatuschecker.model.Server;
import com.example.serverstatuschecker.model.ServerStatus;
import com.example.serverstatuschecker.repository.ServerRepository;
import com.example.serverstatuschecker.repository.ServerStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServerStatusService extends BaseService {

    private final ServerRepository serverRepository;
    private final ServerStatusRepository serverStatusRepository;

    public ServerStatusService(ServerRepository serverRepository, ServerStatusRepository serverStatusRepository,
                               CommonCache cache, RequestCounterService counterService) {
        super(counterService, cache);
        this.serverRepository = serverRepository;
        this.serverStatusRepository = serverStatusRepository;
    }

    @Transactional
    public ServerStatus checkServerStatus(ServerStatus request) {
        String cacheKey = "status_" + request.getUrl().hashCode();
        return executeWithCache((long) cacheKey.hashCode(), cacheKey, () -> {
            ServerStatus response = new ServerStatus();
            response.setUrl(request.getUrl());
            try {
                URL serverUrl = new URL(request.getUrl());
                HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();
                response.setIsAvailable(responseCode >= 200 && responseCode < 300);
                response.setMessage(response.isAvailable() ? "Сервер доступен" :
                        "Сервер ответил с кодом: " + responseCode);
                connection.disconnect();
            } catch (Exception e) {
                log.error("Ошибка проверки статуса сервера для URL: {}", request.getUrl(), e);
                response.setIsAvailable(false);
                response.setMessage("Не удалось подключиться: " + e.getMessage());
            }
            Server server = serverRepository.findById(1L).orElseGet(() -> {
                Server newServer = new Server();
                newServer.setName("Default Server");
                return serverRepository.save(newServer);
            });
            response.setServer(server);
            ServerStatus savedResponse = serverStatusRepository.save(response);
            updateRelatedCache(serverStatusRepository, savedResponse, "all_statuses");
            return savedResponse;
        }, "Кэш найден для ключа: {}");
    }

    @Transactional
    public List<ServerStatus> checkServerStatuses(List<ServerStatusDto> requests) {
        counterService.increment();
        Server defaultServer = serverRepository.findById(1L).orElseGet(() -> {
            Server newServer = new Server();
            newServer.setName("Default Server");
            return serverRepository.save(newServer);
        });

        List<ServerStatus> responses = requests.stream()
                .map(dto -> {
                    String cacheKey = "status_" + dto.getUrl().hashCode();
                    ServerStatus cachedStatus = cache.getServerStatusById((long) cacheKey.hashCode());
                    if (cachedStatus != null) {
                        log.info("Кэш найден для ключа: {}", cacheKey);
                        return cachedStatus;
                    }
                    ServerStatus response = new ServerStatus();
                    response.setUrl(dto.getUrl());
                    try {
                        URL serverUrl = new URL(dto.getUrl());
                        HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);
                        int responseCode = connection.getResponseCode();
                        response.setIsAvailable(responseCode >= 200 && responseCode < 300);
                        response.setMessage(response.isAvailable() ? "Сервер доступен" :
                                "Сервер ответил с кодом: " + responseCode);
                        connection.disconnect();
                    } catch (Exception e) {
                        log.error("Ошибка проверки статуса сервера для URL: {}", dto.getUrl(), e);
                        response.setIsAvailable(false);
                        response.setMessage("Не удалось подключиться: " + e.getMessage());
                    }
                    response.setServer(defaultServer);
                    return response;
                })
                .collect(Collectors.toList());

        List<ServerStatus> savedResponses = serverStatusRepository.saveAll(responses);
        savedResponses.forEach(status -> cache.putServerStatus(status));
        updateRelatedCache(serverStatusRepository, savedResponses.get(0), "all_statuses");
        return savedResponses;
    }

    @Transactional
    public ServerStatus createServerStatus(ServerStatus serverStatus) {
        return executeWithCache(serverStatus.getId(), "status_" + serverStatus.getId(), () -> {
            ServerStatus savedStatus = serverStatusRepository.save(serverStatus);
            updateRelatedCache(serverStatusRepository, savedStatus, "all_statuses");
            return savedStatus;
        }, "Кэш найден для статуса id: {}");
    }

    @Transactional
    public List<ServerStatus> getAllServerStatuses() {
        return executeWithCacheForList("all_statuses", serverStatusRepository::findAll, "Кэш найден для всех статусов");
    }

    @Transactional
    public ServerStatus getServerStatusById(Long id) {
        return executeWithCache(id, "status_" + id, () ->
                        serverStatusRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Серверный статус не найден с id: " + id)),
                "Кэш найден для статуса id: {}");
    }

    @Transactional
    public ServerStatus updateServerStatus(Long id, ServerStatus updatedStatus) {
        return executeWithCache(id, "status_" + id, () -> {
            ServerStatus status = serverStatusRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Серверный статус не найден с id: " + id));
            status.setUrl(updatedStatus.getUrl());
            status.setIsAvailable(updatedStatus.isAvailable());
            status.setMessage(updatedStatus.getMessage());
            ServerStatus savedStatus = serverStatusRepository.save(status);
            updateRelatedCache(serverStatusRepository, savedStatus, "all_statuses");
            return savedStatus;
        }, "Кэш найден для статуса id: {}");
    }

    @Transactional
    public void deleteServerStatus(Long id) {
        executeWithCacheClear("status_" + id, () -> serverStatusRepository.deleteById(id),
                "Кэш найден для статуса id: {}", id);
    }

    @Transactional
    public List<ServerStatus> getStatusesByServerName(String serverName) {
        return executeWithCacheForList("statuses_" + serverName, () ->
                serverStatusRepository.findByServerName(serverName), "Кэш найден для имени сервера: {}");
    }
}