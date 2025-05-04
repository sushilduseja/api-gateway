package com.example.apigateway.repository;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.model.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InMemoryServiceRegistryRepositoryTest {
    
    private InMemoryServiceRegistryRepository repository;
    private static final Instant FIXED_TIME = Instant.parse("2025-05-03T10:15:30Z");
    
    @Mock
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        repository = new InMemoryServiceRegistryRepository();
    }

    @Test
    void shouldRegisterAndRetrieveService() {
        // Given
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.UP);

        // When
        repository.register(instance);
        var instances = repository.getAllInstances();

        // Then
        assertEquals(1, instances.size());
        var registeredInstance = instances.iterator().next();
        assertEquals("test-service", registeredInstance.serviceName());
        assertEquals("instance-1", registeredInstance.instanceId());
        assertEquals("http://localhost:8080", registeredInstance.baseUrl());
        assertEquals(ServiceStatus.UP, registeredInstance.status());
    }

    @Test
    void shouldUpdateInstanceStatus() {
        // Given
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.UP);
        repository.register(instance);

        // When
        boolean updated = repository.updateStatus("test-service", "instance-1", ServiceStatus.DOWN);

        // Then
        assertTrue(updated);
        var updatedInstance = repository.getInstance("test-service", "instance-1");
        assertNotNull(updatedInstance);
        assertEquals(ServiceStatus.DOWN, updatedInstance.status());
    }

    @Test
    void shouldReturnOnlyUpInstances() {
        // Given
        repository.register(createTestInstance("service-1", "instance-1", ServiceStatus.UP));
        repository.register(createTestInstance("service-1", "instance-2", ServiceStatus.DOWN));
        repository.register(createTestInstance("service-1", "instance-3", ServiceStatus.UP));

        // When
        List<ServiceInstance> upInstances = repository.getUpInstancesByService("service-1");

        // Then
        assertEquals(2, upInstances.size());
        assertTrue(upInstances.stream().allMatch(instance -> instance.status() == ServiceStatus.UP));
    }

    @Test
    void shouldDeregisterInstance() {
        // Given
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.UP);
        repository.register(instance);

        // When
        boolean removed = repository.deregister("test-service", "instance-1");

        // Then
        assertTrue(removed);
        assertTrue(repository.getAllInstances().isEmpty());
    }

    @Test
    void shouldNotUpdateStatusOfNonexistentInstance() {
        // When
        boolean updated = repository.updateStatus("unknown", "unknown", ServiceStatus.DOWN);

        // Then
        assertFalse(updated);
    }

    @Test
    void shouldReturnEmptyListForUnknownService() {
        // When
        var instances = repository.getInstancesByService("unknown-service");

        // Then
        assertTrue(instances.isEmpty());
    }

    private ServiceInstance createTestInstance(String serviceName, String instanceId, ServiceStatus status) {
        return new ServiceInstance(
            serviceName,
            instanceId,
            "http://localhost:8080",
            status,
            FIXED_TIME
        );
    }
}