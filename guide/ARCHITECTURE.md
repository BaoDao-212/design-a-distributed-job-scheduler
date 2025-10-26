# System Architecture

## Overview

Spring Boot 3.5.5 application with Java 21, following a layered architecture pattern. Built on the custom `vn.com.mbbank.kanban.core` library (version 3.2.26) for foundational components and utilities.

## Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Java Version**: 21
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security
- **Build Tool**: Maven
- **Code Generation**: Lombok, MapStruct
- **Core Library**: Kanban-Core 3.2.26

## Project Structure

```
backend/
├── src/main/java/vn/com/mbbank/kanban/ai_contest/
│   ├── AiContestApplication.java    # Main application class
│   ├── constants/                   # Application constants
│   ├── controllers/                 # REST controllers
│   ├── dtos/                        # Data Transfer Objects
│   │   ├── request/                 # Request DTOs
│   │   └── response/                # Response DTOs
│   ├── entities/                    # JPA entities (NO RELATIONSHIPS)
│   ├── enums/                       # Enumeration definitions
│   ├── mappers/                     # MapStruct object mappers
│   ├── repositories/                # Data access layer
│   │   └── impl/                    # Custom repository implementations
│   └── services/                    # Business logic layer
│       └── impl/                    # Service implementations
├── src/main/resources/
│   ├── application.yml              # Main configuration
│   └── application-prod.yml         # Production configuration
└── pom.xml                          # Maven dependencies
```

## Architecture Layers

### 1. Controller Layer (REST API)
- REST endpoints and request handling
- Input validation using Bean Validation
- Response formatting using ResponseUtils
- Exception handling delegation

### 2. Service Layer (Business Logic)
- Business logic and transaction management
- DTO to Entity mapping using MapStruct
- Exception handling using BusinessException
- Integration with kanban-core base services

### 3. Repository Layer (Data Access)
- Standard JPA operations via JpaCommonRepository
- Custom queries using SqlQueryUtil
- No entity relationships - flat data model only

### 4. Entity Layer (Domain Models)
- Simple entities extending BaseEntity<ID>
- No JPA relationships (@OneToMany, @ManyToOne, etc.)
- Audit fields managed by kanban-core

## Kanban-Core Integration

The application leverages `vn.com.mbbank.kanban.core` for:

### Base Classes
- `BaseEntity<ID>` - Entity auditing and common fields
- `BaseService<T, ID>` - Common service operations
- `BaseServiceImpl<T, ID>` - Service implementation base
- `JpaCommonRepository<T, ID>` - Repository operations
- `KanbanBaseMapper<T, S>` - MapStruct mapping base

### Utilities
- `KanbanCommonUtil` - Common validation and utility methods
- `SqlQueryUtil` - SQL query execution utilities
- `ResponseUtils` - Standardized API response formatting
- `KanbanStringUtils`, `KanbanDateUtils` - String and date utilities

### Configuration
- Security configurations
- JPA/Hibernate configurations
- Exception handling framework
- CORS and authentication settings

## Design Principles

### 1. Flat Data Model
- **NO JPA relationships** between entities
- Each entity is independent and self-contained
- Related data accessed through service layer coordination
- Foreign keys stored as simple ID fields

### 2. DTO-Driven API
- Separate request and response DTOs
- Clean separation between API contracts and domain models
- MapStruct for compile-time mapping generation

### 3. Dependency Injection
- Constructor injection using Lombok's `@RequiredArgsConstructor`
- Spring's IoC container for component management
- Clear dependency boundaries between layers

### 4. Exception-Driven Error Handling
- Custom `ErrorCode` enum for all business exceptions
- Consistent error response format
- Parameterized error messages for internationalization

## Component Relationships

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │───▶│    Services     │───▶│  Repositories   │
│                 │    │                 │    │                 │
│ - REST endpoints│    │ - Business logic│    │ - Data access   │
│ - Validation    │    │ - DTO mapping   │    │ - Custom queries│
│ - Response fmt  │    │ - Transactions  │    │ - JPA operations│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│      DTOs       │    │     Mappers     │    │    Entities     │
│                 │    │                 │    │                 │
│ - Request DTOs  │    │ - MapStruct     │    │ - Simple POJOs  │
│ - Response DTOs │    │ - Type-safe     │    │ - No relations  │
│ - Validation    │    │ - Compile-time  │    │ - Audit fields  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Data Flow

1. **Request Processing**:
   - Controller receives HTTP request
   - Bean Validation validates request DTO
   - Controller delegates to service layer

2. **Business Logic**:
   - Service validates business rules
   - MapStruct converts DTO to Entity
   - Service coordinates with repositories

3. **Data Access**:
   - Repository performs database operations
   - Custom queries use SqlQueryUtil
   - Results returned as entities

4. **Response Generation**:
   - MapStruct converts Entity to Response DTO
   - ResponseUtils formats standardized response
   - Controller returns ResponseData<T>

## Security Model

- Authentication handled by kanban-core security framework
- URL-based whitelisting for public endpoints
- CORS configuration for frontend integration
- No method-level security annotations required

## Performance Considerations

- Connection pooling via HikariCP
- Query optimization through custom repositories
- Caching framework available via kanban-core
- Execution time monitoring with `@LogExecutionTime`

---

**Key Architectural Decisions**:
1. **No JPA relationships** to maintain simplicity and performance
2. **DTO-first design** for clean API contracts
3. **Kanban-core integration** for consistency across projects
4. **Flat entity model** for easier maintenance and testing
