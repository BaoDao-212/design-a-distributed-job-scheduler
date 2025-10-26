# Backend Documentation

This folder contains streamlined documentation for the backend application. Each file serves a specific purpose without overlap.

## Documentation Index

### 📊 [DIAGRAMS.md](./DIAGRAMS.md)
**Architecture and flow diagrams**
- High-level service overview
- Sequence diagrams (submission, dispatch, execution)
- Leader election & health monitoring flows
- Segment partitioning and scaling strategy

### 📋 [ARCHITECTURE.md](./ARCHITECTURE.md)
**System architecture and design principles**
- Project structure and technology stack
- Kanban-core integration patterns
- Component relationships and data flow

### 🚀 [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md)
**Complete development workflow and implementation guide**
- Setup instructions and prerequisites
- Step-by-step component creation (DTOs, entities, services, controllers)
- Code templates and examples
- Best practices and coding standards
- **CRITICAL RULES and restrictions**

### ⚙️ [CONFIGURATION.md](./CONFIGURATION.md)
**Environment and deployment configuration**
- Application configuration files
- Database and security settings
- Environment-specific configurations
- Docker and deployment setup

### 🔄 [API_PATTERNS.md](./API_PATTERNS.md)
**API design and response patterns**
- Standardized response structures
- Controller implementation patterns
- Error handling and validation
- API versioning and migration

## Quick Navigation

### For New Developers
1. Start with [DIAGRAMS.md](./DIAGRAMS.md) to visualize the system architecture
2. Read [ARCHITECTURE.md](./ARCHITECTURE.md) to understand the system design
3. Follow [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md) for complete development workflow
4. Review [API_PATTERNS.md](./API_PATTERNS.md) for API implementation standards
5. Refer to [CONFIGURATION.md](./CONFIGURATION.md) for environment setup

### For Experienced Developers
- Use [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md) for patterns, templates, and critical rules
- Check [API_PATTERNS.md](./API_PATTERNS.md) for response standards
- Refer to [CONFIGURATION.md](./CONFIGURATION.md) for deployment configurations

## Key Technologies

- **Framework**: Spring Boot 3.5.5 with Java 21
- **Database**: PostgreSQL with Spring Data JPA
- **Core Library**: Kanban-Core 3.2.26 (custom framework)
- **Build Tool**: Maven
- **Additional**: Lombok, MapStruct

## Critical Rules

⚠️ **MANDATORY READING**: All developers must read and follow the critical rules in [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md), especially:
- **NO JPA Relationships** (@OneToMany, @ManyToOne, @OneToOne, @ManyToMany)
- Entity design restrictions
- Mandatory patterns and conventions

---

**Note**: This documentation is streamlined to avoid redundancy. Each file serves a specific purpose without overlap.
