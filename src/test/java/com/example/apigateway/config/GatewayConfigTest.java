package com.example.apigateway.config;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.model.ServiceStatus;
import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    @Mock
    private InMemoryServiceRegistryRepository repository;

    @Mock
    private RouteLocatorBuilder builder;

    @Mock
    private RouteLocatorBuilder.Builder routesBuilder;

    private static final Instant FIXED_TIME = Instant.parse("2025-05-03T10:15:30Z");

    @Test
    void shouldCreateRouteForUpService() {
        // Given
        var gateway = new GatewayConfig();
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.UP);
        
        when(repository.getAllInstances()).thenReturn(List.of(instance));
        when(repository.getUpInstancesByService("test-service"))
            .thenReturn(List.of(instance));
        when(builder.routes()).thenReturn(routesBuilder);
        
        // When
        RouteLocator routeLocator = gateway.dynamicRoutes(builder, repository);
        
        // Then
        StepVerifier.create(routeLocator.getRoutes())
            .expectNextMatches(route -> 
                route.getId().equals("test-service-route") &&
                route.getUri().toString().equals("http://localhost:8080"))
            .expectComplete()
            .verify();
    }

    @Test
    void shouldCreateUnavailableRouteForDownService() {
        // Given
        var gateway = new GatewayConfig();
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.DOWN);
        
        when(repository.getAllInstances()).thenReturn(List.of(instance));
        when(repository.getUpInstancesByService("test-service"))
            .thenReturn(List.of());
        when(builder.routes()).thenReturn(routesBuilder);
        
        // When
        RouteLocator routeLocator = gateway.dynamicRoutes(builder, repository);
        
        // Then
        StepVerifier.create(routeLocator.getRoutes())
            .expectNextMatches(route -> 
                route.getId().equals("test-service-unavailable") &&
                route.getUri().toString().equals("no://op"))
            .expectComplete()
            .verify();
    }

    @Test
    void shouldBalanceRequestsAcrossMultipleInstances() {
        // Given
        var gateway = new GatewayConfig();
        var instance1 = createTestInstance("test-service", "instance-1", ServiceStatus.UP);
        var instance2 = createTestInstance("test-service", "instance-2", ServiceStatus.UP);
        
        when(repository.getAllInstances()).thenReturn(List.of(instance1, instance2));
        when(repository.getUpInstancesByService("test-service"))
            .thenReturn(List.of(instance1, instance2));
        when(builder.routes()).thenReturn(routesBuilder);
        
        // When
        RouteLocator routeLocator = gateway.dynamicRoutes(builder, repository);
        
        // Then
        StepVerifier.create(routeLocator.getRoutes())
            .expectNextMatches(route -> 
                route.getId().equals("test-service-route") &&
                (route.getUri().toString().equals("http://localhost:8080")))
            .expectComplete()
            .verify();
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