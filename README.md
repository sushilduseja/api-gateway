Okay, here is the updated README reflecting the latest code, incorporating dynamic routing with Spring Cloud LoadBalancer and the reactive nature of the application.

# Reactive API Gateway & Service Registry

A Spring Boot-based API Gateway service built on the reactive stack (Spring WebFlux, Project Reactor) that provides dynamic service discovery, routing, and load balancing capabilities using an in-memory service registry.

## Features

*   **In-Memory Service Registry:** Allows backend services to register/deregister themselves and update their status (UP/DOWN).
*   **Dynamic Routing:** Uses Spring Cloud Gateway to route incoming requests based on path prefixes (`/gateway/{serviceName}/**`).
*   **Dynamic Load Balancing:** Integrates with Spring Cloud LoadBalancer and a custom `ServiceInstanceListSupplier` to discover available (`UP`) service instances from the registry *at runtime* and balance requests across them (default: Round Robin).
*   **Service Status Awareness:** Only routes requests to instances currently marked as `UP`.
*   **Reactive Stack:** Built entirely with Spring WebFlux and Project Reactor (Mono/Flux) for non-blocking I/O.
*   **Java 21 Features:** Leverages modern Java features like Records, Record Patterns, and Text Blocks.

## Core Technologies

*   Java 21
*   Spring Boot 3+
*   Spring Cloud Gateway
*   Spring Cloud LoadBalancer
*   Spring WebFlux / Project Reactor
*   Gradle

## Prerequisites

*   Java 21 or higher installed and configured (ensure `JAVA_HOME` is set).
*   Gradle (the project includes a Gradle wrapper `./gradlew`).
*   Git.

## Getting Started

1.  **Clone the repository:**
    ```bash
    git clone [your-repository-url]
    cd [repository-directory-name]
    ```

2.  **Build the project:**
    *(This will also download dependencies)*
    ```bash
    ./gradlew build
    ```
    *(On Windows, use `gradlew build`)*

3.  **Run the application:**
    ```bash
    ./gradlew bootRun
    ```
    *(On Windows, use `gradlew bootRun`)*

    Alternatively, you can run the built JAR:
    ```bash
    java -jar build/libs/api-gateway-*.jar
    ```
    *(Adjust JAR name if necessary)*

The application will start on port `8080` by default. You should see logs indicating the definition of LoadBalanced routes.

## How it Works

1.  **Service Registry:**
    *   The `InMemoryServiceRegistryRepository` acts as a simple, in-memory database holding service registration details.
    *   Services are represented by the `ServiceInstance` record (service name, instance ID, base URL, status, timestamp).
    *   REST endpoints under `/registry` allow external services (or manual tools like `curl`) to register, deregister, and update their status.

2.  **API Gateway Routing:**
    *   `GatewayConfig.java` defines route rules using Spring Cloud Gateway's `RouteLocatorBuilder`.
    *   It creates routes matching the pattern `/gateway/{serviceName}/**`.
    *   Crucially, it uses the `lb://{serviceName}` URI scheme (e.g., `lb://service-a`). This tells the gateway to hand off request handling to the Spring Cloud LoadBalancer mechanism for the specified `serviceName`.

3.  **Dynamic Load Balancing & Instance Discovery:**
    *   `LoadBalancerConfig.java` provides a custom `ServiceInstanceListSupplier` bean.
    *   When the gateway receives a request for `lb://{serviceName}`, the LoadBalancer asks this supplier for a list of available instances for that `serviceName`.
    *   The supplier queries the `InMemoryServiceRegistryRepository` *at that moment*, filters for instances with `status == UP`, and maps them to Spring Cloud's `ServiceInstance` format.
    *   The LoadBalancer then chooses one instance (default: Round Robin) and forwards the request.
    *   This ensures routing decisions are based on the *current* registry state without requiring gateway restarts.

## API Endpoints

### Service Registry API (on port 8080)

*   `POST /registry`: Register a new service instance. Expects JSON body like `{"serviceName": "...", "instanceId": "...", "baseUrl": "..."}`. Returns the registered instance with `status: UP` and timestamp.
*   `DELETE /registry/{serviceName}/{instanceId}`: Deregister a specific service instance. Returns 2xx on success, error otherwise.
*   `PUT /registry/{serviceName}/{instanceId}/status`: Update the status of an instance. Expects JSON body `{"status": "UP"}` or `{"status": "DOWN"}`. Returns 2xx on success, error otherwise.
*   `GET /registry`: Get a list of all registered service instances.
*   `GET /registry/{serviceName}`: Get a list of all instances for a specific service name.
*   `GET /registry/{serviceName}/{instanceId}/summary`: Get a formatted text summary of a specific instance.

### API Gateway Usage (on port 8080)

*   Send requests to `http://localhost:8080/gateway/{serviceName}/{downstreamPath}`.
*   The gateway will:
    1.  Identify the `{serviceName}`.
    2.  Ask the LoadBalancer for an `UP` instance of that service from the registry.
    3.  Rewrite the path to remove `/gateway/{serviceName}`.
    4.  Forward the request with the rewritten path (`/{downstreamPath}`) to the chosen instance's `baseUrl`.
    5.  Return the response from the downstream service.
*   If no `UP` instances are found for `{serviceName}` at request time, it will typically return a `503 Service Unavailable`.
*   If the `{serviceName}` itself is completely unknown or the path doesn't match any service route, it will return a `404 Not Found`.

## Development

1.  Import the project into your IDE (IntelliJ IDEA recommended, supports Gradle projects well).
2.  Make code changes.
3.  Run unit/integration tests:
    ```bash
    ./gradlew test
    ```
4.  Build the application:
    ```bash
    ./gradlew build
    ```
5.  Run locally using `./gradlew bootRun` or by running the `ApiGatewayApplication` class directly from your IDE.

## Limitations

*   **In-Memory Registry:** Service registrations are lost when the gateway application stops. This is suitable for demos and testing but not for production. A persistent store (like Consul, Eureka, ZooKeeper, or a database) would be needed for production use.
*   **Basic Load Balancing:** Uses the default Spring Cloud LoadBalancer strategy (Round Robin). More sophisticated strategies might be needed for complex scenarios.
*   **No Security:** The registry endpoints and gateway routes are unsecured. Authentication and authorization would be essential additions.
