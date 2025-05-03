package com.example.apigateway.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public record ServiceInstance(
    @NotBlank(message = "Service name is required")
    String serviceName,
    
    @NotBlank(message = "Instance ID is required")
    String instanceId,
    
    @NotBlank(message = "Base URL is required")
    @Pattern(regexp = "^https?://.*", message = "Base URL must start with http:// or https://")
    String baseUrl,
    
    Instant registrationTimestamp
) {}