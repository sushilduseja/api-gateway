package com.example.apigateway.controller;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collection;

@RestController
@RequestMapping("/registry")
public class ServiceRegistryController {
    private final InMemoryServiceRegistryRepository repository;

    public ServiceRegistryController(InMemoryServiceRegistryRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceInstance register(@Valid @RequestBody ServiceInstance registrationRequest) {
        var instance = new ServiceInstance(
            registrationRequest.serviceName(),
            registrationRequest.instanceId(),
            registrationRequest.baseUrl(),
            Instant.now()
        );
        repository.register(instance);
        return instance;
    }

    @DeleteMapping("/{serviceName}/{instanceId}")
    public ResponseEntity<Void> deregister(
            @PathVariable(name = "serviceName") String serviceName,
            @PathVariable(name = "instanceId") String instanceId) {
        boolean removed = repository.deregister(serviceName, instanceId);
        return removed ? ResponseEntity.noContent().build() 
                     : ResponseEntity.notFound().build();
    }

    @GetMapping
    public Collection<ServiceInstance> getAllInstances() {
        return repository.getAllInstances();
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<Collection<ServiceInstance>> getServiceInstances(
            @PathVariable(name = "serviceName") String serviceName) {
        var instances = repository.getInstancesByService(serviceName);
        return instances.isEmpty() 
            ? ResponseEntity.notFound().build()
            : ResponseEntity.ok(instances);
    }
}