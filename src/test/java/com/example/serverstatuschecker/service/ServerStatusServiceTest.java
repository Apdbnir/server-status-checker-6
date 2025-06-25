package com.example.serverstatuschecker.service;

import com.example.serverstatuschecker.cache.CommonCache;
import com.example.serverstatuschecker.dto.ServerStatusDto;
import com.example.serverstatuschecker.model.Server;
import com.example.serverstatuschecker.model.ServerStatus;
import com.example.serverstatuschecker.repository.ServerRepository;
import com.example.serverstatuschecker.repository.ServerStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerStatusServiceTest {

    private static final Long SERVER_ID = 1L;
    private static final String DEFAULT_SERVER_NAME = "Default Server";
    private static final String URL_1 = "http://example.com";
    private static final String URL_2 = "http://test.com";
    private static final String MESSAGE_AVAILABLE = "Сервер доступен";
    private static final String MESSAGE_UNAVAILABLE = "Сервер ответил с кодом: 500";
    private static final Long STATUS_ID_1 = 1L;
    private static final Long STATUS_ID_2 = 2L;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ServerStatusRepository serverStatusRepository;

    @Mock
    private CommonCache cache;

    @Mock
    private Server defaultServer;

    @Mock
    private ServerStatusDto dto1;

    @Mock
    private ServerStatusDto dto2;

    @Mock
    private ServerStatus status1;

    @Mock
    private ServerStatus status2;

    @InjectMocks
    private ServerStatusService serverStatusService;

    @BeforeEach
    void setUp() {
        when(defaultServer.getId()).thenReturn(SERVER_ID);
        when(defaultServer.getName()).thenReturn(DEFAULT_SERVER_NAME);

        when(dto1.getUrl()).thenReturn(URL_1);
        when(dto1.isAvailable()).thenReturn(true);
        when(dto1.getMessage()).thenReturn(MESSAGE_AVAILABLE);

        when(dto2.getUrl()).thenReturn(URL_2);
        when(dto2.isAvailable()).thenReturn(false);
        when(dto2.getMessage()).thenReturn(MESSAGE_UNAVAILABLE);

        when(status1.getId()).thenReturn(STATUS_ID_1);
        when(status1.getUrl()).thenReturn(URL_1);
        when(status1.isAvailable()).thenReturn(true);
        when(status1.getMessage()).thenReturn(MESSAGE_AVAILABLE);
        when(status1.getServer()).thenReturn(defaultServer);

        when(status2.getId()).thenReturn(STATUS_ID_2);
        when(status2.getUrl()).thenReturn(URL_2);
        when(status2.isAvailable()).thenReturn(false);
        when(status2.getMessage()).thenReturn(MESSAGE_UNAVAILABLE);
        when(status2.getServer()).thenReturn(defaultServer);
    }

    @Test
    void testCheckServerStatusesSuccess() {

        List<ServerStatusDto> requests = Arrays.asList(dto1, dto2);
        when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.of(defaultServer));
        when(cache.getServerStatusById(anyLong())).thenReturn(null);
        when(serverStatusRepository.saveAll(anyList())).thenReturn(Arrays.asList(status1, status2));
        when(serverStatusRepository.findAll()).thenReturn(Arrays.asList(status1, status2));
        when(serverStatusRepository.findByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1, status2));


        List<ServerStatus> result = serverStatusService.checkServerStatuses(requests);


        assertEquals(2, result.size());
        assertEquals(status1, result.get(0));
        assertEquals(status2, result.get(1));
        verify(serverRepository, times(1)).findById(SERVER_ID);
        verify(serverStatusRepository, times(1)).saveAll(anyList());
        verify(cache, times(2)).putServerStatus(any(ServerStatus.class));
        verify(cache, times(1)).putAllServerStatuses(anyList());
        verify(cache, times(1)).putStatusesByServerName(DEFAULT_SERVER_NAME, anyList());
    }

    @Test
    void testCheckServerStatusesWithCachedStatus() {

        List<ServerStatusDto> requests = Arrays.asList(dto1);
        when(cache.getServerStatusById(anyLong())).thenReturn(status1);


        List<ServerStatus> result = serverStatusService.checkServerStatuses(requests);


        assertEquals(1, result.size());
        assertEquals(status1, result.get(0));
        verify(serverRepository, never()).findById(anyLong());
        verify(serverStatusRepository, never()).saveAll(anyList());
        verify(cache, never()).putServerStatus(any(ServerStatus.class));
    }

    @Test
    void testCheckServerStatusesNewServerCreated() {

        when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.empty());
        when(serverRepository.save(any(Server.class))).thenReturn(defaultServer);
        when(cache.getServerStatusById(anyLong())).thenReturn(null);
        when(serverStatusRepository.saveAll(anyList())).thenReturn(Arrays.asList(status1));
        when(serverStatusRepository.findAll()).thenReturn(Arrays.asList(status1));
        when(serverStatusRepository.findByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1));


        List<ServerStatus> result = serverStatusService.checkServerStatuses(Arrays.asList(dto1));


        assertEquals(1, result.size());
        assertEquals(status1, result.get(0));
        verify(serverRepository, times(1)).findById(SERVER_ID);
        verify(serverRepository, times(1)).save(any(Server.class));
        verify(serverStatusRepository, times(1)).saveAll(anyList());
        verify(cache, times(1)).putServerStatus(any(ServerStatus.class));
    }

    @Test
    void testCheckServerStatusCacheHit() {

        ServerStatus request = mock(ServerStatus.class);
        when(request.getUrl()).thenReturn(URL_1);
        when(cache.getServerStatusById(anyLong())).thenReturn(status1);

        ServerStatus result = serverStatusService.checkServerStatus(request);

        assertEquals(status1, result);
        verify(serverRepository, never()).findById(anyLong());
        verify(serverStatusRepository, never()).save(any(ServerStatus.class));
    }

    @Test
    void testCheckServerStatusNoCacheHit() {

        ServerStatus request = mock(ServerStatus.class);
        when(request.getUrl()).thenReturn(URL_1);
        when(cache.getServerStatusById(anyLong())).thenReturn(null);
        when(serverRepository.findById(SERVER_ID)).thenReturn(Optional.of(defaultServer));
        when(serverStatusRepository.save(any(ServerStatus.class))).thenReturn(status1);
        when(serverStatusRepository.findAll()).thenReturn(Arrays.asList(status1));
        when(serverStatusRepository.findByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1));


        ServerStatus result = serverStatusService.checkServerStatus(request);


        assertEquals(status1, result);
        verify(serverRepository, times(1)).findById(SERVER_ID);
        verify(serverStatusRepository, times(1)).save(any(ServerStatus.class));
        verify(cache, times(1)).putServerStatus(any(ServerStatus.class));
        verify(cache, times(1)).putAllServerStatuses(anyList());
        verify(cache, times(1)).putStatusesByServerName(DEFAULT_SERVER_NAME, anyList());
    }

    @Test
    void testCreateServerStatusSuccess() {

        ServerStatus serverStatus = mock(ServerStatus.class);
        when(serverStatus.getId()).thenReturn(STATUS_ID_1);
        when(serverStatus.getServer()).thenReturn(defaultServer);
        when(defaultServer.getName()).thenReturn(DEFAULT_SERVER_NAME);
        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(null);
        when(serverStatusRepository.save(serverStatus)).thenReturn(status1);
        when(serverStatusRepository.findAll()).thenReturn(Arrays.asList(status1));
        when(serverStatusRepository.findByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1));


        ServerStatus result = serverStatusService.createServerStatus(serverStatus);


        assertEquals(status1, result);
        verify(serverStatusRepository, times(1)).save(serverStatus);
        verify(cache, times(1)).putServerStatus(status1);
        verify(cache, times(1)).putAllServerStatuses(anyList());
        verify(cache, times(1)).putStatusesByServerName(DEFAULT_SERVER_NAME, anyList());
    }

    @Test
    void testCreateServerStatusCacheHit() {

        ServerStatus serverStatus = mock(ServerStatus.class);
        when(serverStatus.getId()).thenReturn(STATUS_ID_1);
        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(status1);


        ServerStatus result = serverStatusService.createServerStatus(serverStatus);


        assertEquals(status1, result);
        verify(serverStatusRepository, never()).save(any(ServerStatus.class));
        verify(cache, never()).putServerStatus(any(ServerStatus.class));
    }

    @Test
    void testGetAllServerStatusesCacheHit() {

        when(cache.getAllServerStatuses()).thenReturn(Arrays.asList(status1, status2));


        List<ServerStatus> result = serverStatusService.getAllServerStatuses();


        assertEquals(2, result.size());
        assertEquals(status1, result.get(0));
        assertEquals(status2, result.get(1));
        verify(serverStatusRepository, never()).findAll();
    }

    @Test
    void testGetAllServerStatusesNoCacheHit() {

        when(cache.getAllServerStatuses()).thenReturn(null);
        when(serverStatusRepository.findAll()).thenReturn(Arrays.asList(status1, status2));

        List<ServerStatus> result = serverStatusService.getAllServerStatuses();


        assertEquals(2, result.size());
        verify(serverStatusRepository, times(1)).findAll();
        verify(cache, times(1)).putAllServerStatuses(anyList());
    }

    @Test
    void testGetServerStatusByIdSuccess() {

        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(null);
        when(serverStatusRepository.findById(STATUS_ID_1)).thenReturn(Optional.of(status1));


        ServerStatus result = serverStatusService.getServerStatusById(STATUS_ID_1);


        assertEquals(status1, result);
        verify(serverStatusRepository, times(1)).findById(STATUS_ID_1);
        verify(cache, times(1)).putServerStatus(status1);
    }

    @Test
    void testGetServerStatusByIdNotFound() {

        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(null);
        when(serverStatusRepository.findById(STATUS_ID_1)).thenReturn(Optional.empty());


        assertThrows(RuntimeException.class, () -> serverStatusService.getServerStatusById(STATUS_ID_1));
        verify(serverStatusRepository, times(1)).findById(STATUS_ID_1);
    }

    @Test
    void testUpdateServerStatusSuccess() {

        ServerStatus updatedStatus = mock(ServerStatus.class);
        when(updatedStatus.getUrl()).thenReturn(URL_1);
        when(updatedStatus.isAvailable()).thenReturn(true);
        when(updatedStatus.getMessage()).thenReturn(MESSAGE_AVAILABLE);
        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(null);
        when(serverStatusRepository.findById(STATUS_ID_1)).thenReturn(Optional.of(status1));
        when(serverStatusRepository.save(status1)).thenReturn(status1);
        when(serverStatusRepository.findAll()).thenReturn(Arrays.asList(status1));
        when(serverStatusRepository.findByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1));
        when(status1.getServer()).thenReturn(defaultServer);


        ServerStatus result = serverStatusService.updateServerStatus(STATUS_ID_1, updatedStatus);


        assertEquals(status1, result);
        verify(status1, times(1)).setUrl(URL_1);
        verify(status1, times(1)).setIsAvailable(true);
        verify(status1, times(1)).setMessage(MESSAGE_AVAILABLE);
        verify(serverStatusRepository, times(1)).save(status1);
        verify(cache, times(1)).putServerStatus(status1);
        verify(cache, times(1)).putAllServerStatuses(anyList());
        verify(cache, times(1)).putStatusesByServerName(DEFAULT_SERVER_NAME, anyList());
    }

    @Test
    void testUpdateServerStatusNotFound() {

        ServerStatus updatedStatus = mock(ServerStatus.class);
        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(null);
        when(serverStatusRepository.findById(STATUS_ID_1)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> serverStatusService.updateServerStatus(STATUS_ID_1, updatedStatus));
        verify(serverStatusRepository, times(1)).findById(STATUS_ID_1);
    }

    @Test
    void testDeleteServerStatusSuccess() {

        when(cache.getServerStatusById(STATUS_ID_1)).thenReturn(status1);
        doNothing().when(serverStatusRepository).deleteById(STATUS_ID_1);


        serverStatusService.deleteServerStatus(STATUS_ID_1);


        verify(serverStatusRepository, times(1)).deleteById(STATUS_ID_1);
        verify(cache, times(1)).clearServerStatusCache();
    }

    @Test
    void testGetStatusesByServerNameSuccess() {

        when(cache.getStatusesByServerName(DEFAULT_SERVER_NAME)).thenReturn(null);
        when(serverStatusRepository.findByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1, status2));


        List<ServerStatus> result = serverStatusService.getStatusesByServerName(DEFAULT_SERVER_NAME);


        assertEquals(2, result.size());
        assertEquals(status1, result.get(0));
        assertEquals(status2, result.get(1));
        verify(serverStatusRepository, times(1)).findByServerName(DEFAULT_SERVER_NAME);
        verify(cache, times(1)).putStatusesByServerName(DEFAULT_SERVER_NAME, anyList());
    }

    @Test
    void testGetStatusesByServerNameCacheHit() {

        when(cache.getStatusesByServerName(DEFAULT_SERVER_NAME)).thenReturn(Arrays.asList(status1, status2));


        List<ServerStatus> result = serverStatusService.getStatusesByServerName(DEFAULT_SERVER_NAME);


        assertEquals(2, result.size());
        verify(serverStatusRepository, never()).findByServerName(DEFAULT_SERVER_NAME);
    }
}