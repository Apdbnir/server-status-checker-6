package com.example.serverstatuschecker.repository;

import com.example.serverstatuschecker.model.ServerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServerStatusRepository extends JpaRepository<ServerStatus, Long> {
    @Query("SELECT s FROM ServerStatus s JOIN s.server srv WHERE srv.name = :serverName")
    List<ServerStatus> findByServerName(@Param("serverName") String serverName);
}