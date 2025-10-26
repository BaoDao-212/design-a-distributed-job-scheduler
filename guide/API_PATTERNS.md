# API Patterns

## Response Structure

All API endpoints use `ResponseData<T>` wrapper from kanban-core library.

### Success Response Format
```json
{
  "success": true,
  "data": <actual_response_data>,
  "message": null,
  "errorCode": null,
  "timestamp": "2024-08-24T10:30:00Z"
}
```

### Error Response Format
```json
{
  "success": false,
  "data": null,
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-08-24T10:30:00Z"
}
```

## Controller Implementation Pattern

### Required Imports
```java
import vn.com.mbbank.kanban.ai_contest.utils.ResponseUtils;
import vn.com.mbbank.kanban.core.common.ResponseData;
import vn.com.mbbank.kanban.core.exceptions.BusinessException;
```

### Standard Controller Template
```java
@RestController
@RequestMapping("/api-endpoint")
@RequiredArgsConstructor
public class ExampleController {
    
    private final ExampleService service;
    
    @GetMapping
    public ResponseData<Page<ExampleResponse>> findAll(PaginationRequest request) throws BusinessException {
        Page<ExampleResponse> responses = service.findAll(request);
        return ResponseUtils.success(responses);
    }
    
    @GetMapping("/{id}")
    public ResponseData<ExampleResponse> findById(@PathVariable Long id) throws BusinessException {
        ExampleResponse response = service.findResponseById(id);
        return ResponseUtils.success(response);
    }
    
    @PostMapping
    public ResponseData<ExampleResponse> create(@Valid @RequestBody ExampleRequest request) 
            throws BusinessException {
        ExampleResponse response = service.createOrUpdate(request);
        return ResponseUtils.success(response);
    }
    
    @PutMapping("/{id}")
    public ResponseData<ExampleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExampleRequest request) throws BusinessException {
        request.setId(id);
        ExampleResponse response = service.createOrUpdate(request);
        return ResponseUtils.success(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseData<String> delete(@PathVariable Long id) throws BusinessException {
        service.deleteById(id);
        return ResponseUtils.success("OK");
    }
}
```

## Response Examples

### Single Entity Response
**Endpoint:** `GET /api/products/1`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Product 1",
    "price": 100,
    "description": "Product description",
    "createdAt": "2024-08-24T10:00:00Z",
    "updatedAt": "2024-08-24T10:00:00Z"
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2024-08-24T10:30:00Z"
}
```

### List Response
**Endpoint:** `GET /api/products`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Product 1",
      "price": 100
    },
    {
      "id": 2,
      "name": "Product 2", 
      "price": 200
    }
  ],
  "message": null,
  "errorCode": null,
  "timestamp": "2024-08-24T10:30:00Z"
}
```

### Paginated Response
**Endpoint:** `GET /api/products?page=0&size=10`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Product 1",
        "price": 100
      }
    ],
    "totalPages": 5,
    "totalElements": 50,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  },
  "message": null,
  "errorCode": null,
  "timestamp": "2024-08-24T10:30:00Z"
}
```

### Delete Response
**Endpoint:** `DELETE /api/products/1`
```json
{
  "success": true,
  "data": "OK",
  "message": null,
  "errorCode": null,
  "timestamp": "2024-08-24T10:30:00Z"
}
```

## Error Response Examples

### Validation Error
**Endpoint:** `POST /api/products` (with invalid data)
```json
{
  "success": false,
  "data": null,
  "message": "Product name is required",
  "errorCode": "PRODUCT_NAME_IS_REQUIRED",
  "timestamp": "2024-08-24T10:30:00Z"
}
```

### Not Found Error
**Endpoint:** `GET /api/products/999`
```json
{
  "success": false,
  "data": null,
  "message": "Product with ID 999 not found",
  "errorCode": "PRODUCT_NOT_FOUND",
  "timestamp": "2024-08-24T10:30:00Z"
}
```

## Mandatory Rules

### 1. Always Use ResponseUtils.success()
```java
// ✅ Correct
@GetMapping("/{id}")
public ResponseData<ProductResponse> getProduct(@PathVariable Long id) throws BusinessException {
    ProductResponse product = productService.findById(id);
    return ResponseUtils.success(product);
}

// ❌ FORBIDDEN
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) throws BusinessException {
    ProductResponse product = productService.findById(id);
    return ResponseEntity.ok(product);  // NEVER use ResponseEntity
}
```

### 2. Handle Void Operations Correctly
```java
// ✅ For delete operations
@DeleteMapping("/{id}")
public ResponseData<String> delete(@PathVariable Long id) throws BusinessException {
    service.deleteById(id);
    return ResponseUtils.success("OK");  // Return String, not Void
}

// ✅ For logout operations
@PostMapping("/logout")
public ResponseData<String> logout(@RequestHeader("Authorization") String authHeader)
        throws BusinessException {
    String token = authHeader.substring(7);
    authService.logout(token);
    return ResponseUtils.success("OK");
}
```

### 3. Consistent Error Handling
All business exceptions are automatically handled by kanban-core framework and converted to standard error response format.

```java
@PostMapping
public ResponseData<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) 
        throws BusinessException {
    // Validation errors automatically converted to error responses
    if (productService.existsByName(request.getName())) {
        throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS, request.getName());
    }
    
    ProductResponse response = productService.create(request);
    return ResponseUtils.success(response);
}
```

## Migration from ResponseEntity

**Before (FORBIDDEN):**
```java
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) throws BusinessException {
    ProductResponse product = productService.findById(id);
    return ResponseEntity.ok(product);
}
```

**After (REQUIRED):**
```java
@GetMapping("/{id}")
public ResponseData<ProductResponse> getProduct(@PathVariable Long id) throws BusinessException {
    ProductResponse product = productService.findById(id);
    return ResponseUtils.success(product);
}
```

---

**Key Rules:**
1. **NEVER use ResponseEntity** - Always use ResponseData<T>
2. **ALWAYS use ResponseUtils.success()** for all responses
3. **Return String "OK"** for void operations, not Void type
4. **Let kanban-core handle exceptions** - just throw BusinessException
