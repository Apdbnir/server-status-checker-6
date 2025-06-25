package com.example.serverstatuschecker.controller;

import com.example.serverstatuschecker.model.Server;
import com.example.serverstatuschecker.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<Server> createServer(@RequestBody Server server) {
        if (server == null || server.getName() == null || server.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Server name cannot be null or empty");
        }
        return ResponseEntity.ok(serverService.createServer(server));
    }

    @GetMapping
    public ResponseEntity<List<Server>> getAllServers() {
        return ResponseEntity.ok(serverService.getAllServers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Server> getServerById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid server ID");
        }
        return ResponseEntity.ok(serverService.getServerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Server> updateServer(@PathVariable Long id, @RequestBody Server server) {
        if (id == null || id <= 0 || server == null || server.getName() == null || server.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid ID or server name");
        }
        return ResponseEntity.ok(serverService.updateServer(id, server));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid server ID");
        }
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }
}