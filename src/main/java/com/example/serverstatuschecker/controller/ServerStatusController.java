package com.example.serverstatuschecker.controller;

import com.example.serverstatuschecker.dto.ServerStatusDto;
import com.example.serverstatuschecker.model.ServerStatus;
import com.example.serverstatuschecker.service.RequestCounterService;
import com.example.serverstatuschecker.service.ServerStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/server-status")
@RequiredArgsConstructor
public class ServerStatusController {

    private final ServerStatusService serverStatusService;
    private final RequestCounterService counterService;

    @GetMapping("/check")
    public ResponseEntity<ServerStatus> checkServerStatus(@RequestParam String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым или null");
        }
        ServerStatus request = new ServerStatus();
        request.setUrl(url);
        ServerStatus response = serverStatusService.checkServerStatus(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ServerStatus> createServerStatus(@RequestBody ServerStatus serverStatus) {
        if (serverStatus == null || serverStatus.getUrl() == null || serverStatus.getUrl().trim().isEmpty() ||
                serverStatus.getServer() == null || serverStatus.getServer().getId() == null) {
            throw new IllegalArgumentException("Недопустимые данные статуса сервера");
        }
        return ResponseEntity.ok(serverStatusService.createServerStatus(serverStatus));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ServerStatus>> checkServerStatuses(@RequestBody List<ServerStatusDto> requests) {
        if (requests == null || requests.isEmpty() || requests.stream().anyMatch(dto ->
                dto.getUrl() == null || dto.getUrl().trim().isEmpty())) {
            throw new IllegalArgumentException("Недопустимые или пустые данные запроса");
        }
        return ResponseEntity.ok(serverStatusService.checkServerStatuses(requests));
    }

    @GetMapping
    public ResponseEntity<List<ServerStatus>> getAllServerStatuses() {
        return ResponseEntity.ok(serverStatusService.getAllServerStatuses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerStatus> getServerStatusById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Недопустимый ID статуса");
        }
        return ResponseEntity.ok(serverStatusService.getServerStatusById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerStatus> updateServerStatus(@PathVariable Long id, @RequestBody ServerStatus serverStatus) {
        if (id == null || id <= 0 || serverStatus == null) {
            throw new IllegalArgumentException("Недопустимый ID или данные статуса");
        }
        return ResponseEntity.ok(serverStatusService.updateServerStatus(id, serverStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServerStatus(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Недопустимый ID статуса");
        }
        serverStatusService.deleteServerStatus(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statuses-by-server")
    public ResponseEntity<List<ServerStatus>> getStatusesByServerName(@RequestParam String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя сервера не может быть пустым или null");
        }
        return ResponseEntity.ok(serverStatusService.getStatusesByServerName(serverName));
    }

    @GetMapping("/request-count")
    public ResponseEntity<Long> getRequestCount() {
        return ResponseEntity.ok(counterService.getCount());
    }

    @PostMapping("/request-count/reset")
    public ResponseEntity<Void> resetRequestCount() {
        counterService.reset();
        return ResponseEntity.noContent().build();
    }
}