package com.example.apigateway.controller;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.model.ServiceStatus;
import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@WebFluxTest(ServiceRegistryController.class)
class ServiceRegistryControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private InMemoryServiceRegistryRepository repository;

    private static final Instant FIXED_TIME = Instant.parse("2025-05-03T10:15:30Z");

    @Test
    void registerService_ShouldSetStatusToUpAndTimestamp() {
        // Given
        ArgumentCaptor<ServiceInstance> instanceCaptor = ArgumentCaptor.forClass(ServiceInstance.class);
        doNothing().when(repository).register(instanceCaptor.capture());

        // When & Then
        webClient.post()
                .uri("/registry")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "serviceName": "test-service",
                        "instanceId": "instance-1",
                        "baseUrl": "http://localhost:8080"
                    }""")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.serviceName").isEqualTo("test-service")
                .jsonPath("$.instanceId").isEqualTo("instance-1")
                .jsonPath("$.baseUrl").isEqualTo("http://localhost:8080")
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.registrationTimestamp").exists();

        // Verify the registered instance
        ServiceInstance registeredInstance = instanceCaptor.getValue();
        assertEquals(ServiceStatus.UP, registeredInstance.status());
        assertNotNull(registeredInstance.registrationTimestamp());
    }

    @Test
    void updateStatus_ShouldReturnOkWhenSuccessful() {
        // Given
        when(repository.updateStatus("test-service", "instance-1", ServiceStatus.DOWN))
                .thenReturn(true);

        // When & Then
        webClient.put()
                .uri("/registry/test-service/instance-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "status": "DOWN"
                    }""")
                .exchange()
                .expectStatus().isOk();

        verify(repository).updateStatus("test-service", "instance-1", ServiceStatus.DOWN);
    }

    @Test
    void updateStatus_ShouldReturnNotFoundWhenInstanceDoesNotExist() {
        // Given
        when(repository.updateStatus("test-service", "instance-1", ServiceStatus.DOWN))
                .thenReturn(false);

        // When & Then
        webClient.put()
                .uri("/registry/test-service/instance-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "status": "DOWN"
                    }""")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void updateStatus_ShouldReturnBadRequestForInvalidStatus() {
        webClient.put()
                .uri("/registry/test-service/instance-1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "status": "INVALID_STATUS"
                    }""")
                .exchange()
                .expectStatus().isBadRequest();

        verify(repository, never()).updateStatus(any(), any(), any());
    }

    @Test
    void getAllInstances_ShouldReturnAllInstancesFromRepository() {
        // Given
        var instance1 = createTestInstance("service1", "instance1", ServiceStatus.UP);
        var instance2 = createTestInstance("service2", "instance2", ServiceStatus.DOWN);
        when(repository.getAllInstances()).thenReturn(List.of(instance1, instance2));

        // When & Then
        webClient.get()
                .uri("/registry")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].serviceName").isEqualTo("service1")
                .jsonPath("$[0].status").isEqualTo("UP")
                .jsonPath("$[1].serviceName").isEqualTo("service2")
                .jsonPath("$[1].status").isEqualTo("DOWN");
    }

    @Test
    void getServiceInstances_ShouldReturnInstancesForService() {
        // Given
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.UP);
        when(repository.getInstancesByService("test-service"))
                .thenReturn(List.of(instance));

        // When & Then
        webClient.get()
                .uri("/registry/test-service")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].serviceName").isEqualTo("test-service")
                .jsonPath("$[0].status").isEqualTo("UP");
    }

    @Test
    void getServiceInstances_ShouldReturn404WhenNoInstancesFound() {
        // Given
        when(repository.getInstancesByService("unknown-service"))
                .thenReturn(Collections.emptyList());

        // When & Then
        webClient.get()
                .uri("/registry/unknown-service")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getInstanceSummary_ShouldIncludeStatus() {
        // Given
        var instance = createTestInstance("test-service", "instance-1", ServiceStatus.UP);
        when(repository.getInstance("test-service", "instance-1"))
                .thenReturn(instance);

        // When & Then
        webClient.get()
                .uri("/registry/test-service/instance-1/summary")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assert response.contains("Status      : UP");
                });
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