package com.example.apigateway.controller;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.model.ServiceStatus;
import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/registry")
public class ServiceRegistryController {
    private final InMemoryServiceRegistryRepository repository;

    public ServiceRegistryController(InMemoryServiceRegistryRepository repository) {
        this.repository = repository;
    }

    public record StatusUpdateRequest(@NotNull ServiceStatus status) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ServiceInstance> register(@Valid @RequestBody ServiceInstance registrationRequest) {
        var instance = new ServiceInstance(
            registrationRequest.serviceName(),
            registrationRequest.instanceId(),
            registrationRequest.baseUrl(),
            ServiceStatus.UP,
            Instant.now()
        );
        return Mono.fromCallable(() -> {
            repository.register(instance);
            return instance;
        });
    }

    @DeleteMapping("/{serviceName}/{instanceId}")
    public Mono<Void> deregister(
            @PathVariable(name = "serviceName") String serviceName,
            @PathVariable(name = "instanceId") String instanceId) {
        return Mono.fromCallable(() -> repository.deregister(serviceName, instanceId))
                .filter(removed -> removed)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Service not found")))
                .then();
    }

    @PutMapping("/{serviceName}/{instanceId}/status")
    public Mono<Void> updateStatus(
            @PathVariable String serviceName,
            @PathVariable String instanceId,
            @Valid @RequestBody StatusUpdateRequest statusUpdate) {
        if (statusUpdate instanceof StatusUpdateRequest(ServiceStatus newStatus)) {
            return Mono.fromCallable(() -> repository.updateStatus(serviceName, instanceId, newStatus))
                    .filter(updated -> updated)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Service not found")))
                    .then();
        }
        return Mono.error(new IllegalArgumentException("Invalid status update request"));
    }

    @GetMapping
    public Flux<ServiceInstance> getAllInstances() {
        return Flux.fromIterable(repository.getAllInstances());
    }

    @GetMapping("/{serviceName}")
    public Flux<ServiceInstance> getServiceInstances(
            @PathVariable(name = "serviceName") String serviceName) {
        var instances = repository.getInstancesByService(serviceName);
        if (instances.isEmpty()) {
            return Flux.error(new IllegalArgumentException("Service not found"));
        }
        return Flux.fromIterable(instances);
    }

    @GetMapping("/{serviceName}/{instanceId}/summary")
    public Mono<String> getInstanceSummary(
            @PathVariable String serviceName,
            @PathVariable String instanceId) {
        
        return Mono.fromCallable(() -> repository.getInstance(serviceName, instanceId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Instance not found")))
                .map(instance -> {
                    if (instance instanceof ServiceInstance(String svcName, String id, String url, ServiceStatus status, Instant regTime)) {
                        return String.format("""
                                Service Instance Summary:
                                ----------------------
                                Service Name: %s
                                Instance ID : %s
                                URL        : %s
                                Status     : %s
                                Registered : %s
                                ----------------------
                                """, svcName, id, url, status, regTime);
                    }
                    throw new IllegalStateException("Unexpected instance type encountered");
                });
    }
}