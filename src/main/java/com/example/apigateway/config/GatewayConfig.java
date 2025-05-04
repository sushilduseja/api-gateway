package com.example.apigateway.config;

import com.example.apigateway.repository.InMemoryServiceRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class GatewayConfig {
    private static final Logger log = LoggerFactory.getLogger(GatewayConfig.class);

    @Bean
    public RouteLocator dynamicRoutes(RouteLocatorBuilder builder, InMemoryServiceRegistryRepository registry) {
        log.info("Building dynamic routes using LoadBalancer...");
        var routes = builder.routes();

        // Get distinct service names from registry
        Set<String> serviceNames = registry.getAllInstances().stream()
                .map(instance -> instance.serviceName())
                .collect(Collectors.toSet());

        // Create a LoadBalancer route for each service
        serviceNames.forEach(serviceName -> {
            log.info("Defining LoadBalanced route for service: {}", serviceName);
            routes.route(serviceName + "-route",
                r -> r.path("/gateway/" + serviceName + "/**")
                    .filters(f -> f.rewritePath(
                        "/gateway/" + serviceName + "/(?<remaining>.*)",
                        "/${remaining}"))
                    .uri("lb://" + serviceName));
        });

        // Fallback route for unknown services
        routes.route("fallback-404",
            r -> r.path("/gateway/**")
                .filters(f -> f.setStatus(404))
                .uri("no://op"));

        log.info("Finished defining dynamic routes");
        return routes.build();
    }
}