package com.example.apigateway.controller;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceRegistryController.class)
class ServiceRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InMemoryServiceRegistryRepository repository;

    @Test
    void registerService_ShouldReturnCreatedStatus() throws Exception {
        doNothing().when(repository).register(any());

        mockMvc.perform(post("/registry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "serviceName": "test-service",
                        "instanceId": "instance-1",
                        "baseUrl": "http://localhost:8080"
                    }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.instanceId").value("instance-1"))
                .andExpect(jsonPath("$.registrationTimestamp").exists());
    }

    @Test
    void getAllInstances_ShouldReturnInstances() throws Exception {
        var instance1 = new ServiceInstance(
            "service1", "instance1", "http://localhost:8081", Instant.now()
        );
        var instance2 = new ServiceInstance(
            "service2", "instance2", "http://localhost:8082", Instant.now()
        );

        when(repository.getAllInstances()).thenReturn(List.of(instance1, instance2));

        mockMvc.perform(get("/registry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("service1"))
                .andExpect(jsonPath("$[1].serviceName").value("service2"));
    }

    @Test
    void getServiceInstances_ShouldReturnServiceInstances() throws Exception {
        var instance = new ServiceInstance(
            "test-service", "instance-1", "http://localhost:8080", Instant.now()
        );

        when(repository.getInstancesByService("test-service"))
            .thenReturn(List.of(instance));

        mockMvc.perform(get("/registry/test-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("test-service"));
    }

    @Test
    void getServiceInstances_ShouldReturn404_WhenServiceNotFound() throws Exception {
        when(repository.getInstancesByService("unknown-service"))
            .thenReturn(List.of());

        mockMvc.perform(get("/registry/unknown-service"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deregisterService_ShouldReturnNoContent() throws Exception {
        when(repository.deregister("test-service", "instance-1"))
            .thenReturn(true);

        mockMvc.perform(delete("/registry/test-service/instance-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deregisterService_ShouldReturn404_WhenServiceNotFound() throws Exception {
        when(repository.deregister("test-service", "instance-1"))
            .thenReturn(false);

        mockMvc.perform(delete("/registry/test-service/instance-1"))
                .andExpect(status().isNotFound());
    }
}