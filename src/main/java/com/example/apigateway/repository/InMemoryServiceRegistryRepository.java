package com.example.apigateway.repository;

import com.example.apigateway.model.ServiceInstance;
import com.example.apigateway.model.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
public class InMemoryServiceRegistryRepository {
    private static final Logger log = LoggerFactory.getLogger(InMemoryServiceRegistryRepository.class);
    
    private final ConcurrentMap<String, Map<String, ServiceInstance>> registry = new ConcurrentHashMap<>();

    public void register(ServiceInstance instance) {
        // Ensure new instances are registered with UP status
        ServiceInstance registeredInstance = new ServiceInstance(
            instance.serviceName(),
            instance.instanceId(),
            instance.baseUrl(),
            ServiceStatus.UP,
            instance.registrationTimestamp()
        );

        registry.computeIfAbsent(registeredInstance.serviceName(),
                k -> Collections.synchronizedMap(new LinkedHashMap<>()))
                .put(registeredInstance.instanceId(), registeredInstance);
        log.info("Registered: {}/{} at {} with status {}", 
            registeredInstance.serviceName(), registeredInstance.instanceId(), registeredInstance.baseUrl(), registeredInstance.status());
    }

    public boolean updateStatus(String serviceName, String instanceId, ServiceStatus newStatus) {
        Map<String, ServiceInstance> instances = registry.get(serviceName);
        if (instances != null) {
            synchronized (instances) {
                ServiceInstance currentInstance = instances.get(instanceId);
                if (currentInstance != null) {
                    ServiceInstance updatedInstance = new ServiceInstance(
                        currentInstance.serviceName(),
                        currentInstance.instanceId(),
                        currentInstance.baseUrl(),
                        newStatus,
                        currentInstance.registrationTimestamp()
                    );
                    instances.put(instanceId, updatedInstance);
                    log.info("Updated status for {}/{} to {}", serviceName, instanceId, newStatus);
                    return true;
                }
            }
        }
        log.warn("Failed to update status for non-existent instance: {}/{}", serviceName, instanceId);
        return false;
    }

    public List<ServiceInstance> getUpInstancesByService(String serviceName) {
        return registry.getOrDefault(serviceName, Collections.emptyMap())
                .values().stream()
                .filter(instance -> instance.status() == ServiceStatus.UP)
                .toList();
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
        return registry.getOrDefault(
                serviceName,
                Collections.synchronizedMap(new LinkedHashMap<>())
            ).values();
    }

    public ServiceInstance getInstance(String serviceName, String instanceId) {
        return registry.getOrDefault(
                serviceName,
                Collections.synchronizedMap(new LinkedHashMap<>())
            ).get(instanceId);
    }
}