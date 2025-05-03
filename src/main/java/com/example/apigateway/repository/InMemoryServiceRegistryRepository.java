package com.example.apigateway.repository;

import com.example.apigateway.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class InMemoryServiceRegistryRepository {
    private static final Logger log = LoggerFactory.getLogger(InMemoryServiceRegistryRepository.class);
    
    private final ConcurrentMap<String, ConcurrentMap<String, ServiceInstance>> registry = new ConcurrentHashMap<>();

    public void register(ServiceInstance instance) {
        registry.computeIfAbsent(instance.serviceName(), k -> new ConcurrentHashMap<>())
                .put(instance.instanceId(), instance);
        log.info("Registered: {}/{} at {}", 
            instance.serviceName(), instance.instanceId(), instance.baseUrl());
    }

    public boolean deregister(String serviceName, String instanceId) {
        var serviceMap = registry.get(serviceName);
        if (serviceMap == null || !serviceMap.containsKey(instanceId)) {
            return false;
        }

        serviceMap.remove(instanceId);
        if (serviceMap.isEmpty()) {
            registry.remove(serviceName);
        }

        log.info("Deregistered: {}/{}", serviceName, instanceId);
        return true;
    }

    public Collection<ServiceInstance> getAllInstances() {
        return registry.values().stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());
    }

    public Collection<ServiceInstance> getInstancesByService(String serviceName) {
        return registry.getOrDefault(serviceName, new ConcurrentHashMap<>()).values();
    }
}