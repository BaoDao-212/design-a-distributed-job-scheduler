# Development Guide

## 🚨 CRITICAL RULES - READ FIRST

### ❌ STRICTLY FORBIDDEN - JPA Relationships

**NEVER use these annotations in entities:**
- `@OneToMany`
- `@ManyToOne`
- `@OneToOne`
- `@ManyToMany`
- `@JoinColumn`
- `@JoinTable`

**Why forbidden:**
- Causes performance issues with lazy loading
- Creates circular dependencies and serialization problems
- Makes testing and mocking difficult
- Violates our flat data model architecture

**✅ Instead, use:**
- Simple ID fields to reference other entities
- Service layer coordination for related data
- Multiple repository calls when needed
- DTOs to aggregate data from multiple entities

### ❌ Entity Relationship Example (FORBIDDEN)
```java
// ❌ NEVER DO THIS
@Entity
public class User extends BaseEntity<Long> {
    @OneToMany(mappedBy = "user")  // FORBIDDEN!
    private List<Order> orders;

    @ManyToOne                     // FORBIDDEN!
    @JoinColumn(name = "role_id")  // FORBIDDEN!
    private Role role;
}
```

### ✅ Correct Entity Design
```java
// ✅ CORRECT APPROACH
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    // Simple ID reference - NO relationship annotation
    @Column(name = "role_id")
    private Long roleId;

    // Get related data through service layer, not entity relationships
}
```

## Quick Start

### Prerequisites
- Java 21
- Maven 3.6+
- PostgreSQL database
- IDE with Lombok plugin

### Setup Steps
1. Clone the repository
2. Configure database connection in `application.yml`
3. Run `mvn clean install` to build the project
4. Start the application with `mvn spring-boot:run`
5. Access the API at `http://localhost:8002/api`

## Development Workflow

### Step 1: Create DTOs

**Request DTO (in `dtos/request/`):**
```java
package vn.com.mbbank.kanban.ai_contest.dtos.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ProductRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Price is required")
    private Integer price;

    private String description;

    // For updates - include ID
    private Long id;
}
```

**Response DTO (in `dtos/response/`):**
```java
package vn.com.mbbank.kanban.ai_contest.dtos.response;

import lombok.Data;
import java.time.Instant;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private Integer price;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### Step 2: Create Entity (NO RELATIONSHIPS!)

```java
@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductEntity extends BaseEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(length = 1000)
    private String description;

    // ✅ CORRECT: Simple ID reference (no @ManyToOne!)
    @Column(name = "category_id")
    private Long categoryId;

    // ❌ NEVER add @OneToMany, @ManyToOne, @OneToOne, @ManyToMany
}
```

### Step 3: Create Mappers

**Request to Entity Mapper:**
```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    componentModel = "spring")
public interface ProductRequestToEntityMapper extends
    KanbanBaseMapper<ProductRequest, ProductEntity> {

    ProductRequestToEntityMapper INSTANCE =
        Mappers.getMapper(ProductRequestToEntityMapper.class);
}
```

**Entity to Response Mapper:**
```java
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    componentModel = "spring")
public interface ProductEntityToResponseMapper extends
    KanbanBaseMapper<ProductEntity, ProductResponse> {

    ProductEntityToResponseMapper INSTANCE =
        Mappers.getMapper(ProductEntityToResponseMapper.class);
}
```

### Step 4: Add Error Codes

Add domain-specific error codes to the `ErrorCode` enum:
```java
public enum ErrorCode implements BaseErrorCode {
    // Product domain errors (100-199)
    PRODUCT_NAME_IS_REQUIRED("100", "Product name is required", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND("101", "Product with ID {0} not found", HttpStatus.NOT_FOUND),
    PRODUCT_ALREADY_EXISTS("102", "Product with name {0} already exists", HttpStatus.CONFLICT),
    PRODUCT_PRICE_INVALID("103", "Product price must be greater than 0", HttpStatus.BAD_REQUEST),

    // User domain errors (200-299)
    USER_NOT_FOUND("200", "User with ID {0} not found", HttpStatus.NOT_FOUND),

    // System errors (400-499)
    VALIDATION_ERROR("400", "Validation failed: {0}", HttpStatus.BAD_REQUEST);

    // Constructor and methods remain the same...
}
```

**Error Code Categories:**
- **100-199**: Product domain
- **200-299**: User domain
- **300-399**: Order domain
- **400-499**: System/validation errors
- **500-599**: External service errors

### Step 5: Create Repository

**Main Repository:**
```java
public interface ProductRepository extends JpaCommonRepository<ProductEntity, Long>, ProductRepositoryCustom {
    // Standard JPA methods for simple queries
    List<ProductEntity> findByName(String name);
    boolean existsByName(String name);
    List<ProductEntity> findByCategoryId(Long categoryId);
}
```

**Custom Repository Interface:**
```java
public interface ProductRepositoryCustom {
    List<ProductEntity> findByComplexCriteria(String criteria);
    List<ProductEntity> findProductsWithPriceRange(Integer minPrice, Integer maxPrice);
}
```

**Custom Repository Implementation:**
```java
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
    private final SqlQueryUtil sqlQueryUtil;

    @Override
    public List<ProductEntity> findByComplexCriteria(String criteria) {
        String sql = """
            SELECT * FROM products
            WHERE name LIKE :criteria
            OR description LIKE :criteria
            ORDER BY created_date DESC
            """;
        return sqlQueryUtil.queryModel().queryForList(sql,
            Map.of("criteria", "%" + criteria + "%"),
            ProductEntity.class);
    }

    @Override
    public List<ProductEntity> findProductsWithPriceRange(Integer minPrice, Integer maxPrice) {
        String sql = """
            SELECT * FROM products
            WHERE price BETWEEN :minPrice AND :maxPrice
            ORDER BY price ASC
            """;
        return sqlQueryUtil.queryModel().queryForList(sql,
            Map.of("minPrice", minPrice, "maxPrice", maxPrice),
            ProductEntity.class);
    }
}
```

### Step 6: Create Service Layer

**Service Interface:**
```java
public interface ProductService extends BaseService<ProductEntity, Long> {
    ProductResponse createOrUpdate(ProductRequest request) throws BusinessException;
    List<ProductResponse> findByCustomCriteria(String criteria);
    ProductResponse findResponseById(Long id) throws BusinessException;
    List<ProductResponse> findByCategoryId(Long categoryId);
}
```

**Service Implementation:**
```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends BaseServiceImpl<ProductEntity, Long> implements ProductService {
    private final ProductRepository repository;

    @Override
    protected JpaCommonRepository<ProductEntity, Long> getRepository() {
        return repository;
    }

    @Override
    public ProductResponse createOrUpdate(ProductRequest request) throws BusinessException {
        // Validation
        if (KanbanCommonUtil.isNullOrEmpty(request.getName())) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_IS_REQUIRED);
        }

        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_INVALID);
        }

        // Check for duplicates (for new products only)
        if (request.getId() == null && repository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS, request.getName());
        }

        // Mapping and save
        var entity = ProductRequestToEntityMapper.INSTANCE.mapTo(request);
        var result = save(entity);
        return ProductEntityToResponseMapper.INSTANCE.mapTo(result);
    }

    @Override
    public List<ProductResponse> findByCustomCriteria(String criteria) {
        var entities = repository.findByComplexCriteria(criteria);
        return ProductEntityToResponseMapper.INSTANCE.mapTo(entities);
    }

    @Override
    public ProductResponse findResponseById(Long id) throws BusinessException {
        var entity = findById(id).orElseThrow(() ->
            new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, id));
        return ProductEntityToResponseMapper.INSTANCE.mapTo(entity);
    }

    @Override
    public List<ProductResponse> findByCategoryId(Long categoryId) {
        var entities = repository.findByCategoryId(categoryId);
        return ProductEntityToResponseMapper.INSTANCE.mapTo(entities);
    }
}
```

### Step 7: Create Controller

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    @GetMapping
    public ResponseData<Page<ProductResponse>> findAll(PaginationRequest request) throws BusinessException {
        Page<ProductResponse> responses = service.findAll(request);
        return ResponseUtils.success(responses);
    }

    @GetMapping("/{id}")
    public ResponseData<ProductResponse> findById(@PathVariable Long id) throws BusinessException {
        ProductResponse response = service.findResponseById(id);
        return ResponseUtils.success(response);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseData<List<ProductResponse>> findByCategoryId(@PathVariable Long categoryId) throws BusinessException {
        List<ProductResponse> responses = service.findByCategoryId(categoryId);
        return ResponseUtils.success(responses);
    }



    @PostMapping
    public ResponseData<ProductResponse> create(@Valid @RequestBody ProductRequest request) throws BusinessException {
        ProductResponse response = service.createOrUpdate(request);
        return ResponseUtils.success(response);
    }

    @PutMapping("/{id}")
    public ResponseData<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) throws BusinessException {
        request.setId(id);
        ProductResponse response = service.createOrUpdate(request);
        return ResponseUtils.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> delete(@PathVariable Long id) throws BusinessException {
        service.deleteById(id);
        return ResponseUtils.success("OK");
    }
}
```

## Mandatory Coding Standards

### 1. Lombok Usage Rules

**✅ REQUIRED for DTOs:**
```java
@Data  // Always use for DTOs
public class ProductRequest {
    @NotBlank(message = "Name is required")
    private String name;
}
```

**✅ REQUIRED for Entities:**
```java
@Entity
@Data
@EqualsAndHashCode(callSuper = true)  // MANDATORY when extending BaseEntity
public class ProductEntity extends BaseEntity<Long> {
    // fields...
}
```

**✅ REQUIRED for Services/Controllers:**
```java
@Service
@RequiredArgsConstructor  // ONLY this annotation
public class ProductServiceImpl {
    private final ProductRepository repository;
    // NO @Data on service classes!
}
```

### 2. Exception Handling Rules

**✅ ALWAYS use ErrorCode enum:**
```java
// Correct
throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, productId);

// ❌ NEVER use raw strings
throw new RuntimeException("Product not found");
```

**✅ ALWAYS validate with KanbanCommonUtil:**
```java
if (KanbanCommonUtil.isNullOrEmpty(request.getName())) {
    throw new BusinessException(ErrorCode.PRODUCT_NAME_IS_REQUIRED);
}
```

### 3. Related Data Access Rules

**✅ CORRECT: Service layer coordination for related data**
```java
@Service
public class ProductServiceImpl {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductWithCategoryResponse getProductWithCategory(Long productId) {
        // Get product
        var product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, productId));

        // Get related category separately
        var category = categoryRepository.findById(product.getCategoryId())
            .orElse(null);

        // Combine in response DTO
        return ProductWithCategoryResponse.builder()
            .product(ProductEntityToResponseMapper.INSTANCE.mapTo(product))
            .category(category != null ? CategoryEntityToResponseMapper.INSTANCE.mapTo(category) : null)
            .build();
    }
}
```

**❌ FORBIDDEN: Entity relationships**
```java
// ❌ NEVER DO THIS
@Entity
public class ProductEntity {
    @ManyToOne  // FORBIDDEN!
    private CategoryEntity category;
}
```

## Common Patterns & Templates

### 1. Pagination Pattern
```java
@GetMapping
public ResponseData<Page<ProductResponse>> findAll(PaginationRequest request) throws BusinessException {
    Page<ProductResponse> responses = service.findAll(request);
    return ResponseUtils.success(responses);
}
```

### 2. Custom Query Pattern
```java
public List<ProductEntity> findByDateRange(LocalDate startDate, LocalDate endDate) {
    String sql = """
        SELECT * FROM products
        WHERE created_date BETWEEN :startDate AND :endDate
        ORDER BY created_date DESC
        """;

    return sqlQueryUtil.queryModel().queryForList(sql,
        Map.of("startDate", startDate, "endDate", endDate),
        ProductEntity.class);
}
```

### 3. Validation Pattern
```java
private void validateProductRequest(ProductRequest request) throws BusinessException {
    if (KanbanCommonUtil.isNullOrEmpty(request.getName())) {
        throw new BusinessException(ErrorCode.PRODUCT_NAME_IS_REQUIRED);
    }

    if (request.getPrice() == null || request.getPrice() <= 0) {
        throw new BusinessException(ErrorCode.PRODUCT_PRICE_INVALID);
    }
}
```

## Quick Troubleshooting

### Build Issues
```bash
# Clean and rebuild
mvn clean install

# Check Java version
java -version  # Should be 21

# Check Maven version
mvn -version   # Should be 3.6+
```

### Database Issues
```yaml
# Enable SQL logging in application.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.tool.hbm2ddl: DEBUG
```

### Common Fixes
1. **Mapper not found**: Run `mvn clean compile` to regenerate MapStruct mappers
2. **Bean creation failed**: Check `@RequiredArgsConstructor` and package scanning
3. **SQL errors**: Verify entity annotations and table names
4. **Validation errors**: Check `@Valid` annotation on controller methods

---

## 🚨 FINAL REMINDERS

1. **NO JPA RELATIONSHIPS** - Use service layer coordination instead
2. **ALWAYS use ErrorCode enum** for exceptions
3. **ALWAYS extend kanban-core base classes** when available
4. **ALWAYS use ResponseUtils.success()** in controllers
5. **ALWAYS validate with KanbanCommonUtil** before business logic
