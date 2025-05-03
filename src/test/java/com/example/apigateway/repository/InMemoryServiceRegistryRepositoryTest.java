package com.example.apigateway.repository;

import com.example.apigateway.model.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryServiceRegistryRepositoryTest {
    private final InMemoryServiceRegistryRepository repository = new InMemoryServiceRegistryRepository();

    @Test
    void shouldRegisterAndRetrieveService() {
        // Given
        var instance = new ServiceInstance(
            "test-service",
            "instance-1",
            "http://localhost:8080",
            Instant.now()
        );

        // When
        repository.register(instance);
        var instances = repository.getAllInstances();

        // Then
        assertEquals(1, instances.size());
        assertTrue(instances.contains(instance));
    }

    @Test
    void shouldRetrieveServicesByName() {
        // Given
        var instance1 = new ServiceInstance(
            "service-1",
            "instance-1",
            "http://localhost:8081",
            Instant.now()
        );
        var instance2 = new ServiceInstance(
            "service-1",
            "instance-2",
            "http://localhost:8082",
            Instant.now()
        );
        var instance3 = new ServiceInstance(
            "service-2",
            "instance-1",
            "http://localhost:9081",
            Instant.now()
        );

        // When
        repository.register(instance1);
        repository.register(instance2);
        repository.register(instance3);

        // Then
        var service1Instances = repository.getInstancesByService("service-1");
        assertEquals(2, service1Instances.size());
        assertTrue(service1Instances.contains(instance1));
        assertTrue(service1Instances.contains(instance2));
    }

    @Test
    void shouldDeregisterService() {
        // Given
        var instance = new ServiceInstance(
            "test-service",
            "instance-1",
            "http://localhost:8080",
            Instant.now()
        );
        repository.register(instance);

        // When
        boolean removed = repository.deregister("test-service", "instance-1");

        // Then
        assertTrue(removed);
        assertTrue(repository.getAllInstances().isEmpty());
    }

    @Test
    void deregisterShouldReturnFalseWhenServiceNotFound() {
        boolean removed = repository.deregister("unknown", "unknown");
        assertFalse(removed);
    }
}