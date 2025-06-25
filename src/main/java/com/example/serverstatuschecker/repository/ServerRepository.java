package com.example.serverstatuschecker.repository;

import com.example.serverstatuschecker.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<Server, Long> {
}