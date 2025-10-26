# Configuration Guide

## Application Configuration

### application.yml (Development)
```yaml
server:
  port: 8002
  servlet:
    context-path: /api

spring:
  application.name: ai-contest
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://118.71.167.52:5432/contestai
    username: contestai
    password: contestai
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  
  # JPA/Hibernate Configuration
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate.format_sql: true
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect

# Logging Configuration
logging:
  level:
    org.hibernate.tool.hbm2ddl: DEBUG
    org.hibernate.SQL: DEBUG

# Kanban Framework Configuration
kanban:
  general:
    white-list:
      origin: localhost:8000
  authentication:
    white-list-url: /*
```

### application-prod.yml (Production)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:prod-db-host}:${DB_PORT:5432}/${DB_NAME:prod_database}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      
  jpa:
    hibernate:
      ddl-auto: validate  # NEVER use 'update' in production
    show-sql: false

logging:
  level:
    org.hibernate.SQL: WARN
    org.hibernate.tool.hbm2ddl: WARN
    vn.com.mbbank.kanban: INFO

kanban:
  general:
    white-list:
      origin: ${FRONTEND_DOMAIN:your-frontend-domain.com}
  authentication:
    white-list-url: /health,/actuator/**
```

## Database Setup

### PostgreSQL Configuration
```sql
-- Create database and user
CREATE DATABASE contestai;
CREATE USER contestai WITH PASSWORD 'contestai';
GRANT ALL PRIVILEGES ON DATABASE contestai TO contestai;

-- For production, use strong passwords
CREATE USER prod_user WITH PASSWORD '${STRONG_PASSWORD}';
GRANT ALL PRIVILEGES ON DATABASE prod_database TO prod_user;
```

### Connection Pool Settings
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # Adjust based on load
      minimum-idle: 2            # Min idle connections
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000       # 10 minutes
      max-lifetime: 1800000      # 30 minutes
      leak-detection-threshold: 60000  # 1 minute
```

## JPA/Hibernate Configuration

### DDL Auto Options
- **none**: No schema management (production)
- **validate**: Validate schema matches entities (production)
- **update**: Update schema (development only)
- **create**: Drop and create schema (testing only)
- **create-drop**: Create on start, drop on shutdown (testing only)

### Performance Settings
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 20
        order_inserts: true
        order_updates: true
        generate_statistics: false  # Enable only for debugging
```

## Security Configuration

### Authentication Whitelist
```yaml
kanban:
  authentication:
    white-list-url: |
      /health,
      /actuator/**,
      /swagger-ui/**,
      /v3/api-docs/**,
      /products/public
```

### CORS Configuration
```yaml
kanban:
  general:
    white-list:
      origin: |
        localhost:3000,
        localhost:8000,
        ${FRONTEND_DOMAIN:your-frontend-domain.com}
```

## Environment-Specific Configuration

### Development (application-dev.yml)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    
logging:
  level:
    vn.com.mbbank.kanban: DEBUG
    org.hibernate.SQL: DEBUG
```

### Testing (application-test.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
```

## Docker Configuration

### Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/ai-contest-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8002

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8002:8002"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=db
      - DB_USERNAME=contestai
      - DB_PASSWORD=contestai
    depends_on:
      - db
      
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: contestai
      POSTGRES_USER: contestai
      POSTGRES_PASSWORD: contestai
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## Monitoring Configuration

### Actuator Setup
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### Health Check Endpoint
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withException(e)
                .build();
        }
        return Health.down().build();
    }
}
```

## Environment Variables

### Required for Production
```bash
# Database
export DB_HOST=your-db-host
export DB_PORT=5432
export DB_NAME=your-database
export DB_USERNAME=your-username
export DB_PASSWORD=your-strong-password

# Application
export SPRING_PROFILES_ACTIVE=prod
export FRONTEND_DOMAIN=your-frontend-domain.com

# Optional
export SERVER_PORT=8080
export LOG_LEVEL=INFO
```

## Common Issues & Solutions

### Database Connection Failed
```bash
# Check database connectivity
telnet your-db-host 5432

# Verify credentials
psql -h your-db-host -U your-username -d your-database
```

### Port Already in Use
```bash
# Find process using port
lsof -i :8002

# Kill process
kill -9 <PID>

# Or change port in application.yml
server:
  port: 8003
```

### Schema Validation Errors
```yaml
# For development - auto-update schema
spring:
  jpa:
    hibernate:
      ddl-auto: update

# For production - validate only
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

---

**Configuration Checklist:**
1. ✅ Use environment variables for sensitive data
2. ✅ Set `ddl-auto: validate` in production
3. ✅ Configure appropriate connection pool sizes
4. ✅ Enable health checks for monitoring
5. ✅ Use strong passwords in production
6. ✅ Configure CORS for your frontend domain
