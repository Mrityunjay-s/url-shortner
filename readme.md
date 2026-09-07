# URL Shortener

A simple and scalable URL Shortener built with **Java, Spring Boot, Redis, Docker, and Azure Container Apps**.

The project is being developed with production concerns in mind, including performance, distributed state, observability, resilience, and horizontal scalability.

---

## Overview

A URL shortener converts a long URL into a compact URL.

For example:

```text
https://example.com/products/category/some-very-long-url
```

becomes:

```text
https://short.domain/aZ91x
```

When a user visits the shortened URL, the service looks up the corresponding original URL and redirects the user.

The primary focus of this project is not only implementing the basic URL-shortening functionality, but also understanding how the system can evolve into a reliable distributed service.

---

## Tech Stack

| Technology           | Purpose                             |
| -------------------- | ----------------------------------- |
| Java                 | Application language                |
| Spring Boot          | Backend framework                   |
| Redis                | URL storage and distributed counter |
| Jedis                | Redis client                        |
| Maven                | Build and dependency management     |
| Docker               | Containerization                    |
| Azure Container Apps | Application hosting                 |
| Azure Managed Redis  | Managed Redis infrastructure        |
| Spring AOP           | Cross-cutting logging               |

---

# Architecture

The current application follows a simple layered architecture:

```text
                    Client
                      │
                      ▼
              ┌───────────────┐
              │   Controller  │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │    Service    │
              └───────┬───────┘
                      │
                      ▼
              ┌───────────────┐
              │ Redis Access  │
              └───────┬───────┘
                      │
                      ▼
                   Redis
```

The application is deployed as a container on **Azure Container Apps**, while Redis is provided through **Azure Managed Redis**.

Keeping the application stateless allows multiple application instances to share the same Redis-backed state.

---

# URL Shortening Flow

The basic request flow is:

```text
POST /shorten
      │
      ▼
 Controller
      │
      ▼
   Service
      │
      ├── Generate ID
      │
      ├── Encode ID
      │
      └── Store mapping
              │
              ▼
            Redis
              │
              ▼
       Short URL Response
```

The generated short code is used as the key for the URL mapping.

For example:

```text
shortCode = aZ91x

Redis:

url:aZ91x -> https://example.com/some/long/url
```

---

# URL Redirection

When the short URL is requested:

```text
GET /aZ91x
```

the application performs a Redis lookup:

```text
aZ91x
  │
  ▼
Redis
  │
  ▼
Original URL
  │
  ▼
HTTP Redirect
```

Since URL redirection is expected to be much more frequent than URL creation, Redis provides a low-latency lookup mechanism for this workload.

---

# Redis

Redis is currently the primary data store for URL mappings.

The application uses Redis for:

* URL mappings
* Unique ID generation
* Counters
* TTL/expiration where required
* Shared state across application instances

The Redis infrastructure is managed through Azure.

---

## Redis Key Naming

Keys follow a namespace-based approach.

Example:

```text
url:aZ91x
```

As the application grows, the namespace can be made more explicit:

```text
urlshortener:url:aZ91x
urlshortener:counter
urlshortener:metadata:aZ91x
```

Using a consistent namespace helps avoid collisions and makes Redis easier to operate when multiple applications or features share the same infrastructure.

---

# Counter-Based ID Generation

One of the important design decisions in the project is how short IDs are generated.

A straightforward approach would be to generate random strings:

```text
Generate random ID
       │
       ▼
Check Redis
       │
   ┌───┴───┐
   │       │
  Free   Exists
   │       │
   ▼       ▼
 Store    Retry
```

This can work, but it introduces collision checking.

Instead, the current implementation uses a **counter-based approach**.

The flow is:

```text
Redis Counter
     │
     ▼
Numeric ID
     │
     ▼
Base62 Encoding
     │
     ▼
Short Code
```

Redis provides atomic increment operations, allowing multiple application instances to safely obtain unique IDs.

---

## Why Base62?

The generated numeric ID can be encoded using Base62.

The character set contains:

```text
0-9
a-z
A-Z
```

This allows large numeric values to be represented using relatively short strings.

For example:

```text
1000001
   │
   ▼
Base62
   │
   ▼
4C92
```

The resulting value becomes the short URL identifier.

---

# Why This Approach?

The counter + Base62 approach provides:

* Simple implementation
* Unique IDs
* No normal collision checking
* Compact URLs
* Atomic ID generation
* Easy horizontal scaling for the initial architecture

It also keeps ID generation separate from URL storage, allowing the strategy to be changed later if necessary.

---

# Trade-offs of the Counter Approach

The counter approach also has limitations.

### Predictable IDs

Sequential IDs can make URL enumeration easier.

### Global Counter

A single counter can become a hot key at very high write throughput.

### Information Leakage

Sequential IDs may expose approximate information about the number of URLs created.

These limitations are acceptable for the current stage of the project, but they are areas for future improvement.

Possible future alternatives include:

* Random identifiers
* Snowflake-style IDs
* Distributed ID generation
* Range-based ID allocation
* Randomized Base62 identifiers

---

# AOP Logging

Logging is implemented using **Spring AOP** for cross-cutting concerns.

Instead of putting logging code inside every service method, an aspect can intercept method execution.

For example:

```text
Request
   │
   ▼
 AOP Aspect
   │
   ├── Method entry
   ├── Execution timing
   ├── Exception logging
   │
   ▼
Business Method
   │
   ▼
 AOP Aspect
   │
   └── Method exit
```

This keeps the business logic clean while providing consistent logging across the application.

The AOP layer can later be extended to support:

* Correlation IDs
* Trace IDs
* Performance metrics
* Structured logging

---

# Docker

The application is containerized using Docker.

The container is designed to run in the Linux environment used by the Azure deployment platform.

Docker Buildx is used to build the application image for the required deployment architecture.

The Makefile is used to keep Docker commands simple and consistent.

Example:

```bash
make build TAG=1.0.0
```

The exact registry and deployment configuration should remain outside public documentation.

---

# Makefile

The project uses a `Makefile` to simplify common operations.

The idea is to have developers and future automation use the same commands rather than duplicating long Docker commands.

For example:

```text
make build
make test
make run
make clean
```

The Makefile will also make the eventual CI/CD implementation simpler because GitHub Actions can call the same commands used locally.

---

# Azure Deployment

The application is currently deployed to **Azure Container Apps**.

Azure Container Apps provides the runtime for the Dockerized Spring Boot application.

Redis is hosted separately using Azure Managed Redis.

This separation keeps the application stateless:

```text
Application
     │
     └── Stateless
            │
            ▼
       Azure Redis
            │
            └── Shared State
```

This allows the application to scale to multiple instances without each instance maintaining its own copy of the URL mappings.

---

# Configuration

Environment-specific configuration should be provided externally rather than hard-coded.

For example:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      username: ${REDIS_USERNAME}
      password: ${REDIS_PASSWORD}
```

Secrets should never be committed to Git.

Avoid storing credentials in:

```text
application.yml
application.properties
.env
Dockerfile
Makefile
```

Production secrets should be supplied through the deployment environment or a dedicated secret-management solution.

---

# Local Development

## Prerequisites

Install:

* Java
* Maven
* Docker
* Docker Buildx
* Git

Verify the installation:

```bash
java -version
mvn -version
docker --version
docker buildx version
git --version
```

---

## Run the Application

Clone the repository:

```bash
git clone <repository-url>
cd urlshortner
```

Build:

```bash
./mvnw clean package
```

Run:

```bash
./mvnw spring-boot:run
```

---

# Local Redis

For local development, Redis can be run using Docker:

```bash
docker run \
  --name urlshortener-redis \
  -p 6379:6379 \
  -d redis
```

The application can then connect to:

```text
localhost:6379
```

---

# Current Limitations

The current implementation intentionally focuses on getting the core system working correctly before adding more distributed-system complexity.

Current areas that need improvement include:

* Redis failure handling
* Timeouts
* Retry strategy
* Circuit breaking
* Rate limiting
* Better observability
* ID-generation scalability
* Security hardening
* Automated CI/CD

These are planned as future stages rather than being prematurely added to the initial implementation.

---

# Resiliency

The next major engineering focus is **resilience**.

Redis is currently an important dependency, so the application needs to handle Redis failures gracefully.

The planned resilience mechanisms include:

### Timeouts

Redis operations should have bounded timeouts so that a slow dependency does not block application resources indefinitely.

### Retry

Transient failures can be retried using:

* Limited retry attempts
* Exponential backoff
* Jitter

Retries should not be applied blindly to every error.

### Circuit Breaker

A circuit breaker will prevent the application from continuously sending requests to an unhealthy Redis instance.

### Bulkhead

Bulkhead isolation can prevent one type of workload from consuming all available application resources.

### Graceful Degradation

The application should return controlled responses when dependencies are unavailable rather than hanging or repeatedly retrying.

---

# Observability

The current AOP logging provides the initial foundation for observability.

The next stage will introduce:

* Structured logs
* Correlation IDs
* Request latency metrics
* Redis latency metrics
* Redis error metrics
* JVM metrics
* Application health metrics
* Distributed tracing
* Dashboards
* Alerts

The goal is to make it possible to answer questions such as:

> Why did requests become slower?

> Is Redis causing the latency?

> Which endpoint is failing?

> Is the application running out of resources?

---

# Rate Limiting

A public URL shortener needs protection against abusive traffic.

Redis can eventually be used for distributed rate limiting.

Potential limits include:

* Requests per IP
* Requests per user
* URLs created per minute
* URLs created per hour

Possible algorithms include:

* Token Bucket

The exact strategy will depend on the expected traffic and product requirements.

---

# Security

Future security improvements include:

* HTTPS
* Secure Redis connections
* Secret management
* Rate limiting
* Input validation
* Dependency vulnerability scanning
* Container image scanning
* Non-root containers
* Least-privilege access

URL validation will also be strengthened to prevent unsafe URLs from entering the system.

---

# Health Checks

The application will eventually expose health endpoints such as:

```text
/actuator/health
```

Health checks should distinguish between:

### Liveness

Is the application process alive?

### Readiness

Is this application instance ready to receive traffic?

This becomes especially important when multiple instances are running in Azure Container Apps.

---

# Scalability

The application is designed to remain stateless so that it can eventually scale horizontally.

For example:

```text
                 Load Balancer
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       App #1       App #2       App #3
          │           │           │
          └───────────┼───────────┘
                      ▼
                Managed Redis
```

The application instances do not need to know which instance previously handled a request.

All shared state is stored externally.

---

# Counter Scaling

The global counter is sufficient for the current implementation, but it could eventually become a bottleneck.

One possible optimization is range-based allocation.

Instead of requesting a new counter value for every URL, an application instance could obtain a range:

```text
Instance A → 1,000,000 - 1,999,999
Instance B → 2,000,000 - 2,999,999
```

The application can then generate IDs locally within its assigned range.

This reduces Redis counter operations.

This optimization should only be introduced after load testing demonstrates that the counter is actually a bottleneck.

---

# Testing Strategy

The project should eventually have multiple layers of testing.

## Unit Tests

Test individual components such as:

* URL generation
* Base62 encoding
* Validation
* Business logic
* Error handling

## Integration Tests

Test:

* Redis interaction
* Counter behavior
* URL storage
* URL retrieval
* TTL behavior
* Redis failures

## End-to-End Tests

Test the complete flow:

```text
Create Short URL
       │
       ▼
Receive Short Code
       │
       ▼
Request Short URL
       │
       ▼
Redirect
```

---

# Failure Testing

Resilience mechanisms should be tested by intentionally introducing failures.

Examples:

### Redis unavailable

Verify that the application:

* Times out
* Does not hang indefinitely
* Handles errors correctly
* Does not exhaust threads

### Redis latency

Introduce artificial latency and verify timeout and retry behavior.

### Application instance failure

Verify that other application instances can continue serving traffic.

### Deployment failure

Verify that a previous version can be restored.

---

# CI/CD

CI/CD is **not implemented yet**.

It is planned as a future improvement.

The intended pipeline will eventually include:

1. Build
2. Unit tests
3. Integration tests
4. Docker image build
5. Security scanning
6. Container registry push
7. Azure Container Apps deployment
8. Deployment health verification
9. Rollback

The existing Makefile will provide a consistent interface that can be reused by the CI/CD pipeline.

---

# Future Architecture

The project will evolve incrementally.

The current system is intentionally simple:

```text
Spring Boot
     │
     ▼
Redis
```

The target architecture may eventually include:

```text
                    Client
                      │
                      ▼
               Load Balancer
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       App #1       App #2       App #3
          │           │           │
          └───────────┼───────────┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       URL Store   Rate Limit   Analytics
                      │
                      ▼
                 Redis Cluster
```

Additional components will only be introduced when there is a real requirement for them.

---

# Architecture Principles

The project follows a few important principles.

### Keep the Application Stateless

Application instances should not own persistent shared state.

### Keep Configuration External

Environment-specific values and secrets should not be hard-coded.

### Start Simple

The initial architecture should solve the core problem without unnecessary distributed-system complexity.

### Measure Before Optimizing

Scaling mechanisms such as Redis Cluster or distributed ID generation should be introduced when measurements demonstrate the need.

### Fail Fast

External dependencies should have bounded timeouts.

### Retry Carefully

Only transient failures should normally be retried.

### Design for Horizontal Scaling

Adding another application instance should not require application-level changes.

### Build for Observability

Logs, metrics, and traces should make production behavior understandable.

# Design Philosophy

This project follows an incremental approach:

> **Build → Measure → Identify Bottlenecks → Improve → Scale**

Instead of adding every distributed-system pattern upfront, the system starts with a simple architecture and evolves as requirements and real-world behavior demand it.

The current focus is getting the fundamentals right:

**URL shortening → Redis → Containerization → Azure deployment**

The next focus will be:

**Resilience → Observability → Security → Scalability → CI/CD**

This approach keeps the system understandable while still providing a path toward a production-grade distributed URL Shortener.
