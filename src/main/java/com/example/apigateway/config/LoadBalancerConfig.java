package com.example.apigateway.config;

import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerConfig.class)
public class LoadBalancerConfig {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancerConfig.class);

    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context,
            InMemoryServiceRegistryRepository registry) {
        
        return new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                log.debug("LoadBalancer requesting instances");
                var allInstances = registry.getAllInstances();
                log.debug("Found {} total instances", allInstances.size());
                
                List<ServiceInstance> springInstances = allInstances.stream()
                    .filter(instance -> instance.status() == com.example.apigateway.model.ServiceStatus.UP)
                    .map(instance -> {
                        URI uri = URI.create(instance.baseUrl());
                        return (ServiceInstance) new DefaultServiceInstance(
                            instance.instanceId(),
                            instance.serviceName(),
                            uri.getHost(),
                            uri.getPort(),
                            uri.getScheme().equalsIgnoreCase("https")
                        );
                    })
                    .collect(Collectors.toList());
                
                return Flux.just(springInstances);
            }
        };
    }
}