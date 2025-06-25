package com.example.serverstatuschecker.service;

import com.example.serverstatuschecker.cache.CommonCache;
import com.example.serverstatuschecker.model.Server;
import com.example.serverstatuschecker.model.ServerStatus;
import com.example.serverstatuschecker.repository.ServerStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseService {

    protected final RequestCounterService counterService;
    protected final CommonCache cache;

    @Transactional
    protected <T> T executeWithCache(Long id, String cacheKey, Supplier<T> dbSupplier, String logMessage) {
        counterService.increment();
        T cachedEntity = (T) cache.getById(id, cacheKey);
        if (cachedEntity != null) {
            log.info(logMessage, id);
            return cachedEntity;
        }
        T entity = dbSupplier.get();
        cache.put(entity, cacheKey);
        return entity;
    }

    @Transactional
    protected <T> List<T> executeWithCacheForList(String cacheKey, Supplier<List<T>> dbSupplier, String logMessage) {
        counterService.increment();
        List<T> cachedEntities = (List<T>) cache.getAll(cacheKey);
        if (cachedEntities != null) {
            log.info(logMessage);
            return cachedEntities;
        }
        List<T> entities = dbSupplier.get();
        cache.putAll(entities, cacheKey);
        return entities;
    }

    @Transactional
    protected void executeWithCacheClear(String cacheKey, Runnable dbOperation, String logMessage, Long id) {
        counterService.increment();
        if (cache.getById(id, cacheKey) != null) {
            log.info(logMessage, id);
        }
        dbOperation.run();
        cache.clearCache(cacheKey);
    }

    @Transactional
    protected void updateRelatedCache(ServerStatusRepository serverStatusRepository, Object entity, String listCacheKey) {
        if (entity instanceof Server) {
            Server server = (Server) entity;
            cache.putAllServers(((ServerRepository) serverStatusRepository.getJpaRepository()).findAll());
            if (server.getName() != null) {
                cache.putStatusesByServerName(server.getName(), serverStatusRepository.findByServerName(server.getName()));
            }
        } else if (entity instanceof ServerStatus) {
            ServerStatus status = (ServerStatus) entity;
            cache.putAllServerStatuses(serverStatusRepository.findAll());
            if (status.getServer() != null && status.getServer().getName() != null) {
                cache.putStatusesByServerName(status.getServer().getName(), serverStatusRepository.findByServerName(status.getServer().getName()));
            }
        }
        cache.putAll(cache.getAll(listCacheKey), listCacheKey);
    }
}