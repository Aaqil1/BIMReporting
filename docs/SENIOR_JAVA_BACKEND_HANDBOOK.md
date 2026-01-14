# Senior Java Backend Developer Technical Handbook
## Interview Preparation Guide for Publicis Sapient

**Target Audience:** Senior Java Backend Developer (7+ years experience)  
**Purpose:** Complete, interview-ready technical reference  
**Format:** Production-grade markdown, PDF-ready

---

# SECTION 1: CORE JAVA

## 1.1 Object-Oriented Programming (OOP) Principles

### Encapsulation
**Definition:** Bundling data and methods that operate on that data within a single unit (class), while restricting direct access to some components.

**Enterprise Example:**
```java
// Bad: Public fields expose internal state
public class Order {
    public BigDecimal total;
    public String status;
}

// Good: Encapsulated with controlled access
public class Order {
    private BigDecimal total;
    private OrderStatus status;
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void applyDiscount(BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }
        this.total = this.total.subtract(discount);
    }
}
```

**Why it matters:** Prevents invalid state mutations, enables validation, maintains invariants.

### Inheritance
**Definition:** Mechanism where a child class acquires properties and behaviors from a parent class.

**Enterprise Example:**
```java
// Base payment processor
public abstract class PaymentProcessor {
    protected PaymentGateway gateway;
    
    public abstract PaymentResult process(PaymentRequest request);
    
    protected void logTransaction(String transactionId) {
        // Common logging logic
    }
}

// Specific implementations
public class CreditCardProcessor extends PaymentProcessor {
    @Override
    public PaymentResult process(PaymentRequest request) {
        logTransaction(request.getTransactionId());
        return gateway.chargeCreditCard(request);
    }
}

public class PayPalProcessor extends PaymentProcessor {
    @Override
    public PaymentResult process(PaymentRequest request) {
        logTransaction(request.getTransactionId());
        return gateway.chargePayPal(request);
    }
}
```

### Polymorphism
**Definition:** Ability of objects of different types to be accessed through the same interface.

**Enterprise Example:**
```java
// Interface-based polymorphism
public interface NotificationService {
    void send(String recipient, String message);
}

@Service
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String recipient, String message) {
        // Email implementation
    }
}

@Service
public class SMSNotificationService implements NotificationService {
    @Override
    public void send(String recipient, String message) {
        // SMS implementation
    }
}

// Usage - polymorphic behavior
@Service
public class OrderService {
    private final List<NotificationService> notificationServices;
    
    public void notifyOrderCreated(Order order) {
        notificationServices.forEach(service -> 
            service.send(order.getCustomerEmail(), "Order created")
        );
    }
}
```

### Abstraction
**Definition:** Hiding complex implementation details and exposing only essential features.

**Enterprise Example:**
```java
// Abstraction through interface
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByCustomerId(Long customerId);
}

// Implementation details hidden
@Repository
public class JpaOrderRepository implements OrderRepository {
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public Order save(Order order) {
        // Complex JPA logic hidden
        return em.merge(order);
    }
}
```

## 1.2 SOLID Principles

### Single Responsibility Principle (SRP)
**Definition:** A class should have only one reason to change.

**Spring Boot Example:**
```java
// Violation: OrderService does too much
@Service
public class OrderService {
    public Order createOrder(OrderRequest request) { }
    public void sendEmail(Order order) { }
    public void generateInvoice(Order order) { }
    public void updateInventory(Order order) { }
}

// Correct: Separate responsibilities
@Service
public class OrderService {
    private final EmailService emailService;
    private final InvoiceService invoiceService;
    private final InventoryService inventoryService;
    
    public Order createOrder(OrderRequest request) {
        Order order = // create order logic
        emailService.sendOrderConfirmation(order);
        invoiceService.generate(order);
        inventoryService.update(order);
        return order;
    }
}
```

### Open/Closed Principle (OCP)
**Definition:** Software entities should be open for extension but closed for modification.

**Spring Boot Example:**
```java
// Strategy pattern in Spring
public interface PaymentStrategy {
    PaymentResult pay(BigDecimal amount);
}

@Component("creditCard")
public class CreditCardStrategy implements PaymentStrategy {
    @Override
    public PaymentResult pay(BigDecimal amount) {
        // Credit card logic
    }
}

@Component("paypal")
public class PayPalStrategy implements PaymentStrategy {
    @Override
    public PaymentResult pay(BigDecimal amount) {
        // PayPal logic
    }
}

@Service
public class PaymentService {
    private final Map<String, PaymentStrategy> strategies;
    
    public PaymentService(List<PaymentStrategy> strategies) {
        this.strategies = strategies.stream()
            .collect(Collectors.toMap(
                s -> s.getClass().getAnnotation(Component.class).value(),
                Function.identity()
            ));
    }
    
    public PaymentResult processPayment(String method, BigDecimal amount) {
        return strategies.get(method).pay(amount);
    }
}
```

### Liskov Substitution Principle (LSP)
**Definition:** Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.

**Example:**
```java
// Violation
public class Rectangle {
    protected int width;
    protected int height;
    
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width; // Breaks LSP
    }
}

// Correct: Use composition or interface
public interface Shape {
    int getArea();
}

public class Rectangle implements Shape {
    private int width, height;
    public int getArea() { return width * height; }
}

public class Square implements Shape {
    private int side;
    public int getArea() { return side * side; }
}
```

### Interface Segregation Principle (ISP)
**Definition:** Clients should not be forced to depend on interfaces they don't use.

**Example:**
```java
// Violation: Fat interface
public interface Worker {
    void work();
    void eat();
    void sleep();
}

// Correct: Segregated interfaces
public interface Workable {
    void work();
}

public interface Eatable {
    void eat();
}

public interface Sleepable {
    void sleep();
}

// Classes implement only what they need
public class Human implements Workable, Eatable, Sleepable {
    // implements all
}

public class Robot implements Workable {
    // only implements work
}
```

### Dependency Inversion Principle (DIP)
**Definition:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

**Spring Boot Example:**
```java
// Violation: Direct dependency on concrete class
@Service
public class OrderService {
    private final JpaOrderRepository repository = new JpaOrderRepository();
}

// Correct: Depend on abstraction
@Service
public class OrderService {
    private final OrderRepository repository; // Interface
    
    public OrderService(OrderRepository repository) {
        this.repository = repository; // Spring injects implementation
    }
}
```

## 1.3 Immutability

### What is Immutability?
An object whose state cannot be modified after creation.

### Benefits
1. **Thread Safety:** Immutable objects are inherently thread-safe
2. **No Side Effects:** Predictable behavior
3. **Cacheable:** Safe to cache and reuse
4. **Simpler Reasoning:** Easier to understand and debug

### Creating Immutable Classes
```java
// 1. Make class final
public final class ImmutableOrder {
    // 2. Make all fields final and private
    private final Long id;
    private final BigDecimal total;
    private final List<OrderItem> items; // Mutable collection!
    
    // 3. Initialize via constructor
    public ImmutableOrder(Long id, BigDecimal total, List<OrderItem> items) {
        this.id = id;
        this.total = total;
        // 4. Defensive copy for mutable collections
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }
    
    // 5. Only getters, no setters
    public Long getId() { return id; }
    public BigDecimal getTotal() { return total; }
    public List<OrderItem> getItems() { 
        // Return defensive copy
        return Collections.unmodifiableList(items);
    }
}
```

### Records (Java 14+)
```java
// Immutable by default
public record OrderRecord(
    Long id,
    BigDecimal total,
    List<OrderItem> items
) {
    // Compact constructor for validation
    public OrderRecord {
        if (id == null || total == null) {
            throw new IllegalArgumentException();
        }
        // Defensive copy
        items = List.copyOf(items);
    }
}
```

## 1.4 equals() and hashCode() Contract

### The Contract
1. If `a.equals(b)` returns `true`, then `a.hashCode()` must equal `b.hashCode()`
2. If `a.hashCode() == b.hashCode()`, `a.equals(b)` may or may not be `true`
3. If `a.equals(b)` is `true`, then `a.equals(b)` must always be `true` (consistency)
4. `a.equals(null)` must return `false`

### Why Both Are Required
```java
public class Order {
    private Long id;
    private String orderNumber;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id) &&
               Objects.equals(orderNumber, order.orderNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, orderNumber);
    }
}
```

### HashMap Behavior
```java
Map<Order, String> map = new HashMap<>();
Order order1 = new Order(1L, "ORD-001");
Order order2 = new Order(1L, "ORD-001");

map.put(order1, "value1");

// If hashCode() not implemented correctly:
// order2 won't find the bucket, returns null
// If equals() not implemented correctly:
// order2 finds bucket but wrong entry returned

String value = map.get(order2); // Must return "value1"
```

### Best Practices
1. Use `Objects.equals()` and `Objects.hash()` (Java 7+)
2. Include all fields used in `equals()` in `hashCode()`
3. Use `@EqualsAndHashCode` (Lombok) for boilerplate
4. For JPA entities, use database ID in equals/hashCode

```java
@Entity
public class Order {
    @Id
    @GeneratedValue
    private Long id;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id != null && id.equals(order.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode(); // Consistent for all instances
    }
}
```

## 1.5 static vs instance

### static Members
- Belong to the class, not instances
- Shared across all instances
- Loaded when class is first loaded
- Cannot access instance members directly

```java
public class OrderService {
    private static int totalOrders = 0; // Class-level
    private String serviceName; // Instance-level
    
    public static int getTotalOrders() {
        // Cannot access serviceName here
        return totalOrders;
    }
    
    public void processOrder() {
        totalOrders++; // Can access static
        this.serviceName = "OrderService"; // Can access instance
    }
}
```

### When to Use static
1. **Utility methods:** `Math.max()`, `Collections.sort()`
2. **Constants:** `public static final String API_VERSION = "v1"`
3. **Factory methods:** `LocalDate.now()`
4. **Shared state:** Counters, caches (with thread-safety)

### Instance Members
- Belong to specific object instances
- Each instance has its own copy
- Can access both static and instance members

## 1.6 JVM Memory Model

### Memory Areas

```
┌─────────────────────────────────────────────────────────┐
│                    JVM Memory Model                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Method Area (Metaspace)              │  │
│  │  - Class metadata                                 │  │
│  │  - Static variables                               │  │
│  │  - Method bytecode                                │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │                    Heap                            │  │
│  │  ┌──────────────────────────────────────────────┐ │  │
│  │  │         Young Generation                     │ │  │
│  │  │  ┌──────────────┐  ┌────────────────────┐ │ │  │
│  │  │  │   Eden       │  │  Survivor (S0/S1)   │ │ │  │
│  │  │  └──────────────┘  └────────────────────┘ │ │  │
│  │  └──────────────────────────────────────────────┘ │  │
│  │  ┌──────────────────────────────────────────────┐ │  │
│  │  │         Old Generation (Tenured)              │ │  │
│  │  └──────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Stack (Per Thread)                   │  │
│  │  - Local variables                                │  │
│  │  - Method parameters                              │  │
│  │  - Return addresses                               │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Program Counter (Per Thread)         │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │              Native Method Stack                  │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Heap Structure
- **Young Generation:**
  - **Eden:** New objects allocated here
  - **Survivor Spaces (S0, S1):** Objects that survive minor GC
  
- **Old Generation:**
  - Long-lived objects
  - Objects promoted from Young Generation after surviving multiple GC cycles

### Stack
- Each thread has its own stack
- Stores local variables, method parameters, return addresses
- Stack overflow occurs when stack size exceeds limit

### Method Area (Metaspace in Java 8+)
- Stores class metadata, static variables, method bytecode
- Shared across all threads

## 1.7 Garbage Collection

### GC Algorithms

#### Serial GC
- Single-threaded
- Stop-the-world pauses
- Suitable for small applications

#### Parallel GC
- Multiple threads for GC
- Default in Java 8
- Better throughput

#### G1 GC (Garbage First)
- Default in Java 11+
- Low latency goal
- Divides heap into regions
- Concurrent marking

#### ZGC / Shenandoah
- Ultra-low latency (sub-10ms)
- Concurrent collection
- For large heaps (multi-GB)

### GC Process

```
┌─────────────────────────────────────────────────────────┐
│              Garbage Collection Lifecycle                │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Object Allocation (Eden)                           │
│     ┌────────┐                                          │
│     │ Object │                                          │
│     └────────┘                                          │
│                                                          │
│  2. Minor GC (Eden → Survivor)                          │
│     ┌────────┐      ┌──────────┐                        │
│     │ Live   │ ───> │ Survivor │                        │
│     └────────┘      └──────────┘                        │
│     ┌────────┐                                          │
│     │ Dead   │ ───> [Collected]                        │
│     └────────┘                                          │
│                                                          │
│  3. Promotion (Survivor → Old Generation)              │
│     ┌──────────┐      ┌──────────────┐                   │
│     │ Survivor │ ───> │ Old Gen      │                   │
│     └──────────┘      └──────────────┘                   │
│                                                          │
│  4. Major GC (Old Generation)                            │
│     ┌──────────────┐                                    │
│     │ Old Gen      │ ───> [Collected]                  │
│     └──────────────┘                                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### GC Tuning Flags
```bash
# Heap size
-Xms2g -Xmx4g

# GC algorithm
-XX:+UseG1GC

# GC logging
-Xlog:gc*:file=gc.log:time,level,tags

# Heap dump on OOM
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dump.hprof
```

## 1.8 Performance Troubleshooting

### Common Issues

#### 1. Memory Leaks
```java
// Leak: Static collection grows indefinitely
private static List<Order> orders = new ArrayList<>();

// Fix: Use WeakReference or bounded collection
private static Map<Long, WeakReference<Order>> orderCache = new ConcurrentHashMap<>();
```

#### 2. Excessive Object Creation
```java
// Bad: Creates new StringBuilder in loop
String result = "";
for (String item : items) {
    result += item; // Creates new String each iteration
}

// Good: Reuse StringBuilder
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item);
}
String result = sb.toString();
```

#### 3. Inefficient Collections
```java
// Bad: O(n) lookup
List<Order> orders = new ArrayList<>();
orders.stream().filter(o -> o.getId().equals(id)).findFirst();

// Good: O(1) lookup
Map<Long, Order> orderMap = new HashMap<>();
orderMap.get(id);
```

### Profiling Tools
1. **JVisualVM:** Built-in profiler
2. **JProfiler:** Commercial profiler
3. **YourKit:** Commercial profiler
4. **Java Flight Recorder (JFR):** Built-in in JDK 11+

### Performance Best Practices
1. Use appropriate data structures
2. Avoid premature optimization
3. Profile before optimizing
4. Use connection pooling
5. Cache frequently accessed data
6. Use lazy initialization where appropriate

## 1.9 Interview Questions & Answers

### Q1: Explain the difference between == and equals()
**Answer:**
- `==` compares references (memory addresses)
- `equals()` compares object content (when overridden)

```java
String s1 = new String("hello");
String s2 = new String("hello");
String s3 = s1;

System.out.println(s1 == s2);        // false (different references)
System.out.println(s1.equals(s2));  // true (same content)
System.out.println(s1 == s3);       // true (same reference)
```

### Q2: Why is String immutable in Java?
**Answer:**
1. **Security:** Prevents modification of sensitive data (passwords, URLs)
2. **Thread Safety:** Can be shared across threads without synchronization
3. **Caching:** String pool allows reuse
4. **HashCode Caching:** HashCode calculated once and cached

### Q3: What is the difference between final, finally, and finalize?
**Answer:**
- **final:** Keyword for immutable variables, methods that can't be overridden, classes that can't be extended
- **finally:** Block that always executes (unless JVM exits)
- **finalize():** Deprecated method called by GC before object destruction (unreliable, avoid)

### Q4: Explain pass-by-value vs pass-by-reference
**Answer:**
Java is **always pass-by-value**, but for objects, the value is the reference:

```java
public void modify(List<String> list) {
    list.add("new"); // Modifies original list (reference passed)
    list = new ArrayList<>(); // Doesn't affect original (reference reassigned)
}
```

### Q5: What happens when you call System.gc()?
**Answer:**
- It's a **hint** to JVM, not a command
- JVM may or may not run GC
- Generally should not be called manually
- May cause performance issues if called frequently

---

# SECTION 2: COLLECTIONS & JAVA 8+

## 2.1 HashMap Internals

### Structure

```
┌─────────────────────────────────────────────────────────┐
│                    HashMap Internal Structure            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Bucket Array (Node<K,V>[])                             │
│  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐          │
│  │ [0] │  │ [1] │  │ [2] │  │ [3] │  │ [4] │  ...     │
│  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘          │
│     │        │        │        │        │              │
│     │        │        │        │        │              │
│     ▼        ▼        ▼        ▼        ▼              │
│  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐          │
│  │Node │  │Node │  │Node │  │Node │  │Node │          │
│  │key  │  │key  │  │key  │  │key  │  │key  │          │
│  │val  │  │val  │  │val  │  │val  │  │val  │          │
│  │next │  │next │  │next │  │next │  │next │          │
│  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘          │
│     │        │        │        │        │              │
│     ▼        ▼        ▼        ▼        ▼              │
│  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐          │
│  │Node │  │Node │  │Node │  │Node │  │Node │          │
│  └─────┘  └─────┘  └─────┘  └─────┘  └─────┘          │
│                                                          │
│  Tree Structure (when threshold exceeded)               │
│  ┌──────────────────────────────────────────┐          │
│  │         TreeNode (Red-Black Tree)        │          │
│  │              (Java 8+)                   │          │
│  └──────────────────────────────────────────┘          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Key Concepts

#### 1. Hash Function
```java
// HashMap uses key's hashCode()
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}

// Bucket index calculation
int index = (n - 1) & hash; // n = array length (power of 2)
```

#### 2. Collision Handling
- **Chaining:** Multiple entries in same bucket form a linked list
- **Treeification (Java 8+):** When list length > 8, converts to Red-Black Tree

#### 3. Load Factor & Resizing
```java
// Default values
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final int DEFAULT_INITIAL_CAPACITY = 16;
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;

// Resize when: size > capacity * loadFactor
// New capacity = old capacity * 2
```

### Complexity Analysis

| Operation | Average | Worst Case |
|-----------|---------|------------|
| get()     | O(1)    | O(log n)   |
| put()     | O(1)    | O(log n)   |
| remove()  | O(1)    | O(log n)   |
| contains()| O(1)    | O(log n)   |

**Worst case:** When all keys hash to same bucket (poor hashCode implementation)

### Common Issues

#### Poor hashCode Implementation
```java
// Bad: All objects return same hashCode
@Override
public int hashCode() {
    return 1; // All objects in same bucket!
}

// Good: Distributes evenly
@Override
public int hashCode() {
    return Objects.hash(id, name, email);
}
```

#### Mutable Keys
```java
Map<Order, String> map = new HashMap<>();
Order order = new Order(1L, "ORD-001");
map.put(order, "value");

order.setId(2L); // Modifies key!

// Now map.get(order) returns null
// Because hashCode changed, bucket index changed
```

## 2.2 ConcurrentHashMap Internals

### Differences from HashMap

1. **Thread-Safe:** Multiple threads can read/write concurrently
2. **No Locking for Reads:** Uses volatile reads
3. **Segment-Based (Java 7):** Divided into segments, each with its own lock
4. **Node-Based (Java 8+):** Uses synchronized blocks on individual buckets

### Java 8+ Implementation

```java
// ConcurrentHashMap structure
┌─────────────────────────────────────────────────────────┐
│              ConcurrentHashMap (Java 8+)                 │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Node Array (volatile Node<K,V>[])                      │
│  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐                    │
│  │ [0] │  │ [1] │  │ [2] │  │ [3] │  ...              │
│  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘                    │
│     │        │        │        │                        │
│     ▼        ▼        ▼        ▼                        │
│  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐                    │
│  │Node │  │Node │  │Node │  │Node │                    │
│  │(vol)│  │(vol)│  │(vol)│  │(vol)│                    │
│  └─────┘  └─────┘  └─────┘  └─────┘                    │
│                                                          │
│  Synchronization: Only on specific bucket during write  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Key Methods

```java
// Thread-safe operations
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// putIfAbsent - atomic check-and-set
map.putIfAbsent("key", 1);

// compute - atomic update
map.compute("key", (k, v) -> v == null ? 1 : v + 1);

// merge - atomic merge
map.merge("key", 1, Integer::sum);
```

### Performance Characteristics

- **Reads:** No locking, very fast (volatile reads)
- **Writes:** Minimal locking (only affected bucket)
- **Scalability:** Better than synchronized HashMap for high concurrency

## 2.3 fail-fast vs fail-safe Iterators

### fail-fast
**Definition:** Iterator throws `ConcurrentModificationException` if collection is modified during iteration.

**Collections:**
- `ArrayList`, `HashMap`, `HashSet`, `Vector`

```java
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");

// This will throw ConcurrentModificationException
for (String item : list) {
    list.add("c"); // Modification during iteration
}
```

**How it works:**
- Iterator maintains `modCount` (modification count)
- On each operation, checks if `modCount` changed
- If changed, throws `ConcurrentModificationException`

### fail-safe
**Definition:** Iterator works on snapshot, doesn't throw exception on modification.

**Collections:**
- `ConcurrentHashMap`, `CopyOnWriteArrayList`

```java
List<String> list = new CopyOnWriteArrayList<>();
list.add("a");
list.add("b");

// This won't throw exception
for (String item : list) {
    list.add("c"); // Safe - works on snapshot
}
```

**Trade-off:**
- **fail-fast:** Fast, but can't modify during iteration
- **fail-safe:** Safe, but may not see latest changes

## 2.4 Comparable vs Comparator

### Comparable
**Definition:** Interface for natural ordering. Class implements it to define default sort order.

```java
public class Order implements Comparable<Order> {
    private Long id;
    private BigDecimal total;
    private LocalDateTime createdAt;
    
    @Override
    public int compareTo(Order other) {
        // Natural order: by creation date (newest first)
        return other.createdAt.compareTo(this.createdAt);
    }
}

// Usage
List<Order> orders = new ArrayList<>();
Collections.sort(orders); // Uses compareTo()
```

### Comparator
**Definition:** External comparison logic. Can define multiple sorting strategies.

```java
// Multiple comparators for different sorting needs
Comparator<Order> byTotal = Comparator.comparing(Order::getTotal);
Comparator<Order> byDate = Comparator.comparing(Order::getCreatedAt);
Comparator<Order> byTotalThenDate = byTotal.thenComparing(byDate);

// Usage
orders.sort(byTotal);
orders.sort(byTotal.reversed()); // Descending
orders.sort(byTotalThenDate); // Multiple criteria
```

### When to Use Which

- **Comparable:** When there's one natural/default ordering
- **Comparator:** When you need multiple sorting strategies or can't modify the class

## 2.5 Streams API

### What are Streams?
**Definition:** Sequence of elements supporting functional-style operations (map, filter, reduce).

**Characteristics:**
1. **Not a data structure:** Doesn't store elements
2. **Functional:** Operations don't modify source
3. **Lazy:** Operations executed only when terminal operation called
4. **Pipelined:** Operations can be chained

### Common Operations

#### Intermediate Operations (Lazy)
```java
List<Order> orders = // ... 

// filter - Predicate
List<Order> activeOrders = orders.stream()
    .filter(o -> o.getStatus() == OrderStatus.ACTIVE)
    .collect(Collectors.toList());

// map - Transform
List<Long> orderIds = orders.stream()
    .map(Order::getId)
    .collect(Collectors.toList());

// flatMap - Flatten nested collections
List<OrderItem> allItems = orders.stream()
    .flatMap(order -> order.getItems().stream())
    .collect(Collectors.toList());

// distinct - Remove duplicates
List<String> uniqueEmails = orders.stream()
    .map(Order::getCustomerEmail)
    .distinct()
    .collect(Collectors.toList());

// sorted - Sort
List<Order> sortedByTotal = orders.stream()
    .sorted(Comparator.comparing(Order::getTotal).reversed())
    .collect(Collectors.toList());

// limit / skip - Pagination
List<Order> first10 = orders.stream()
    .skip(0)
    .limit(10)
    .collect(Collectors.toList());
```

#### Terminal Operations (Eager)
```java
// collect - Convert to collection
List<Order> list = orders.stream().collect(Collectors.toList());
Set<Order> set = orders.stream().collect(Collectors.toSet());
Map<Long, Order> map = orders.stream()
    .collect(Collectors.toMap(Order::getId, Function.identity()));

// reduce - Aggregate
BigDecimal total = orders.stream()
    .map(Order::getTotal)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// forEach - Side effect
orders.stream().forEach(order -> System.out.println(order));

// count - Count elements
long count = orders.stream().count();

// anyMatch / allMatch / noneMatch - Predicate checks
boolean hasActive = orders.stream()
    .anyMatch(o -> o.getStatus() == OrderStatus.ACTIVE);

// findFirst / findAny - Get element
Optional<Order> first = orders.stream().findFirst();
```

### Advanced Stream Operations

#### Grouping
```java
// Group by status
Map<OrderStatus, List<Order>> byStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus));

// Group by status with count
Map<OrderStatus, Long> countByStatus = orders.stream()
    .collect(Collectors.groupingBy(
        Order::getStatus,
        Collectors.counting()
    ));

// Group by status with sum
Map<OrderStatus, BigDecimal> totalByStatus = orders.stream()
    .collect(Collectors.groupingBy(
        Order::getStatus,
        Collectors.reducing(
            BigDecimal.ZERO,
            Order::getTotal,
            BigDecimal::add
        )
    ));
```

#### Partitioning
```java
// Partition into two groups (true/false)
Map<Boolean, List<Order>> partitioned = orders.stream()
    .collect(Collectors.partitioningBy(
        o -> o.getTotal().compareTo(new BigDecimal("100")) > 0
    ));
```

#### Joining
```java
// Join strings
String orderIds = orders.stream()
    .map(o -> o.getId().toString())
    .collect(Collectors.joining(", ", "[", "]"));
// Result: "[1, 2, 3]"
```

### Parallel Streams
```java
// Parallel processing
List<Order> processed = orders.parallelStream()
    .filter(o -> o.getTotal().compareTo(BigDecimal.valueOf(100)) > 0)
    .collect(Collectors.toList());

// When to use:
// - Large datasets
// - CPU-intensive operations
// - Operations are stateless and independent
```

**Caveats:**
- Overhead for small datasets
- Thread safety required
- Order not guaranteed (unless using `forEachOrdered`)

## 2.6 Optional

### Purpose
**Definition:** Container object that may or may not contain a non-null value. Prevents `NullPointerException`.

### Creation
```java
// Empty Optional
Optional<String> empty = Optional.empty();

// Optional with value
Optional<String> value = Optional.of("hello");

// Optional that may be null
Optional<String> nullable = Optional.ofNullable(getString()); // Returns null-safe Optional
```

### Common Operations
```java
Optional<Order> order = orderRepository.findById(id);

// isPresent / isEmpty
if (order.isPresent()) {
    Order o = order.get();
}

// ifPresent - Execute if value exists
order.ifPresent(o -> System.out.println(o.getTotal()));

// orElse - Default value
Order defaultOrder = order.orElse(new Order());

// orElseGet - Lazy default (supplier)
Order defaultOrder = order.orElseGet(() -> createDefaultOrder());

// orElseThrow - Throw exception if empty
Order order = orderRepository.findById(id)
    .orElseThrow(() -> new OrderNotFoundException(id));

// map - Transform if present
Optional<BigDecimal> total = order.map(Order::getTotal);

// flatMap - Flatten nested Optionals
Optional<String> email = order
    .flatMap(Order::getCustomer)
    .map(Customer::getEmail);

// filter - Filter if present
Optional<Order> activeOrder = order
    .filter(o -> o.getStatus() == OrderStatus.ACTIVE);
```

### Best Practices
```java
// Bad: Using Optional.get() without check
Order o = order.get(); // Throws NoSuchElementException if empty

// Good: Use orElse/orElseThrow
Order o = order.orElseThrow(() -> new OrderNotFoundException());

// Bad: Using Optional for method parameters
public void process(Optional<String> name) { } // Don't do this

// Good: Use null or overloaded methods
public void process(String name) { }
public void process() { process(null); }
```

## 2.7 Date & Time API (Java 8+)

### Problems with Old API
- `java.util.Date` is mutable
- Thread-unsafe
- Poor API design
- No timezone handling

### New API Overview

```
┌─────────────────────────────────────────────────────────┐
│              Java 8+ Date/Time API Hierarchy             │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  LocalDate          - Date without time                  │
│  LocalTime          - Time without date                  │
│  LocalDateTime      - Date and time (no timezone)       │
│  ZonedDateTime      - Date and time with timezone       │
│  Instant            - Point in time (UTC)                 │
│  Duration           - Time-based amount (hours, mins)   │
│  Period             - Date-based amount (days, months)   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Common Operations
```java
// Current date/time
LocalDate today = LocalDate.now();
LocalTime now = LocalTime.now();
LocalDateTime dateTime = LocalDateTime.now();
Instant instant = Instant.now();

// Create specific date/time
LocalDate date = LocalDate.of(2024, 1, 15);
LocalTime time = LocalTime.of(14, 30);
LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 14, 30);

// Parsing
LocalDate parsed = LocalDate.parse("2024-01-15");
LocalDateTime parsedDt = LocalDateTime.parse("2024-01-15T14:30:00");

// Formatting
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
String formatted = date.format(formatter);

// Manipulation (immutable - returns new instance)
LocalDate tomorrow = today.plusDays(1);
LocalDate nextMonth = today.plusMonths(1);
LocalDate lastYear = today.minusYears(1);

// Comparison
boolean isAfter = today.isAfter(LocalDate.of(2024, 1, 1));
boolean isBefore = today.isBefore(LocalDate.of(2024, 12, 31));

// Duration and Period
Duration duration = Duration.between(startTime, endTime);
Period period = Period.between(startDate, endDate);

// Timezone
ZonedDateTime zoned = ZonedDateTime.now(ZoneId.of("America/New_York"));
Instant utc = zoned.toInstant();
```

### JPA Integration
```java
@Entity
public class Order {
    @Column
    private LocalDate orderDate;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private Instant updatedAt; // Stored as TIMESTAMP
}
```

## 2.8 Interview Questions & Answers

### Q1: How does HashMap handle collisions?
**Answer:**
1. **Chaining:** Multiple entries in same bucket form linked list
2. **Treeification (Java 8+):** When list length > 8, converts to Red-Black Tree
3. **Bucket index:** Calculated as `(n-1) & hash(key)`

### Q2: Why is ConcurrentHashMap better than synchronized HashMap?
**Answer:**
- **ConcurrentHashMap:** Locks only specific bucket during write, allows concurrent reads
- **synchronized HashMap:** Locks entire map for any operation
- **Result:** Much better performance under high concurrency

### Q3: Explain the difference between map() and flatMap()
**Answer:**
- **map():** Transforms each element (1:1 mapping)
- **flatMap():** Transforms and flattens nested collections (1:many mapping)

```java
List<List<String>> nested = Arrays.asList(
    Arrays.asList("a", "b"),
    Arrays.asList("c", "d")
);

// map: List<List<String>> -> List<Stream<String>>
nested.stream().map(List::stream);

// flatMap: List<List<String>> -> List<String>
nested.stream().flatMap(List::stream).collect(Collectors.toList());
// Result: ["a", "b", "c", "d"]
```

### Q4: When should you use Optional?
**Answer:**
- Return type when value may be absent
- Chain of nullable operations
- **Don't use:** Method parameters, fields, in collections

### Q5: What is the difference between fail-fast and fail-safe iterators?
**Answer:**
- **fail-fast:** Throws `ConcurrentModificationException` on modification during iteration (ArrayList, HashMap)
- **fail-safe:** Works on snapshot, doesn't throw exception (ConcurrentHashMap, CopyOnWriteArrayList)

---

# SECTION 3: MULTITHREADING & CONCURRENCY

## 3.1 Thread Lifecycle

### States

```
┌─────────────────────────────────────────────────────────┐
│                  Thread Lifecycle States                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  NEW ──────> RUNNABLE ──────> RUNNING                   │
│   │             │                    │                  │
│   │             │                    │                  │
│   │             ▼                    ▼                  │
│   │         BLOCKED              WAITING               │
│   │             │                    │                  │
│   │             │                    │                  │
│   │             └─────────> TIMED_WAITING              │
│   │                           │                         │
│   │                           │                         │
│   └───────────────────────────┴─────────> TERMINATED   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### State Descriptions

1. **NEW:** Thread created but not started
2. **RUNNABLE:** Thread is ready to run, waiting for CPU
3. **RUNNING:** Thread is executing
4. **BLOCKED:** Thread waiting for monitor lock (synchronized)
5. **WAITING:** Thread waiting indefinitely (wait(), join())
6. **TIMED_WAITING:** Thread waiting with timeout (sleep(), wait(timeout))
7. **TERMINATED:** Thread has completed execution

### Code Example
```java
Thread thread = new Thread(() -> {
    System.out.println("Running");
});

System.out.println(thread.getState()); // NEW

thread.start();
System.out.println(thread.getState()); // RUNNABLE or RUNNING

thread.join();
System.out.println(thread.getState()); // TERMINATED
```

## 3.2 synchronized vs Lock

### synchronized Keyword

**Definition:** Built-in Java mechanism for mutual exclusion and thread synchronization.

**Forms:**
1. **Synchronized method:** Locks on object instance (instance method) or class (static method)
2. **Synchronized block:** Locks on specific object

```java
// Synchronized method
public synchronized void increment() {
    count++;
}

// Synchronized block
public void increment() {
    synchronized (this) {
        count++;
    }
}

// Synchronized on different object
private final Object lock = new Object();
public void increment() {
    synchronized (lock) {
        count++;
    }
}
```

**Characteristics:**
- Automatic lock acquisition/release
- Reentrant (same thread can acquire lock multiple times)
- No timeout support
- No interrupt support while waiting

### Lock Interface (java.util.concurrent.locks)

**Definition:** More flexible locking mechanism than synchronized.

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Counter {
    private final Lock lock = new ReentrantLock();
    private int count = 0;
    
    public void increment() {
        lock.lock(); // Must manually unlock
        try {
            count++;
        } finally {
            lock.unlock(); // Always in finally
        }
    }
    
    // With timeout
    public boolean tryIncrement() {
        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    count++;
                    return true;
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
```

**Advantages over synchronized:**
- **Timeout support:** `tryLock(timeout)`
- **Interrupt support:** `lockInterruptibly()`
- **Fairness:** `new ReentrantLock(true)` for fair ordering
- **Multiple conditions:** `lock.newCondition()`

### ReadWriteLock

**Definition:** Allows multiple readers or single writer.

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, Object> cache = new HashMap<>();
    
    public Object get(String key) {
        lock.readLock().lock(); // Multiple readers allowed
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void put(String key, Object value) {
        lock.writeLock().lock(); // Exclusive write
        try {
            cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

## 3.3 volatile Keyword

### What is volatile?
**Definition:** Ensures variable visibility across threads and prevents compiler optimizations.

### Problems Without volatile

```java
// Without volatile - may never see update
public class SharedData {
    private boolean flag = false; // Not volatile
    
    public void setFlag() {
        flag = true; // May be cached in CPU register
    }
    
    public void checkFlag() {
        while (!flag) {
            // May loop forever - never sees flag = true
        }
    }
}
```

### Solution with volatile

```java
// With volatile - always sees latest value
public class SharedData {
    private volatile boolean flag = false;
    
    public void setFlag() {
        flag = true; // Immediately visible to all threads
    }
    
    public void checkFlag() {
        while (!flag) {
            // Will see flag = true when set
        }
    }
}
```

### What volatile Guarantees

1. **Visibility:** Changes to volatile variable are immediately visible to all threads
2. **Ordering:** Prevents reordering of operations around volatile access
3. **Happens-Before:** Establishes happens-before relationship

### Limitations

**volatile does NOT provide:**
- Atomicity for compound operations
- Mutual exclusion

```java
// WRONG: volatile doesn't make increment atomic
private volatile int count = 0;

public void increment() {
    count++; // NOT thread-safe! (read-modify-write)
}

// CORRECT: Use AtomicInteger
private final AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet(); // Thread-safe
}
```

## 3.4 Atomic Classes

### Purpose
**Definition:** Classes that provide atomic operations without synchronization.

### Common Atomic Classes

```java
import java.util.concurrent.atomic.*;

// AtomicInteger
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();        // Atomic increment
count.getAndIncrement();        // Get then increment
count.addAndGet(5);             // Atomic add
count.compareAndSet(0, 10);     // CAS operation

// AtomicLong
AtomicLong total = new AtomicLong(0L);
total.addAndGet(100L);

// AtomicReference
AtomicReference<String> ref = new AtomicReference<>("initial");
ref.compareAndSet("initial", "updated");

// AtomicBoolean
AtomicBoolean flag = new AtomicBoolean(false);
flag.compareAndSet(false, true);
```

### Compare-And-Swap (CAS)

**How it works:**
1. Read current value
2. Compare with expected value
3. If equal, update to new value
4. If not equal, retry

```java
// CAS implementation concept
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

**Advantages:**
- No blocking (lock-free)
- Better performance under contention
- No deadlock risk

**Disadvantages:**
- ABA problem (solved with versioning)
- May require retries (busy-waiting)

## 3.5 ExecutorService

### Purpose
**Definition:** Framework for asynchronous execution of tasks, managing thread lifecycle.

### Creating ExecutorService

```java
// Fixed thread pool
ExecutorService executor = Executors.newFixedThreadPool(10);

// Cached thread pool (creates threads as needed)
ExecutorService executor = Executors.newCachedThreadPool();

// Single thread executor
ExecutorService executor = Executors.newSingleThreadExecutor();

// Scheduled executor
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
```

### Submitting Tasks

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

// Submit Runnable (no return value)
Future<?> future1 = executor.submit(() -> {
    System.out.println("Task running");
});

// Submit Callable (returns value)
Future<String> future2 = executor.submit(() -> {
    return "Result";
});

// Execute (fire-and-forget)
executor.execute(() -> {
    System.out.println("Task executed");
});
```

### Getting Results

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Result";
});

// Blocking get
String result = future.get(); // Blocks until complete

// Get with timeout
String result = future.get(5, TimeUnit.SECONDS); // Throws TimeoutException

// Check if done
if (future.isDone()) {
    String result = future.get();
}

// Cancel
future.cancel(true); // true = interrupt if running
```

### Shutting Down

```java
// Graceful shutdown
executor.shutdown(); // Stops accepting new tasks
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // Force shutdown
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

## 3.6 Thread Pools

### Types

#### 1. FixedThreadPool
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
// Fixed number of threads
// Unbounded queue
// Suitable for CPU-bound tasks
```

#### 2. CachedThreadPool
```java
ExecutorService executor = Executors.newCachedThreadPool();
// Creates threads as needed
// Reuses idle threads
// Suitable for short-lived tasks
```

#### 3. SingleThreadExecutor
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
// Single thread
// Sequential execution
// Suitable for tasks requiring ordering
```

#### 4. ScheduledThreadPool
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

// Schedule once
scheduler.schedule(() -> System.out.println("Delayed"), 5, TimeUnit.SECONDS);

// Schedule periodically
scheduler.scheduleAtFixedRate(
    () -> System.out.println("Periodic"),
    0, 1, TimeUnit.SECONDS
);
```

### Custom ThreadPoolExecutor

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                          // corePoolSize
    10,                         // maximumPoolSize
    60L,                        // keepAliveTime
    TimeUnit.SECONDS,           // unit
    new LinkedBlockingQueue<>(100), // workQueue
    new ThreadFactory() {       // threadFactory
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("custom-thread-" + t.getId());
            return t;
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy() // rejectionHandler
);
```

### Rejection Policies

1. **AbortPolicy:** Throws `RejectedExecutionException` (default)
2. **CallerRunsPolicy:** Executes task in caller thread
3. **DiscardPolicy:** Silently discards task
4. **DiscardOldestPolicy:** Discards oldest task in queue

## 3.7 CompletableFuture

### Purpose
**Definition:** Represents asynchronous computation that can be completed in the future. Supports chaining and composition.

### Creating CompletableFuture

```java
// Completed future
CompletableFuture<String> future = CompletableFuture.completedFuture("Result");

// Supply async
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Result";
});

// Run async (no return value)
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Running");
});
```

### Chaining Operations

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(String::toUpperCase)
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + "!"));

String result = future.get(); // "HELLO WORLD!"
```

### Combining Futures

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");

// Combine both results
CompletableFuture<String> combined = future1.thenCombine(
    future2,
    (s1, s2) -> s1 + " " + s2
);

// Wait for both to complete
CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2);

// Get first completed
CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future1, future2);
```

### Error Handling

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) {
            throw new RuntimeException("Error");
        }
        return "Success";
    })
    .exceptionally(ex -> "Error: " + ex.getMessage())
    .handle((result, ex) -> {
        if (ex != null) {
            return "Handled: " + ex.getMessage();
        }
        return result;
    });
```

## 3.8 BlockingQueue

### Purpose
**Definition:** Thread-safe queue that blocks when trying to take from empty queue or put into full queue.

### Types

#### ArrayBlockingQueue
```java
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

// Put (blocks if full)
queue.put("item");

// Take (blocks if empty)
String item = queue.take();

// Offer (non-blocking, returns false if full)
boolean added = queue.offer("item");

// Poll (non-blocking, returns null if empty)
String item = queue.poll();
```

#### LinkedBlockingQueue
```java
BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);
// Similar API to ArrayBlockingQueue
// Unbounded if capacity not specified
```

#### PriorityBlockingQueue
```java
BlockingQueue<Order> queue = new PriorityBlockingQueue<>(
    10,
    Comparator.comparing(Order::getPriority)
);
// Orders by priority
```

#### SynchronousQueue
```java
BlockingQueue<String> queue = new SynchronousQueue<>();
// Each put must wait for take
// No capacity (zero capacity)
```

### Producer-Consumer Pattern

```
┌─────────────────────────────────────────────────────────┐
│            Producer-Consumer Pattern                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Producer Thread          BlockingQueue        Consumer │
│  ┌──────────────┐        ┌──────────────┐   Thread    │
│  │              │        │              │   ┌────────┐ │
│  │  put(item)   │ ─────> │   [item]     │   │ take() │ │
│  │              │        │              │   │        │ │
│  │              │        │   [item]     │   │        │ │
│  │              │        │              │   │        │ │
│  │              │        │   [item]     │   │        │ │
│  └──────────────┘        └──────────────┘   └────────┘ │
│                                                          │
│  - Blocks if queue full    - Thread-safe    - Blocks if │
│  - Non-blocking option      - FIFO order      queue empty│
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Example:**
```java
BlockingQueue<Order> orderQueue = new ArrayBlockingQueue<>(100);

// Producer
class OrderProducer implements Runnable {
    @Override
    public void run() {
        while (true) {
            Order order = createOrder();
            try {
                orderQueue.put(order); // Blocks if full
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

// Consumer
class OrderConsumer implements Runnable {
    @Override
    public void run() {
        while (true) {
            try {
                Order order = orderQueue.take(); // Blocks if empty
                processOrder(order);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

## 3.9 Deadlock, Livelock, Starvation

### Deadlock

**Definition:** Two or more threads blocked forever, each waiting for the other to release a lock.

**Conditions (all must be true):**
1. Mutual exclusion
2. Hold and wait
3. No preemption
4. Circular wait

**Example:**
```java
// Thread 1
synchronized (lock1) {
    synchronized (lock2) {
        // ...
    }
}

// Thread 2
synchronized (lock2) {
    synchronized (lock1) { // DEADLOCK!
        // ...
    }
}
```

**Prevention:**
1. **Lock ordering:** Always acquire locks in same order
2. **Timeout:** Use `tryLock(timeout)`
3. **Avoid nested locks:** Minimize lock scope

### Livelock

**Definition:** Threads are active but unable to make progress due to constantly changing state.

**Example:**
```java
// Two threads trying to pass through narrow corridor
// Each keeps stepping aside for the other
// Neither makes progress
```

**Solution:** Introduce randomness or backoff

### Starvation

**Definition:** Thread unable to gain access to shared resource due to other threads constantly accessing it.

**Causes:**
- Thread priority
- Unfair locking
- CPU-bound threads

**Solution:**
- Fair locks: `new ReentrantLock(true)`
- Thread pool with fair queuing

## 3.10 Interview Questions & Answers

### Q1: What is the difference between synchronized and volatile?
**Answer:**
- **synchronized:** Provides mutual exclusion (only one thread executes) AND visibility
- **volatile:** Provides only visibility, no mutual exclusion
- **Use synchronized:** When you need atomicity (compound operations)
- **Use volatile:** When you only need visibility (simple read/write)

### Q2: Explain the difference between wait() and sleep()
**Answer:**
- **wait():** Releases lock, must be called in synchronized block, can be interrupted
- **sleep():** Doesn't release lock, can be called anywhere, can be interrupted
- **wait():** Object method, wakes on notify()
- **sleep():** Thread method, wakes after timeout

### Q3: What is the difference between Runnable and Callable?
**Answer:**
- **Runnable:** `run()` returns void, cannot throw checked exception
- **Callable:** `call()` returns value, can throw checked exception
- **Use Runnable:** Fire-and-forget tasks
- **Use Callable:** Tasks that return results

### Q4: How does ThreadPoolExecutor handle task rejection?
**Answer:**
When queue is full and all threads are busy:
1. **AbortPolicy (default):** Throws `RejectedExecutionException`
2. **CallerRunsPolicy:** Executes in caller thread
3. **DiscardPolicy:** Silently discards
4. **DiscardOldestPolicy:** Removes oldest task

### Q5: What is the happens-before relationship?
**Answer:**
Guarantees that operations in one thread are visible to another thread:
- **synchronized:** Unlock happens-before subsequent lock
- **volatile:** Write happens-before subsequent read
- **Thread.start():** Code before start() happens-before code in thread
- **Thread.join():** Code in thread happens-before code after join()

---

# SECTION 4: SPRING & SPRING BOOT

## 4.1 Inversion of Control (IoC) & Dependency Injection (DI)

### What is IoC?
**Definition:** Framework controls object creation and lifecycle, rather than application code.

**Traditional Approach:**
```java
// Tight coupling - application controls creation
public class OrderService {
    private OrderRepository repository = new JpaOrderRepository(); // Direct creation
}
```

**IoC Approach:**
```java
// Loose coupling - framework controls creation
@Service
public class OrderService {
    private final OrderRepository repository;
    
    // Framework injects dependency
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

### Dependency Injection Types

#### 1. Constructor Injection (Recommended)
```java
@Service
public class OrderService {
    private final OrderRepository repository;
    private final EmailService emailService;
    
    // Constructor injection - Spring's preferred method
    public OrderService(OrderRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }
}
```

**Benefits:**
- Immutable dependencies (final fields)
- Required dependencies (fail-fast if missing)
- Easier testing
- No circular dependency issues

#### 2. Setter Injection
```java
@Service
public class OrderService {
    private OrderRepository repository;
    
    @Autowired
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Use when:**
- Optional dependencies
- Need to change dependencies at runtime

#### 3. Field Injection (Not Recommended)
```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository repository; // Avoid - harder to test
}
```

**Problems:**
- Can't make field final
- Harder to test (need reflection or Spring context)
- Hides dependencies

## 4.2 Bean Lifecycle

### Lifecycle Phases

```
┌─────────────────────────────────────────────────────────┐
│                  Spring Bean Lifecycle                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Instantiation                                       │
│     ┌──────────────┐                                    │
│     │ new Bean()   │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  2. Populate Properties                                 │
│     ┌──────────────┐                                    │
│     │ Set Fields  │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  3. BeanNameAware.setBeanName()                        │
│  4. BeanFactoryAware.setBeanFactory()                  │
│  5. ApplicationContextAware.setApplicationContext()    │
│            │                                            │
│            ▼                                            │
│  6. @PostConstruct                                      │
│     ┌──────────────┐                                    │
│     │ init-method  │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  7. Bean Ready (In Use)                                 │
│     ┌──────────────┐                                    │
│     │   Active    │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  8. @PreDestroy                                         │
│     ┌──────────────┐                                    │
│     │destroy-method│                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  9. Garbage Collection                                  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Lifecycle Callbacks

```java
@Component
public class OrderProcessor implements BeanNameAware, InitializingBean, DisposableBean {
    
    // 1. BeanNameAware
    @Override
    public void setBeanName(String name) {
        System.out.println("Bean name: " + name);
    }
    
    // 2. InitializingBean
    @Override
    public void afterPropertiesSet() {
        System.out.println("After properties set");
    }
    
    // 3. @PostConstruct (preferred over InitializingBean)
    @PostConstruct
    public void init() {
        System.out.println("PostConstruct");
    }
    
    // 4. @PreDestroy (preferred over DisposableBean)
    @PreDestroy
    public void cleanup() {
        System.out.println("PreDestroy");
    }
    
    // 5. DisposableBean
    @Override
    public void destroy() {
        System.out.println("Destroy");
    }
}
```

### Bean Scopes

```java
// Singleton (default) - One instance per container
@Component
@Scope("singleton")
public class OrderService { }

// Prototype - New instance each time
@Component
@Scope("prototype")
public class OrderProcessor { }

// Request - One per HTTP request (web context)
@Component
@Scope("request")
public class RequestScopedBean { }

// Session - One per HTTP session (web context)
@Component
@Scope("session")
public class SessionScopedBean { }
```

## 4.3 @Component vs @Service vs @Repository

### Purpose
All are stereotypes that mark classes as Spring-managed beans. They're functionally equivalent but convey semantic meaning.

### Differences

```java
// @Component - Generic Spring component
@Component
public class OrderValidator {
    // Generic component
}

// @Service - Business logic layer
@Service
public class OrderService {
    // Business service
}

// @Repository - Data access layer
@Repository
public class OrderRepository {
    // Data access - enables exception translation
    // SQLException -> DataAccessException
}

// @Controller - Web layer (MVC)
@Controller
public class OrderController {
    // Web controller
}
```

### Why Different Annotations?

1. **Semantic Clarity:** Code intent is clear
2. **AOP Pointcuts:** Can target specific layers
3. **Exception Translation:** `@Repository` enables SQLException translation
4. **Future Extensions:** Framework can add layer-specific behavior

## 4.4 @Bean vs @Component

### @Component
**Definition:** Class-level annotation. Spring creates instance automatically.

```java
@Component
public class OrderService {
    // Spring creates instance
}
```

### @Bean
**Definition:** Method-level annotation in `@Configuration` class. You control instantiation.

```java
@Configuration
public class AppConfig {
    
    @Bean
    public OrderService orderService() {
        // You control how instance is created
        return new OrderService(customDependency());
    }
    
    @Bean
    public DataSource dataSource() {
        // Complex setup
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost/db");
        return new HikariDataSource(config);
    }
}
```

### When to Use Which

**Use @Component:**
- Simple classes
- Standard instantiation
- Framework-managed beans

**Use @Bean:**
- Third-party classes (can't add @Component)
- Complex initialization logic
- Conditional bean creation
- Multiple instances with different configs

```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return new HikariDataSource(primaryConfig());
    }
    
    @Bean
    @Qualifier("secondary")
    public DataSource secondaryDataSource() {
        return new HikariDataSource(secondaryConfig());
    }
}
```

## 4.5 @Qualifier / @Primary

### Problem: Multiple Beans of Same Type

```java
// Two implementations
@Component("email")
public class EmailNotificationService implements NotificationService { }

@Component("sms")
public class SMSService implements NotificationService { }

// Which one to inject?
@Service
public class OrderService {
    private final NotificationService notificationService; // Ambiguous!
}
```

### Solution 1: @Qualifier

```java
@Service
public class OrderService {
    private final NotificationService notificationService;
    
    public OrderService(@Qualifier("email") NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

### Solution 2: @Primary

```java
@Component
@Primary // Default when multiple beans exist
public class EmailNotificationService implements NotificationService { }

@Component
public class SMSService implements NotificationService { }

// EmailNotificationService injected by default
@Service
public class OrderService {
    private final NotificationService notificationService; // EmailNotificationService
}
```

### When to Use Which

- **@Qualifier:** Explicit selection, multiple consumers need different beans
- **@Primary:** Default choice, most common use case

## 4.6 Constructor Injection (Deep Dive)

### Why Constructor Injection?

#### 1. Immutability
```java
@Service
public class OrderService {
    private final OrderRepository repository; // final = immutable
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

#### 2. Required Dependencies
```java
// Constructor injection - fails fast if dependency missing
public OrderService(OrderRepository repository) {
    if (repository == null) {
        throw new IllegalArgumentException("Repository required");
    }
    this.repository = repository;
}

// Setter injection - may fail later at runtime
public void setRepository(OrderRepository repository) {
    this.repository = repository; // Could be null!
}
```

#### 3. Testing
```java
// Easy to test - just pass mock
OrderRepository mockRepo = mock(OrderRepository.class);
OrderService service = new OrderService(mockRepo);

// vs Field injection - need Spring context or reflection
```

#### 4. Circular Dependencies
```java
// Constructor injection detects circular dependency at startup
@Service
public class ServiceA {
    public ServiceA(ServiceB serviceB) { } // Circular!
}

@Service
public class ServiceB {
    public ServiceB(ServiceA serviceA) { } // Circular!
}
// Spring fails fast with BeanCurrentlyInCreationException
```

## 4.7 @SpringBootApplication Internals

### What It Does

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**@SpringBootApplication is equivalent to:**
```java
@SpringBootConfiguration  // @Configuration
@EnableAutoConfiguration // Auto-configuration
@ComponentScan           // Component scanning
public class Application { }
```

### Component Scanning

```java
// Scans current package and sub-packages
@SpringBootApplication
public class Application { } // Scans com.example and below

// Custom scan base
@SpringBootApplication(scanBasePackages = "com.custom")
public class Application { }
```

### Auto-Configuration

**How it works:**
1. Spring Boot scans classpath for `META-INF/spring.factories`
2. Finds `@Configuration` classes with `@ConditionalOn*` annotations
3. Evaluates conditions (classpath, properties, beans)
4. Creates beans if conditions met

**Example:**
```java
@Configuration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.url")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JpaAutoConfiguration {
    // Only created if DataSource class exists AND property set
}
```

## 4.8 Auto-Configuration

### How Auto-Configuration Works

```
┌─────────────────────────────────────────────────────────┐
│              Spring Boot Auto-Configuration               │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Application Starts                                   │
│     ┌──────────────┐                                    │
│     │ @SpringBoot  │                                    │
│     │ Application  │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  2. Load spring.factories                               │
│     ┌──────────────┐                                    │
│     │ META-INF/    │                                    │
│     │ spring.      │                                    │
│     │ factories    │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  3. Evaluate Conditions                                 │
│     ┌──────────────┐                                    │
│     │ @Conditional │                                    │
│     │ OnClass      │                                    │
│     │ OnProperty   │                                    │
│     │ OnBean       │                                    │
│     └──────────────┘                                    │
│            │                                            │
│            ▼                                            │
│  4. Create Auto-Configured Beans                       │
│     ┌──────────────┐                                    │
│     │ DataSource   │                                    │
│     │ JPA          │                                    │
│     │ WebMvc       │                                    │
│     └──────────────┘                                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Conditional Annotations

```java
// Conditional on class existing
@ConditionalOnClass(DataSource.class)

// Conditional on property
@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")

// Conditional on bean existing
@ConditionalOnBean(DataSource.class)

// Conditional on bean missing
@ConditionalOnMissingBean(DataSource.class)

// Conditional on classpath resource
@ConditionalOnResource(resources = "classpath:config.properties")
```

### Disabling Auto-Configuration

```java
// Disable specific auto-configuration
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})

// Disable via properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

## 4.9 Profiles

### Purpose
**Definition:** Mechanism to separate configuration for different environments.

### Defining Profiles

```java
// application.yml
spring:
  profiles:
    active: dev

---
spring:
  config:
    activate:
      on-profile: dev
datasource:
  url: jdbc:h2:mem:devdb

---
spring:
  config:
    activate:
      on-profile: prod
datasource:
  url: jdbc:postgresql://prod-server/db
```

### Profile-Specific Beans

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource devDataSource() {
        return new H2DataSource();
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource prodDataSource() {
        return new HikariDataSource();
    }
}
```

### Activating Profiles

```bash
# Via application.properties
spring.profiles.active=dev,local

# Via environment variable
export SPRING_PROFILES_ACTIVE=prod

# Via command line
java -jar app.jar --spring.profiles.active=prod

# Programmatically
SpringApplication app = new SpringApplication(Application.class);
app.setAdditionalProfiles("dev");
```

## 4.10 Actuator

### Purpose
**Definition:** Production-ready features for monitoring and managing application.

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"  # Expose all endpoints
  endpoint:
    health:
      show-details: always
```

**Common Endpoints:**
- `/actuator/health` - Application health
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/env` - Environment variables
- `/actuator/loggers` - Logger configuration
- `/actuator/beans` - All Spring beans

### Custom Health Indicator

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

## 4.11 Interview Questions & Answers

### Q1: Explain the difference between @Component, @Service, @Repository, and @Controller
**Answer:**
All are `@Component` stereotypes with semantic meaning:
- **@Component:** Generic Spring component
- **@Service:** Business logic layer
- **@Repository:** Data access layer (enables exception translation)
- **@Controller:** Web layer (MVC)

Functionally equivalent, but convey intent and enable layer-specific behavior.

### Q2: What is the difference between @Autowired and @Resource?
**Answer:**
- **@Autowired:** Spring-specific, by type, can use @Qualifier
- **@Resource:** JSR-250 standard, by name then type
- **@Inject:** JSR-330 standard, similar to @Autowired

### Q3: Explain Spring Bean Scopes
**Answer:**
1. **singleton (default):** One instance per container
2. **prototype:** New instance each time
3. **request:** One per HTTP request (web)
4. **session:** One per HTTP session (web)
5. **application:** One per ServletContext (web)

### Q4: How does Spring handle circular dependencies?
**Answer:**
- **Constructor injection:** Fails fast with `BeanCurrentlyInCreationException`
- **Setter/Field injection:** Uses proxy objects to break cycle
- **Best practice:** Refactor to avoid circular dependencies

### Q5: What is the difference between @Configuration and @Component?
**Answer:**
- **@Configuration:** Full proxy mode, `@Bean` methods called once (singleton)
- **@Component:** Lite mode, `@Bean` methods called each time (if used as regular method)
- **Use @Configuration:** When defining `@Bean` methods
- **Use @Component:** For regular component scanning

---

# SECTION 5: REST API DESIGN

## 5.1 REST Principles

### What is REST?
**Definition:** Representational State Transfer - architectural style for designing web services.

### Core Principles

1. **Stateless:** Each request contains all information needed
2. **Client-Server:** Separation of concerns
3. **Uniform Interface:** Standard HTTP methods and status codes
4. **Resource-Based:** Everything is a resource (noun, not verb)
5. **Representation:** Resources have multiple representations (JSON, XML)

### RESTful URL Design

```
┌─────────────────────────────────────────────────────────┐
│              REST Request-Response Flow                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Client                    Server                        │
│  ┌──────┐                ┌────────┐                     │
│  │      │  GET /orders   │        │                     │
│  │      │ ─────────────> │        │                     │
│  │      │                │        │                     │
│  │      │  200 OK        │        │                     │
│  │      │ <───────────── │        │                     │
│  │      │ [orders...]    │        │                     │
│  └──────┘                └────────┘                     │
│                                                          │
│  ┌──────┐                ┌────────┐                     │
│  │      │  POST /orders  │        │                     │
│  │      │ ─────────────> │        │                     │
│  │      │ {order data}   │        │                     │
│  │      │                │        │                     │
│  │      │  201 Created   │        │                     │
│  │      │ <───────────── │        │                     │
│  │      │ {id, ...}      │        │                     │
│  └──────┘                └────────┘                     │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Good REST URLs:**
```
GET    /api/orders           - List all orders
GET    /api/orders/123       - Get order 123
POST   /api/orders           - Create order
PUT    /api/orders/123       - Update order 123
DELETE /api/orders/123       - Delete order 123
GET    /api/orders/123/items - Get items for order 123
```

**Bad REST URLs:**
```
GET    /api/getOrders        - Verb in URL
POST   /api/orders/123/delete - Action in URL
GET    /api/order?id=123      - Query param for resource ID
```

## 5.2 HTTP Verbs & Status Codes

### HTTP Verbs

| Verb | Purpose | Idempotent | Safe |
|------|---------|------------|------|
| GET | Retrieve resource | Yes | Yes |
| POST | Create resource | No | No |
| PUT | Update/replace resource | Yes | No |
| PATCH | Partial update | No | No |
| DELETE | Delete resource | Yes | No |

### Status Codes

#### 2xx Success
- **200 OK:** Request succeeded
- **201 Created:** Resource created (include Location header)
- **204 No Content:** Success with no response body

#### 3xx Redirection
- **301 Moved Permanently:** Resource moved
- **304 Not Modified:** Cached version valid

#### 4xx Client Error
- **400 Bad Request:** Invalid request
- **401 Unauthorized:** Authentication required
- **403 Forbidden:** Authenticated but not authorized
- **404 Not Found:** Resource doesn't exist
- **409 Conflict:** Resource conflict (e.g., duplicate)
- **422 Unprocessable Entity:** Validation failed

#### 5xx Server Error
- **500 Internal Server Error:** Generic server error
- **503 Service Unavailable:** Service temporarily unavailable

### Implementation Example

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @GetMapping
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        List<OrderDto> orders = orderService.findAll();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        return orderService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody @Valid OrderRequest request) {
        OrderDto created = orderService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable Long id,
            @RequestBody @Valid OrderRequest request) {
        return orderService.update(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (orderService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

## 5.3 API Versioning

### Strategies

#### 1. URL Path Versioning
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 { }

@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 { }
```

#### 2. Header Versioning
```java
@GetMapping(headers = "API-Version=1")
public ResponseEntity<OrderDto> getOrderV1() { }

@GetMapping(headers = "API-Version=2")
public ResponseEntity<OrderDto> getOrderV2() { }
```

#### 3. Query Parameter Versioning
```java
@GetMapping(params = "version=1")
public ResponseEntity<OrderDto> getOrderV1() { }
```

#### 4. Accept Header (Content Negotiation)
```java
@GetMapping(produces = "application/vnd.api.v1+json")
public ResponseEntity<OrderDto> getOrderV1() { }
```

### Best Practice: URL Path Versioning
- Most explicit and clear
- Easy to route
- Cache-friendly
- Industry standard

## 5.4 Idempotency

### What is Idempotency?
**Definition:** Multiple identical requests have same effect as single request.

### Idempotent Operations
- **GET:** Always idempotent
- **PUT:** Idempotent (replaces resource)
- **DELETE:** Idempotent (deleting already deleted = no-op)

### Non-Idempotent Operations
- **POST:** Creates new resource each time
- **PATCH:** May have side effects

### Implementing Idempotency for POST

```java
@PostMapping
public ResponseEntity<OrderDto> createOrder(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody @Valid OrderRequest request) {
    
    // Check if request already processed
    Optional<Order> existing = orderService.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return ResponseEntity.ok(toDto(existing.get()));
    }
    
    // Process and store idempotency key
    Order created = orderService.create(request, idempotencyKey);
    return ResponseEntity.created(/*...*/).body(toDto(created));
}
```

## 5.5 Pagination & Sorting

### Implementation

```java
@GetMapping
public ResponseEntity<Page<OrderDto>> getAllOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort) {
    
    Pageable pageable = PageRequest.of(page, size, parseSort(sort));
    Page<OrderDto> orders = orderService.findAll(pageable);
    return ResponseEntity.ok(orders);
}
```

### Response Format

```json
{
  "content": [
    {"id": 1, "total": 100.00},
    {"id": 2, "total": 200.00}
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### HATEOAS Links

```java
@GetMapping
public ResponseEntity<PagedModel<OrderDto>> getAllOrders(Pageable pageable) {
    Page<OrderDto> page = orderService.findAll(pageable);
    PagedModel<OrderDto> pagedModel = pagedResourcesAssembler.toModel(page);
    return ResponseEntity.ok(pagedModel);
}
```

## 5.6 Validation

### Bean Validation

```java
public class OrderRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotEmpty(message = "Items cannot be empty")
    @Valid
    private List<OrderItemRequest> items;
    
    @DecimalMin(value = "0.01", message = "Total must be positive")
    private BigDecimal total;
    
    @Email(message = "Invalid email format")
    private String customerEmail;
}
```

### Controller Validation

```java
@PostMapping
public ResponseEntity<OrderDto> createOrder(
        @RequestBody @Valid OrderRequest request,
        BindingResult bindingResult) {
    
    if (bindingResult.hasErrors()) {
        // Handle validation errors
        return ResponseEntity.badRequest().build();
    }
    
    // Process request
}
```

### Custom Validators

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderStatusValidator.class)
public @interface ValidOrderStatus {
    String message() default "Invalid order status";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class OrderStatusValidator implements ConstraintValidator<ValidOrderStatus, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return Arrays.asList("PENDING", "CONFIRMED", "SHIPPED").contains(value);
    }
}
```

## 5.7 Global Exception Handling

### @RestControllerAdvice

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "ORDER_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### Error Response DTO

```java
public class ErrorResponse {
    private String code;
    private String message;
    private Map<String, String> details;
    private LocalDateTime timestamp;
    
    // Constructors, getters, setters
}
```

## 5.8 Interview Questions & Answers

### Q1: What makes an API RESTful?
**Answer:**
- Uses HTTP methods correctly (GET, POST, PUT, DELETE)
- Stateless (each request independent)
- Resource-based URLs (nouns, not verbs)
- Standard HTTP status codes
- JSON/XML representations
- HATEOAS (optional but recommended)

### Q2: When to use PUT vs PATCH?
**Answer:**
- **PUT:** Replace entire resource (idempotent)
- **PATCH:** Partial update (may not be idempotent)
- **Use PUT:** When updating entire resource
- **Use PATCH:** When updating specific fields

### Q3: How do you handle API versioning?
**Answer:**
- **URL Path:** `/api/v1/orders` (recommended)
- **Header:** `API-Version: 1`
- **Query Param:** `?version=1`
- **Accept Header:** Content negotiation

### Q4: What is idempotency and why is it important?
**Answer:**
- Multiple identical requests = same effect as one request
- Important for: Retries, network failures, duplicate prevention
- Implement with: Idempotency keys, unique constraints, state checks

### Q5: How do you design pagination in REST API?
**Answer:**
- Query params: `page`, `size`, `sort`
- Response includes: content, total elements, total pages
- Use Spring Data `Pageable` and `Page`
- Consider cursor-based pagination for large datasets

---

# SECTION 6: SPRING DATA JPA & DATABASES

## 6.1 Entity Relationships

### Types of Relationships

#### One-to-One
```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}

@Entity
public class UserProfile {
    @Id
    private Long id;
    
    @OneToOne(mappedBy = "profile")
    private User user;
}
```

#### One-to-Many
```java
@Entity
public class Order {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
}

@Entity
public class OrderItem {
    @Id
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
```

#### Many-to-Many
```java
@Entity
public class Student {
    @Id
    private Long id;
    
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses;
}

@Entity
public class Course {
    @Id
    private Long id;
    
    @ManyToMany(mappedBy = "courses")
    private Set<Student> students;
}
```

## 6.2 Lazy vs Eager Loading

### Lazy Loading (Default for @OneToMany, @ManyToMany)
```java
@Entity
public class Order {
    @OneToMany(fetch = FetchType.LAZY)
    private List<OrderItem> items; // Not loaded until accessed
}

// Accessing items triggers query
Order order = orderRepository.findById(1L);
List<OrderItem> items = order.getItems(); // Query executed here
```

### Eager Loading (Default for @OneToOne, @ManyToOne)
```java
@Entity
public class OrderItem {
    @ManyToOne(fetch = FetchType.EAGER)
    private Order order; // Loaded immediately
}
```

### Fetch Strategy Diagram

```
┌─────────────────────────────────────────────────────────┐
│              JPA Fetch Strategies                        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  LAZY Loading                                           │
│  ┌──────────┐                                           │
│  │  Order   │                                           │
│  │  (id=1)  │                                           │
│  └────┬─────┘                                           │
│       │                                                  │
│       │ order.getItems()                                │
│       │ (Proxy triggers query)                         │
│       ▼                                                  │
│  ┌──────────┐                                           │
│  │ OrderItem│                                           │
│  │ OrderItem│                                           │
│  └──────────┘                                           │
│                                                          │
│  EAGER Loading                                          │
│  ┌──────────┐                                           │
│  │  Order   │ ───> JOIN ───> ┌──────────┐            │
│  │  (id=1)  │                │ OrderItem│            │
│  └──────────┘                │ OrderItem│            │
│                               └──────────┘            │
│  (Single query with JOIN)                             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Best Practices
- **Use LAZY:** Default for collections (prevents N+1)
- **Use EAGER:** Only when always needed
- **Use @EntityGraph:** For specific queries needing eager loading

## 6.3 N+1 Problem

### What is N+1 Problem?
**Definition:** Executing N+1 queries instead of 1 query when fetching related entities.

### Example of N+1 Problem

```java
// This causes N+1 problem
List<Order> orders = orderRepository.findAll(); // 1 query

for (Order order : orders) {
    List<OrderItem> items = order.getItems(); // N queries (one per order)
}
// Total: 1 + N queries
```

### Solutions

#### 1. Join Fetch
```java
@Query("SELECT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();
```

#### 2. Entity Graph
```java
@Entity
@NamedEntityGraph(
    name = "Order.items",
    attributeNodes = @NamedAttributeNode("items")
)
public class Order { }

@Query("SELECT o FROM Order o")
@EntityGraph("Order.items")
List<Order> findAllWithItems();
```

#### 3. @EntityGraph on Repository
```java
@EntityGraph(attributePaths = {"items", "customer"})
List<Order> findAll();
```

## 6.4 Transactions

### Transaction Propagation

```
┌─────────────────────────────────────────────────────────┐
│            Transaction Propagation Types                 │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  REQUIRED (default)                                     │
│  - Join existing or create new                          │
│                                                          │
│  REQUIRES_NEW                                           │
│  - Always create new transaction                        │
│  - Suspends existing                                    │
│                                                          │
│  SUPPORTS                                               │
│  - Join if exists, run without if not                   │
│                                                          │
│  NOT_SUPPORTED                                           │
│  - Suspend existing, run without transaction           │
│                                                          │
│  NEVER                                                  │
│  - Must not run in transaction                          │
│                                                          │
│  MANDATORY                                              │
│  - Must run in existing transaction                     │
│                                                          │
│  NESTED                                                 │
│  - Create nested transaction (savepoint)                │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Implementation
```java
@Service
@Transactional
public class OrderService {
    
    @Transactional(propagation = Propagation.REQUIRED)
    public Order createOrder(OrderRequest request) {
        // Runs in transaction
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrder(Order order) {
        // New transaction (commits independently)
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendEmail(Order order) {
        // No transaction (suspends if exists)
    }
}
```

### Transaction Isolation Levels

```
┌─────────────────────────────────────────────────────────┐
│            Transaction Isolation Levels                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  READ_UNCOMMITTED (Lowest)                              │
│  - Dirty reads allowed                                  │
│  - No isolation                                         │
│                                                          │
│  READ_COMMITTED (Default in most DBs)                   │
│  - Prevents dirty reads                                 │
│  - Allows non-repeatable reads                          │
│                                                          │
│  REPEATABLE_READ                                        │
│  - Prevents dirty reads                                 │
│  - Prevents non-repeatable reads                        │
│  - Allows phantom reads                                 │
│                                                          │
│  SERIALIZABLE (Highest)                                  │
│  - Full isolation                                       │
│  - Prevents all anomalies                               │
│  - Lowest concurrency                                   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Isolation Anomalies

1. **Dirty Read:** Reading uncommitted data
2. **Non-Repeatable Read:** Same query returns different results
3. **Phantom Read:** New rows appear in range query

### Implementation
```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public Order getOrder(Long id) {
    return orderRepository.findById(id);
}

@Transactional(isolation = Isolation.SERIALIZABLE)
public void processCriticalOrder(Order order) {
    // Highest isolation for critical operations
}
```

## 6.5 Optimistic vs Pessimistic Locking

### Optimistic Locking
**Definition:** Assumes conflicts are rare. Uses version field to detect conflicts.

```java
@Entity
public class Order {
    @Id
    private Long id;
    
    @Version
    private Long version; // Optimistic lock field
    
    private BigDecimal total;
}

// Usage
@Transactional
public void updateOrder(Long id, BigDecimal newTotal) {
    Order order = orderRepository.findById(id).orElseThrow();
    order.setTotal(newTotal);
    // If version changed, throws OptimisticLockException
    orderRepository.save(order);
}
```

**When to use:**
- Low contention
- Read-heavy workloads
- Better performance

### Pessimistic Locking
**Definition:** Locks row during transaction. Prevents concurrent modifications.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findByIdForUpdate(@Param("id") Long id);

// Usage
@Transactional
public void updateOrder(Long id, BigDecimal newTotal) {
    Order order = orderRepository.findByIdForUpdate(id).orElseThrow();
    // Row is locked until transaction commits
    order.setTotal(newTotal);
    orderRepository.save(order);
}
```

**When to use:**
- High contention
- Critical updates
- Must prevent conflicts

## 6.6 Indexing

### Purpose
**Definition:** Database structure that speeds up queries by creating sorted data structure.

### Types of Indexes

#### Single Column Index
```sql
CREATE INDEX idx_order_customer ON orders(customer_id);
```

#### Composite Index
```sql
CREATE INDEX idx_order_status_date ON orders(status, created_at);
```

#### Unique Index
```sql
CREATE UNIQUE INDEX idx_order_number ON orders(order_number);
```

### JPA Index Definition
```java
@Entity
@Table(indexes = {
    @Index(name = "idx_customer", columnList = "customer_id"),
    @Index(name = "idx_status_date", columnList = "status,created_at")
})
public class Order {
    @Column(name = "customer_id")
    private Long customerId;
    
    private OrderStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### Index Best Practices
1. Index foreign keys
2. Index frequently queried columns
3. Composite indexes: Most selective first
4. Don't over-index (slows writes)

## 6.7 SQL Performance Basics

### Query Optimization

#### 1. Use Indexes
```sql
-- Good: Uses index
SELECT * FROM orders WHERE customer_id = 123;

-- Bad: Full table scan
SELECT * FROM orders WHERE UPPER(status) = 'PENDING';
```

#### 2. Avoid SELECT *
```sql
-- Bad: Fetches all columns
SELECT * FROM orders;

-- Good: Only needed columns
SELECT id, total, status FROM orders;
```

#### 3. Use LIMIT
```sql
-- Good: Limits result set
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;

-- Bad: Fetches all
SELECT * FROM orders ORDER BY created_at DESC;
```

#### 4. Avoid Functions on Indexed Columns
```sql
-- Bad: Can't use index
SELECT * FROM orders WHERE YEAR(created_at) = 2024;

-- Good: Uses index
SELECT * FROM orders WHERE created_at >= '2024-01-01' 
  AND created_at < '2025-01-01';
```

### Explain Plan
```sql
EXPLAIN SELECT * FROM orders WHERE customer_id = 123;
-- Shows execution plan, index usage
```

## 6.8 Interview Questions & Answers

### Q1: What is the difference between LAZY and EAGER loading?
**Answer:**
- **LAZY:** Loads related entities when accessed (proxy)
- **EAGER:** Loads immediately with parent entity
- **Default:** LAZY for collections, EAGER for single associations
- **Best practice:** Use LAZY, fetch eagerly only when needed

### Q2: How do you solve the N+1 problem?
**Answer:**
1. **Join Fetch:** `JOIN FETCH` in JPQL
2. **Entity Graph:** `@EntityGraph` annotation
3. **Batch fetching:** `@BatchSize`
4. **Fetch joins:** In query methods

### Q3: Explain transaction propagation types
**Answer:**
- **REQUIRED:** Join existing or create new (default)
- **REQUIRES_NEW:** Always new transaction
- **SUPPORTS:** Join if exists, run without if not
- **NOT_SUPPORTED:** Suspend existing
- **MANDATORY:** Must have existing transaction
- **NEVER:** Must not have transaction
- **NESTED:** Create savepoint

### Q4: What is the difference between optimistic and pessimistic locking?
**Answer:**
- **Optimistic:** Uses version field, assumes low contention, better performance
- **Pessimistic:** Locks row, prevents concurrent access, lower performance
- **Use optimistic:** Low contention, read-heavy
- **Use pessimistic:** High contention, critical updates

### Q5: How do you optimize JPA queries?
**Answer:**
1. Use indexes on frequently queried columns
2. Avoid N+1 (use fetch joins)
3. Use pagination (limit results)
4. Select only needed columns
5. Use `@Query` for complex queries
6. Enable query caching for repeated queries

---

# SECTION 7: MICROSERVICES ARCHITECTURE

## 7.1 Monolith vs Microservices

### Monolith Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Monolithic Architecture                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────┐    │
│  │           Single Application                  │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐   │    │
│  │  │  User    │  │  Order   │  │ Product  │   │    │
│  │  │  Module  │  │  Module  │  │  Module  │   │    │
│  │  └──────────┘  └──────────┘  └──────────┘   │    │
│  │                                                │    │
│  │  ┌──────────────────────────────────────┐   │    │
│  │  │        Shared Database                │   │    │
│  │  └──────────────────────────────────────┘   │    │
│  └──────────────────────────────────────────────┘    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Advantages:**
- Simple to develop and deploy
- Easier testing
- Better performance (no network calls)
- ACID transactions across modules

**Disadvantages:**
- Tight coupling
- Difficult to scale independently
- Technology lock-in
- Single point of failure

### Microservices Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Microservices Architecture                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │  User    │  │  Order   │  │ Product  │            │
│  │ Service  │  │ Service  │  │ Service  │            │
│  │          │  │          │  │          │            │
│  │  ┌────┐  │  │  ┌────┐  │  │  ┌────┐  │            │
│  │  │ DB │  │  │  │ DB │  │  │  │ DB │  │            │
│  │  └────┘  │  │  └────┘  │  │  └────┘  │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
│       │             │             │                    │
│       └─────────────┴─────────────┘                    │
│                   │                                      │
│            ┌──────▼──────┐                             │
│            │   API       │                             │
│            │  Gateway    │                             │
│            └─────────────┘                             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Advantages:**
- Independent deployment
- Technology diversity
- Scalability
- Fault isolation

**Disadvantages:**
- Distributed system complexity
- Network latency
- Data consistency challenges
- Operational overhead

## 7.2 Database per Service

### Principle
**Definition:** Each microservice has its own database. Services don't share databases.

### Why?
1. **Independence:** Service can change schema without affecting others
2. **Technology Choice:** Each service can use appropriate database
3. **Scalability:** Scale databases independently
4. **Fault Isolation:** Database failure affects only one service

### Challenges
- **Data Consistency:** No ACID transactions across services
- **Queries:** Can't join across service databases
- **Data Duplication:** May need to duplicate data

### Solutions
- **Saga Pattern:** For distributed transactions
- **Event Sourcing:** For audit and consistency
- **CQRS:** Separate read/write models

## 7.3 Sync vs Async Communication

### Synchronous Communication

**HTTP/REST:**
```java
// Order Service calls User Service
@RestClient
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserDto getUser(@PathVariable Long id);
}

@Service
public class OrderService {
    private final UserServiceClient userClient;
    
    public Order createOrder(OrderRequest request) {
        // Synchronous call - blocks until response
        UserDto user = userClient.getUser(request.getUserId());
        // Continue processing...
    }
}
```

**Pros:**
- Simple to implement
- Immediate response
- Easy error handling

**Cons:**
- Tight coupling
- Blocking calls
- Cascading failures

### Asynchronous Communication

**Message Queue (Kafka/RabbitMQ):**
```java
// Order Service publishes event
@Service
public class OrderService {
    private final StreamBridge streamBridge;
    
    public Order createOrder(OrderRequest request) {
        Order order = // create order
        // Asynchronous - non-blocking
        streamBridge.send("order-created", new OrderCreatedEvent(order));
        return order;
    }
}

// Notification Service consumes event
@Component
public class OrderEventConsumer {
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Process asynchronously
        sendNotification(event.getOrder());
    }
}
```

**Pros:**
- Loose coupling
- Better scalability
- Fault tolerance

**Cons:**
- Eventual consistency
- Complex error handling
- Message ordering challenges

## 7.4 API Gateway

### Purpose
**Definition:** Single entry point for all client requests. Routes to appropriate microservices.

### Responsibilities
1. **Routing:** Route requests to services
2. **Authentication/Authorization:** Centralized security
3. **Rate Limiting:** Protect backend services
4. **Load Balancing:** Distribute load
5. **Caching:** Cache responses
6. **Request/Response Transformation:** Modify data format

### Implementation (Spring Cloud Gateway)
```java
@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("order-service", r -> r
                .path("/api/orders/**")
                .uri("lb://ORDER-SERVICE"))
            .route("user-service", r -> r
                .path("/api/users/**")
                .uri("lb://USER-SERVICE"))
            .build();
    }
}
```

## 7.5 Config Management

### Problem
- Configuration scattered across services
- Difficult to manage and update
- Environment-specific configs

### Solution: Config Server

**Spring Cloud Config Server:**
```yaml
# configserver/src/main/resources/config/order-service.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost/orders
  jpa:
    hibernate:
      ddl-auto: update
```

**Client Configuration:**
```yaml
# order-service/src/main/resources/application.yml
spring:
  config:
    import: optional:configserver:http://config-server:8888
  application:
    name: order-service
  profiles:
    active: docker
```

## 7.6 Service Discovery

### Problem
- Services need to find each other
- Hard-coded URLs don't work in dynamic environments
- Load balancing across instances

### Solution: Eureka

**Eureka Server:**
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**Eureka Client:**
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
```

**Service Registration:**
```java
@SpringBootApplication
@EnableEurekaClient
public class OrderServiceApplication {
    // Automatically registers with Eureka
}
```

## 7.7 Distributed Tracing

### Problem
- Requests span multiple services
- Difficult to trace request flow
- Hard to debug issues

### Solution: Zipkin / Jaeger

**Implementation:**
```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**Trace Flow:**
```
Request → Gateway → Order Service → User Service → Database
   |         |           |              |
   └─────────┴───────────┴──────────────┘
         Same Trace ID
```

## 7.8 Saga Pattern

### Problem
- No distributed transactions in microservices
- Need to maintain consistency across services

### Solution: Saga Pattern

**Choreography (Event-Driven):**
```
Order Service → OrderCreated Event
Payment Service → PaymentProcessed Event
Inventory Service → InventoryReserved Event
```

**Orchestration (Centralized):**
```
Saga Orchestrator:
  1. Create Order
  2. Process Payment
  3. Reserve Inventory
  4. If any fails → Compensate
```

**Compensation:**
```java
public class OrderSaga {
    public void createOrder(OrderRequest request) {
        try {
            Order order = orderService.create(request);
            paymentService.process(order);
            inventoryService.reserve(order);
        } catch (Exception e) {
            // Compensate
            orderService.cancel(order);
            paymentService.refund(order);
            inventoryService.release(order);
        }
    }
}
```

## 7.9 Interview Questions & Answers

### Q1: When should you use microservices vs monolith?
**Answer:**
- **Microservices:** Large team, independent scaling, technology diversity, complex domain
- **Monolith:** Small team, simple application, tight consistency requirements, startup phase

### Q2: How do you handle data consistency in microservices?
**Answer:**
- **Saga Pattern:** Distributed transactions with compensation
- **Event Sourcing:** Store events, rebuild state
- **CQRS:** Separate read/write models
- **Eventual Consistency:** Accept temporary inconsistency

### Q3: What is the API Gateway pattern?
**Answer:**
- Single entry point for clients
- Routes requests to services
- Handles cross-cutting concerns (auth, rate limiting, caching)
- Examples: Spring Cloud Gateway, Kong, AWS API Gateway

### Q4: How does service discovery work?
**Answer:**
- Services register with discovery server (Eureka, Consul)
- Services query discovery server to find instances
- Load balancing across instances
- Health checks remove unhealthy instances

### Q5: Explain the Saga pattern
**Answer:**
- Pattern for distributed transactions
- **Choreography:** Services coordinate via events
- **Orchestration:** Central coordinator manages flow
- **Compensation:** Rollback via compensating transactions

---

# SECTION 8: APACHE KAFKA (CRITICAL)

## 8.1 Kafka Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────┐
│              Apache Kafka Architecture                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Producers                                              │
│  ┌────────┐  ┌────────┐  ┌────────┐                   │
│  │Producer│  │Producer│  │Producer│                   │
│  └───┬────┘  └───┬────┘  └───┬────┘                   │
│      │           │           │                         │
│      └───────────┴───────────┘                         │
│                  │                                      │
│                  ▼                                      │
│  ┌──────────────────────────────────────────┐        │
│  │         Kafka Cluster                     │        │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐│        │
│  │  │ Broker 1 │  │ Broker 2 │  │ Broker 3 ││        │
│  │  │          │  │          │  │          ││        │
│  │  │ Topic:   │  │ Topic:   │  │ Topic:   ││        │
│  │  │ orders   │  │ orders   │  │ orders   ││        │
│  │  │ P0 P1 P2 │  │ P0 P1 P2 │  │ P0 P1 P2 ││        │
│  │  └──────────┘  └──────────┘  └──────────┘│        │
│  └──────────────────────────────────────────┘        │
│                  │                                      │
│                  ▼                                      │
│  ┌────────┐  ┌────────┐  ┌────────┐                   │
│  │Consumer│  │Consumer│  │Consumer│                   │
│  │Group A │  │Group A │  │Group B │                   │
│  └────────┘  └────────┘  └────────┘                   │
│                                                          │
│  Zookeeper (or KRaft) - Coordination                    │
│  ┌──────────────────────────────────────────┐        │
│  │  - Broker metadata                        │        │
│  │  - Topic configuration                   │        │
│  │  - Consumer group coordination            │        │
│  └──────────────────────────────────────────┘        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Key Concepts

1. **Broker:** Kafka server that stores data
2. **Topic:** Category/feed name to which records are published
3. **Partition:** Topic is divided into partitions for parallelism
4. **Producer:** Publishes messages to topics
5. **Consumer:** Reads messages from topics
6. **Consumer Group:** Group of consumers that work together

## 8.2 Topics, Partitions, Offsets

### Topic Structure

```
Topic: "orders"
├── Partition 0: [msg0, msg1, msg2, ...]
├── Partition 1: [msg0, msg1, msg2, ...]
└── Partition 2: [msg0, msg1, msg2, ...]
```

### Partitions

**Purpose:**
- **Parallelism:** Multiple consumers can read from different partitions
- **Scalability:** Distribute load across brokers
- **Ordering:** Messages within partition are ordered

**Partition Assignment:**
```java
// Producer decides partition
producer.send(new ProducerRecord<>("orders", partition, key, value));

// Or let Kafka decide (key-based or round-robin)
producer.send(new ProducerRecord<>("orders", key, value));
```

### Offsets

**Definition:** Unique identifier for each message within a partition.

```
Partition 0:
Offset: 0    1    2    3    4    5
        [msg][msg][msg][msg][msg][msg]
```

**Offset Management:**
- **Consumer commits offset** after processing
- **On restart:** Consumer resumes from last committed offset
- **Offset storage:** In `__consumer_offsets` topic (internal)

```java
// Manual offset commit
consumer.commitSync();

// Auto commit (default)
properties.setProperty("enable.auto.commit", "true");
properties.setProperty("auto.commit.interval.ms", "5000");
```

## 8.3 Producer Flow

### Producer Internals

```
┌─────────────────────────────────────────────────────────┐
│              Kafka Producer Flow                        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Producer.send()                                    │
│     ┌──────────────┐                                    │
│     │ Create      │                                    │
│     │ ProducerRecord│                                  │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  2. Serialize Key & Value                             │
│     ┌──────────────┐                                    │
│     │ Serializer  │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  3. Partition Selection                                │
│     ┌──────────────┐                                    │
│     │ If key: hash │                                    │
│     │ Else: round  │                                    │
│     │   robin      │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  4. Add to RecordAccumulator                           │
│     ┌──────────────┐                                    │
│     │ Batch by     │                                    │
│     │ partition    │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  5. Sender Thread                                      │
│     ┌──────────────┐                                    │
│     │ Send batch   │                                    │
│     │ to broker    │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  6. Broker Response                                    │
│     ┌──────────────┐                                    │
│     │ Success/     │                                    │
│     │ Error        │                                    │
│     └──────────────┘                                    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Producer Configuration

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

// Reliability
props.put("acks", "all"); // Wait for all replicas
props.put("retries", 3);
props.put("max.in.flight.requests.per.connection", 1); // For ordering

// Performance
props.put("batch.size", 16384);
props.put("linger.ms", 10); // Wait for batch
props.put("compression.type", "snappy");

KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```

### Producer Code Example

```java
@Service
public class OrderEventProducer {
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotal()
        );
        
        // Send with key (ensures same partition)
        kafkaTemplate.send("order-events", 
            order.getCustomerId().toString(), // Key
            event
        );
    }
}
```

## 8.4 Consumer Groups & Rebalancing

### Consumer Groups

**Definition:** Group of consumers that work together to consume a topic.

**Partition Assignment:**
```
Topic: "orders" (3 partitions)
Consumer Group: "order-processors" (2 consumers)

Consumer 1 → Partition 0, Partition 1
Consumer 2 → Partition 2
```

### Consumer Group Rebalancing

```
┌─────────────────────────────────────────────────────────┐
│          Consumer Group Rebalancing Flow                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Initial State                                          │
│  Consumer 1 → P0, P1                                    │
│  Consumer 2 → P2                                        │
│                                                          │
│  Consumer 3 Joins                                       │
│  ┌──────────────────────────────────────┐             │
│  │  1. Consumer 3 sends JoinGroup       │             │
│  │  2. Coordinator triggers rebalance   │             │
│  │  3. All consumers stop consuming      │             │
│  │  4. New partition assignment         │             │
│  │  5. Consumers resume consuming       │             │
│  └──────────────────────────────────────┘             │
│                                                          │
│  After Rebalance                                       │
│  Consumer 1 → P0                                        │
│  Consumer 2 → P1                                        │
│  Consumer 3 → P2                                        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Rebalancing Triggers:**
1. Consumer joins group
2. Consumer leaves group (crash, shutdown)
3. New partition added to topic
4. Consumer heartbeat timeout

**Rebalancing Strategies:**
- **Range:** Assigns consecutive partitions
- **Round Robin:** Distributes evenly
- **Sticky:** Minimizes partition movement
- **Cooperative Sticky:** Incremental rebalancing

### Consumer Configuration

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "order-processors");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

// Offset management
props.put("enable.auto.commit", "false"); // Manual commit
props.put("auto.offset.reset", "earliest"); // earliest or latest

// Performance
props.put("fetch.min.bytes", 1);
props.put("fetch.max.wait.ms", 500);
props.put("max.partition.fetch.bytes", 1048576);

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
```

### Consumer Code Example

```java
@Component
public class OrderEventConsumer {
    
    @KafkaListener(topics = "order-events", groupId = "order-processors")
    public void consume(OrderCreatedEvent event) {
        try {
            processOrder(event);
            // Manual commit after processing
            // ack.acknowledge(); // If using manual ack
        } catch (Exception e) {
            // Handle error
            log.error("Error processing order", e);
        }
    }
}
```

## 8.5 Ordering Guarantees

### Ordering Levels

1. **Per-Partition Ordering:** Messages in same partition are ordered
2. **Global Ordering:** Not guaranteed across partitions

### Maintaining Order

```java
// Same key → same partition → ordered
producer.send(new ProducerRecord<>("orders", "customer-123", order1));
producer.send(new ProducerRecord<>("orders", "customer-123", order2));
// order1 and order2 processed in order (same partition)

// Different keys → different partitions → order not guaranteed
producer.send(new ProducerRecord<>("orders", "customer-123", order1));
producer.send(new ProducerRecord<>("orders", "customer-456", order2));
// Order not guaranteed
```

### Ensuring Ordering

1. **Single Partition:** Topic with 1 partition (limits parallelism)
2. **Key-Based Partitioning:** Same key → same partition
3. **max.in.flight.requests.per.connection = 1:** Prevents reordering on retry

## 8.6 Replication & ISR

### Replication

**Definition:** Copies of partitions across multiple brokers for fault tolerance.

```
Topic: "orders", Partition 0, Replication Factor: 3

Broker 1 (Leader)    Broker 2 (Replica)    Broker 3 (Replica)
    P0                  P0                    P0
```

### ISR (In-Sync Replicas)

**Definition:** Replicas that are up-to-date with the leader.

**ISR Maintenance:**
- Replica sends fetch requests to leader
- If replica lags behind (replica.lag.time.max.ms), removed from ISR
- Leader only waits for ISR replicas (if acks=all)

### Leader Election

```
┌─────────────────────────────────────────────────────────┐
│              Leader Election Process                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Normal State                                           │
│  Broker 1 (Leader) ←─── Broker 2 (Replica)             │
│      │                      │                           │
│      └──────────────────────┘                           │
│                                                          │
│  Leader Fails                                           │
│  Broker 1 (Down)        Broker 2 (Replica)              │
│                                                          │
│  Election                                                │
│  ┌──────────────────────────────────────┐             │
│  │ 1. Controller detects leader down    │             │
│  │ 2. Selects new leader from ISR       │             │
│  │ 3. Updates metadata                  │             │
│  │ 4. Producers/consumers reconnect      │             │
│  └──────────────────────────────────────┘             │
│                                                          │
│  New State                                              │
│  Broker 1 (Down)        Broker 2 (Leader)              │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 8.7 At-Least-Once vs Exactly-Once

### Delivery Semantics

#### At-Least-Once
**Definition:** Message delivered at least once (may have duplicates).

**Configuration:**
```java
// Producer
props.put("acks", "all");
props.put("retries", Integer.MAX_VALUE);

// Consumer
props.put("enable.auto.commit", "false");
// Commit after processing
```

**Trade-off:** May process message multiple times (need idempotent processing)

#### At-Most-Once
**Definition:** Message delivered at most once (may lose messages).

**Configuration:**
```java
// Producer
props.put("acks", "0"); // Fire and forget
props.put("retries", 0);

// Consumer
props.put("enable.auto.commit", "true");
// Commit before processing
```

**Trade-off:** May lose messages

#### Exactly-Once
**Definition:** Message delivered exactly once.

**Configuration:**
```java
// Producer
props.put("enable.idempotence", "true");
props.put("acks", "all");
props.put("max.in.flight.requests.per.connection", 5);
props.put("retries", Integer.MAX_VALUE);

// Consumer (with transactions)
props.put("isolation.level", "read_committed");
```

**How it works:**
- **Idempotent Producer:** Prevents duplicate messages
- **Transactional Producer:** Atomic writes to multiple topics
- **Transactional Consumer:** Reads only committed messages

## 8.8 Idempotent Producer

### Purpose
**Definition:** Prevents duplicate messages even if producer retries.

### How It Works

```
┌─────────────────────────────────────────────────────────┐
│          Idempotent Producer Mechanism                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Producer sends message                                 │
│  ┌──────────────┐                                       │
│  │ PID (Producer│                                       │
│  │  ID)         │                                       │
│  │ Sequence #   │                                       │
│  └──────┬───────┘                                       │
│         │                                               │
│         ▼                                               │
│  Broker stores (PID, Sequence)                        │
│                                                          │
│  If duplicate (same PID, Sequence):                    │
│  ┌──────────────┐                                       │
│  │ Broker       │                                       │
│  │ recognizes   │                                       │
│  │ duplicate    │                                       │
│  │ Returns      │                                       │
│  │ success      │                                       │
│  │ (no store)   │                                       │
│  └──────────────┘                                       │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Configuration

```java
Properties props = new Properties();
props.put("enable.idempotence", "true");
// Automatically sets:
// - acks=all
// - retries=Integer.MAX_VALUE
// - max.in.flight.requests.per.connection=5
```

## 8.9 Retry Topics & DLQ

### Retry Pattern

```
┌─────────────────────────────────────────────────────────┐
│              Retry + DLQ Flow                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Main Topic                                             │
│  ┌──────────────┐                                       │
│  │ order-events │                                       │
│  └──────┬───────┘                                       │
│         │                                               │
│         ▼                                               │
│  Consumer processes                                    │
│         │                                               │
│         ▼                                               │
│  ┌──────────────┐                                       │
│  │ Success?     │                                       │
│  └──┬───────┬───┘                                       │
│     │       │                                           │
│  Yes│       │No                                         │
│     │       │                                           │
│     │       ▼                                           │
│     │  Retry Topic                                      │
│     │  ┌──────────────┐                                 │
│     │  │order-events │                                 │
│     │  │-retry-0     │                                 │
│     │  └──────┬──────┘                                 │
│     │         │                                         │
│     │         ▼                                         │
│     │  Retry Consumer                                  │
│     │         │                                         │
│     │         ▼                                         │
│     │  ┌──────────────┐                                 │
│     │  │ Max retries? │                                 │
│     │  └──┬───────┬───┘                                 │
│     │     │       │                                     │
│     │  No │       │Yes                                  │
│     │     │       │                                     │
│     │     │       ▼                                     │
│     │     │  DLQ (Dead Letter Queue)                    │
│     │     │  ┌──────────────┐                           │
│     │     │  │order-events │                           │
│     │     │  │-dlq         │                           │
│     │     │  └──────────────┘                           │
│     │     │                                             │
│     └─────┘                                             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Implementation

```java
@Component
public class OrderEventConsumer {
    
    @KafkaListener(topics = "order-events", groupId = "order-processors")
    public void consume(OrderCreatedEvent event, Acknowledgment ack) {
        try {
            processOrder(event);
            ack.acknowledge();
        } catch (Exception e) {
            // Send to retry topic
            kafkaTemplate.send("order-events-retry-0", event);
            ack.acknowledge(); // Commit offset to avoid reprocessing
        }
    }
    
    @KafkaListener(topics = "order-events-retry-0", groupId = "retry-processors")
    public void retry(OrderCreatedEvent event, Acknowledgment ack) {
        try {
            processOrder(event);
            ack.acknowledge();
        } catch (Exception e) {
            // After max retries, send to DLQ
            kafkaTemplate.send("order-events-dlq", event);
            ack.acknowledge();
        }
    }
}
```

## 8.10 Duplicate Handling

### Problem
Even with idempotent producer, duplicates can occur:
- Consumer crashes after processing but before commit
- Network issues
- Manual reprocessing

### Solutions

#### 1. Idempotent Processing
```java
@Service
public class OrderProcessor {
    private final Set<String> processedIds = new ConcurrentHashMap<>().newKeySet();
    
    public void processOrder(OrderCreatedEvent event) {
        String idempotencyKey = event.getId() + "-" + event.getTimestamp();
        
        // Check if already processed
        if (processedIds.contains(idempotencyKey)) {
            log.info("Duplicate event, skipping: {}", idempotencyKey);
            return;
        }
        
        // Process
        processOrderInternal(event);
        
        // Mark as processed
        processedIds.add(idempotencyKey);
    }
}
```

#### 2. Database Unique Constraint
```java
@Entity
public class ProcessedEvent {
    @Id
    private String eventId;
    private LocalDateTime processedAt;
}

// Before processing
if (processedEventRepository.existsById(event.getId())) {
    return; // Already processed
}

// After processing
processedEventRepository.save(new ProcessedEvent(event.getId()));
```

#### 3. Distributed Cache (Redis)
```java
@Service
public class OrderProcessor {
    private final RedisTemplate<String, String> redis;
    
    public void processOrder(OrderCreatedEvent event) {
        String key = "processed:" + event.getId();
        
        // Atomic check-and-set
        Boolean set = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(7));
        if (!set) {
            return; // Already processed
        }
        
        processOrderInternal(event);
    }
}
```

## 8.11 Interview Questions & Answers

### Q1: Explain Kafka architecture and components
**Answer:**
- **Brokers:** Kafka servers storing data
- **Topics:** Categories for messages
- **Partitions:** Topics divided for parallelism
- **Producers:** Publish messages
- **Consumers:** Read messages
- **Consumer Groups:** Coordinate consumption
- **Zookeeper/KRaft:** Coordination and metadata

### Q2: How does Kafka ensure ordering?
**Answer:**
- **Per-partition ordering:** Messages in same partition are ordered
- **Key-based partitioning:** Same key → same partition → ordered
- **max.in.flight.requests.per.connection=1:** Prevents reordering on retry
- **Global ordering:** Not guaranteed (would require single partition)

### Q3: What is consumer group rebalancing?
**Answer:**
- Process of redistributing partitions among consumers
- **Triggers:** Consumer joins/leaves, new partitions, heartbeat timeout
- **Process:** Stop consuming → reassign partitions → resume consuming
- **Strategies:** Range, Round Robin, Sticky, Cooperative Sticky

### Q4: Explain at-least-once vs exactly-once semantics
**Answer:**
- **At-least-once:** Message delivered ≥1 times (may have duplicates)
- **Exactly-once:** Message delivered exactly once
- **Implementation:** Idempotent producer + transactional consumer
- **Trade-off:** Exactly-once has performance overhead

### Q5: How do you handle duplicate messages in Kafka?
**Answer:**
1. **Idempotent processing:** Check if already processed
2. **Database unique constraint:** Store processed event IDs
3. **Distributed cache:** Redis for deduplication
4. **Idempotent producer:** Prevents duplicates at source
5. **Idempotent business logic:** Design operations to be idempotent

---

# SECTION 9: RESILIENCE & FAULT TOLERANCE

## 9.1 Timeouts

### Purpose
**Definition:** Maximum time to wait for a response before considering request failed.

### Implementation

```java
// HTTP Client Timeout
@Configuration
public class RestClientConfig {
    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestFactory(clientHttpRequestFactory())
            .baseUrl("http://user-service");
    }
    
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(10000); // 10 seconds
        return factory;
    }
}
```

### Best Practices
- **Connection timeout:** 2-5 seconds
- **Read timeout:** Based on expected response time
- **Different timeouts:** For different operations (read vs write)

## 9.2 Retry

### Purpose
**Definition:** Automatically retry failed operations.

### Resilience4j Retry

```java
@Service
public class OrderService {
    
    @Retry(name = "orderService", fallbackMethod = "fallbackCreateOrder")
    public Order createOrder(OrderRequest request) {
        return orderRepository.save(convert(request));
    }
    
    public Order fallbackCreateOrder(OrderRequest request, Exception ex) {
        log.error("Failed to create order after retries", ex);
        return createOrderManually(request);
    }
}
```

### Configuration

```yaml
resilience4j:
  retry:
    instances:
      orderService:
        maxAttempts: 3
        waitDuration: 1000
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - java.lang.IllegalArgumentException
```

### Retry Flow

```
┌─────────────────────────────────────────────────────────┐
│              Retry Pattern Flow                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Request                                                 │
│     │                                                    │
│     ▼                                                    │
│  Attempt 1 ───> Failure                                 │
│     │                                                    │
│     ▼ (Wait)                                             │
│  Attempt 2 ───> Failure                                 │
│     │                                                    │
│     ▼ (Wait)                                             │
│  Attempt 3 ───> Success                                 │
│     │                                                    │
│     ▼                                                    │
│  Return Result                                           │
│                                                          │
│  If all attempts fail:                                  │
│     ▼                                                    │
│  Fallback / Throw Exception                             │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 9.3 Circuit Breaker

### Purpose
**Definition:** Prevents cascading failures by stopping requests to failing service.

### States

```
┌─────────────────────────────────────────────────────────┐
│            Circuit Breaker States                        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  CLOSED (Normal)                                        │
│  ┌──────────────┐                                       │
│  │ Requests    │                                       │
│  │ flow through│                                       │
│  └──────┬──────┘                                       │
│         │                                               │
│         │ Failure rate > threshold                     │
│         ▼                                               │
│  OPEN (Failing)                                        │
│  ┌──────────────┐                                       │
│  │ Requests     │                                       │
│  │ rejected     │                                       │
│  │ immediately  │                                       │
│  └──────┬──────┘                                       │
│         │                                               │
│         │ After waitDuration                           │
│         ▼                                               │
│  HALF_OPEN (Testing)                                   │
│  ┌──────────────┐                                       │
│  │ Allow limited│                                       │
│  │ requests     │                                       │
│  └──┬───────┬───┘                                       │
│     │       │                                           │
│  Success│   │Failure                                    │
│     │       │                                           │
│     ▼       ▼                                           │
│  CLOSED   OPEN                                          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Implementation

```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackProcess")
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentGateway.charge(request);
    }
    
    public PaymentResult fallbackProcess(PaymentRequest request, Exception ex) {
        log.error("Circuit breaker open, using fallback", ex);
        return PaymentResult.pending(); // Graceful degradation
    }
}
```

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
```

## 9.4 Rate Limiting

### Purpose
**Definition:** Limit number of requests per time period.

### Implementation

```java
@RestController
public class OrderController {
    
    @RateLimiter(name = "orderApi")
    @GetMapping("/api/orders")
    public ResponseEntity<List<Order>> getOrders() {
        return ResponseEntity.ok(orderService.findAll());
    }
}
```

### Configuration

```yaml
resilience4j:
  ratelimiter:
    instances:
      orderApi:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0
```

## 9.5 Bulkhead

### Purpose
**Definition:** Isolate resources to prevent one failure from affecting others.

### Implementation

```java
@Service
public class OrderService {
    
    @Bulkhead(name = "orderProcessing", type = BulkheadType.THREADPOOL)
    public CompletableFuture<Order> processOrderAsync(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            return processOrder(request);
        });
    }
}
```

### Configuration

```yaml
resilience4j:
  bulkhead:
    instances:
      orderProcessing:
        maxConcurrentCalls: 10
        maxWaitDuration: 0
```

## 9.6 Graceful Degradation

### Purpose
**Definition:** Provide reduced functionality when service is unavailable.

### Example

```java
@Service
public class OrderService {
    private final RecommendationService recommendationService;
    
    public Order createOrder(OrderRequest request) {
        Order order = // create order
        
        // Try to get recommendations, but don't fail if unavailable
        try {
            List<Product> recommendations = recommendationService
                .getRecommendations(request.getCustomerId());
            order.setRecommendedProducts(recommendations);
        } catch (Exception e) {
            log.warn("Recommendations unavailable, continuing without them", e);
            // Continue without recommendations
        }
        
        return order;
    }
}
```

## 9.7 Resilience Pattern Flow

```
┌─────────────────────────────────────────────────────────┐
│          Resilience Pattern Flow                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Request                                                 │
│     │                                                    │
│     ▼                                                    │
│  Rate Limiter ───> Reject if over limit                │
│     │                                                    │
│     ▼                                                    │
│  Circuit Breaker ───> Reject if circuit open           │
│     │                                                    │
│     ▼                                                    │
│  Bulkhead ───> Reject if pool exhausted                 │
│     │                                                    │
│     ▼                                                    │
│  Retry ───> Retry on failure                           │
│     │                                                    │
│     ▼                                                    │
│  Timeout ───> Fail if timeout                           │
│     │                                                    │
│     ▼                                                    │
│  Fallback ───> Return fallback response                 │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 9.8 Interview Questions & Answers

### Q1: What is the circuit breaker pattern?
**Answer:**
- Prevents cascading failures by stopping requests to failing service
- **States:** CLOSED (normal), OPEN (failing), HALF_OPEN (testing)
- **Transitions:** CLOSED → OPEN (on high failure rate), OPEN → HALF_OPEN (after wait), HALF_OPEN → CLOSED/OPEN (based on test results)

### Q2: How do you implement retry logic?
**Answer:**
- **Resilience4j @Retry:** Automatic retry with configuration
- **Configuration:** maxAttempts, waitDuration, retryExceptions
- **Best practices:** Exponential backoff, jitter, limit retries
- **Idempotency:** Ensure operations are idempotent

### Q3: What is the difference between timeout and circuit breaker?
**Answer:**
- **Timeout:** Maximum wait time for single request
- **Circuit Breaker:** Stops all requests when service is failing
- **Use timeout:** For individual request protection
- **Use circuit breaker:** For service-level protection

### Q4: Explain rate limiting
**Answer:**
- Limits number of requests per time period
- **Purpose:** Protect services from overload
- **Implementation:** Token bucket, sliding window
- **Configuration:** limitForPeriod, limitRefreshPeriod

### Q5: What is graceful degradation?
**Answer:**
- Provide reduced functionality when service unavailable
- **Example:** Show cached data when live data unavailable
- **Benefit:** Better user experience than complete failure
- **Implementation:** Try-catch with fallback, circuit breaker with fallback

---

*[Section 9 Complete - Continuing to Section 10...]*

---

# SECTION 10: SECURITY

## 10.1 Authentication vs Authorization

### Authentication
**Definition:** Verifying who the user is.

**Methods:**
- Username/password
- JWT tokens
- OAuth2
- API keys

### Authorization
**Definition:** Determining what the user can do.

**Methods:**
- Role-Based Access Control (RBAC)
- Permission-based
- Resource-based

## 10.2 OAuth2 Flows

### Authorization Code Flow

```
┌─────────────────────────────────────────────────────────┐
│          OAuth2 Authorization Code Flow                 │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Client ───> Authorization Server                       │
│  (Redirect to login)                                    │
│                                                          │
│  User logs in                                           │
│                                                          │
│  Authorization Server ───> Client                      │
│  (Redirect with code)                                   │
│                                                          │
│  Client ───> Authorization Server                       │
│  (Exchange code for token)                              │
│                                                          │
│  Authorization Server ───> Client                      │
│  (Access token + Refresh token)                         │
│                                                          │
│  Client ───> Resource Server                            │
│  (Request with access token)                            │
│                                                          │
│  Resource Server ───> Client                            │
│  (Protected resource)                                   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Client Credentials Flow (Service-to-Service)

```java
@Configuration
public class OAuth2Config {
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService() {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository());
    }
}
```

## 10.3 JWT Internals

### JWT Structure

```
Header.Payload.Signature

Header: {"alg":"HS256","typ":"JWT"}
Payload: {"sub":"user123","roles":["USER","ADMIN"],"exp":1234567890}
Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

### JWT Claims

- **iss (issuer):** Who issued the token
- **sub (subject):** User ID
- **aud (audience):** Intended recipient
- **exp (expiration):** Expiration time
- **iat (issued at):** When token was issued
- **roles:** User roles (custom claim)

### JWT Authentication Flow

```
┌─────────────────────────────────────────────────────────┐
│              JWT Authentication Flow                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. User Login                                          │
│     Client ───> Auth Server                             │
│     (username, password)                                │
│                                                          │
│  2. Token Generation                                    │
│     Auth Server creates JWT                             │
│     (Header + Payload + Signature)                      │
│                                                          │
│  3. Token Return                                        │
│     Auth Server ───> Client                             │
│     (JWT token)                                         │
│                                                          │
│  4. Resource Request                                    │
│     Client ───> Resource Server                         │
│     (Request + JWT in Authorization header)             │
│                                                          │
│  5. Token Validation                                    │
│     Resource Server validates:                          │
│     - Signature                                         │
│     - Expiration                                        │
│     - Claims                                            │
│                                                          │
│  6. Resource Response                                   │
│     Resource Server ───> Client                         │
│     (Protected resource)                                │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 10.4 Spring Security Filter Chain

### Filter Chain Order

```
Request → SecurityContextPersistenceFilter
       → UsernamePasswordAuthenticationFilter
       → BasicAuthenticationFilter
       → JwtAuthenticationFilter
       → AuthorizationFilter
       → ExceptionTranslationFilter
       → FilterSecurityInterceptor
       → Controller
```

### Custom JWT Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        String token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            Authentication auth = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

## 10.5 CORS & CSRF

### CORS (Cross-Origin Resource Sharing)

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### CSRF (Cross-Site Request Forgery)

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers("/api/public/**")
        );
        return http.build();
    }
}
```

## 10.6 Secure Coding Practices

1. **Input Validation:** Validate all inputs
2. **SQL Injection Prevention:** Use parameterized queries
3. **XSS Prevention:** Sanitize user input
4. **Password Hashing:** Use BCrypt, never store plaintext
5. **HTTPS:** Always use HTTPS in production
6. **Secrets Management:** Never commit secrets to code
7. **Least Privilege:** Grant minimum required permissions

## 10.7 Interview Questions & Answers

### Q1: Explain the difference between authentication and authorization
**Answer:**
- **Authentication:** Verifying identity (who you are)
- **Authorization:** Verifying permissions (what you can do)
- **Example:** Login = authentication, accessing admin panel = authorization

### Q2: How does JWT work?
**Answer:**
- **Structure:** Header.Payload.Signature
- **Process:** User logs in → receives JWT → sends JWT with requests → server validates
- **Benefits:** Stateless, scalable, no server-side session storage
- **Security:** Signature ensures token integrity, expiration prevents long-lived tokens

### Q3: What is OAuth2?
**Answer:**
- Authorization framework for delegated access
- **Flows:** Authorization Code (web), Client Credentials (service-to-service), Implicit (deprecated)
- **Components:** Resource Owner, Client, Authorization Server, Resource Server
- **Use case:** Allow third-party apps to access user resources

### Q4: How do you prevent CSRF attacks?
**Answer:**
- **CSRF Token:** Include token in requests, validate on server
- **SameSite Cookies:** Prevent cross-site cookie sending
- **Double Submit Cookie:** Token in cookie and form
- **Spring Security:** Automatic CSRF protection

### Q5: What are best practices for password storage?
**Answer:**
- **Never store plaintext:** Always hash passwords
- **Use BCrypt/Argon2:** Strong hashing algorithms
- **Salt:** Unique salt per password
- **Work factor:** Adjustable cost factor for BCrypt
- **Never use MD5/SHA1:** Too fast, vulnerable to brute force

---

*[Section 10 Complete - Continuing to Section 11...]*

---

# SECTION 11: TESTING STRATEGY

## 11.1 Unit Testing

### Purpose
**Definition:** Testing individual components in isolation.

### JUnit 5 Example

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void shouldCreateOrder() {
        // Given
        OrderRequest request = new OrderRequest(1L, BigDecimal.valueOf(100));
        Order expectedOrder = new Order(1L, BigDecimal.valueOf(100));
        
        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);
        
        // When
        Order result = orderService.createOrder(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(100));
        verify(orderRepository).save(any(Order.class));
    }
}
```

## 11.2 Integration Testing

### Purpose
**Definition:** Testing components together with real dependencies.

### Spring Boot Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void shouldCreateOrder() throws Exception {
        OrderRequest request = new OrderRequest(1L, BigDecimal.valueOf(100));
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(100));
    }
}
```

## 11.3 Mockito

### Mocking

```java
@Mock
private OrderRepository orderRepository;

// Stubbing
when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

// Verification
verify(orderRepository).save(order);
verify(orderRepository, times(2)).findAll();
verify(orderRepository, never()).delete(any());
```

## 11.4 Testcontainers

### Purpose
**Definition:** Integration tests with real databases in Docker containers.

### Example

```java
@SpringBootTest
@Testcontainers
class OrderRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void shouldSaveOrder() {
        Order order = new Order(1L, BigDecimal.valueOf(100));
        orderRepository.save(order);
        
        assertThat(orderRepository.findById(1L)).isPresent();
    }
}
```

## 11.5 Interview Questions & Answers

### Q1: What is the difference between unit and integration tests?
**Answer:**
- **Unit test:** Tests component in isolation with mocks
- **Integration test:** Tests components together with real dependencies
- **Unit test:** Fast, isolated, many tests
- **Integration test:** Slower, tests interactions, fewer tests

### Q2: How do you test Spring Boot applications?
**Answer:**
- **@SpringBootTest:** Full application context
- **@WebMvcTest:** Only web layer
- **@DataJpaTest:** Only data layer
- **MockMvc:** Test REST controllers
- **Testcontainers:** Real databases in tests

### Q3: Explain Mockito
**Answer:**
- Mocking framework for Java
- **@Mock:** Create mock object
- **when().thenReturn():** Stub behavior
- **verify():** Verify interactions
- **@InjectMocks:** Inject mocks into class under test

---

*[Section 11 Complete - Continuing to Section 12...]*

---

# SECTION 12: CI/CD & DEVOPS

## 12.1 Maven Lifecycle

### Phases

1. **validate:** Validate project
2. **compile:** Compile source code
3. **test:** Run unit tests
4. **package:** Create JAR/WAR
5. **verify:** Run integration tests
6. **install:** Install to local repository
7. **deploy:** Deploy to remote repository

## 12.2 CI Pipeline Stages

```
┌─────────────────────────────────────────────────────────┐
│              CI/CD Pipeline Stages                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Source Control                                      │
│     ┌──────────────┐                                    │
│     │ Git Push     │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  2. Build                                              │
│     ┌──────────────┐                                    │
│     │ mvn clean    │                                    │
│     │ package      │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  3. Test                                               │
│     ┌──────────────┐                                    │
│     │ Unit Tests   │                                    │
│     │ Integration │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  4. Code Quality                                       │
│     ┌──────────────┐                                    │
│     │ SonarQube    │                                    │
│     │ Code Coverage│                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  5. Security Scan                                      │
│     ┌──────────────┐                                    │
│     │ OWASP        │                                    │
│     │ Dependency   │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  6. Build Docker Image                                 │
│     ┌──────────────┐                                    │
│     │ docker build │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  7. Deploy to Staging                                  │
│     ┌──────────────┐                                    │
│     │ Kubernetes   │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  8. E2E Tests                                          │
│     ┌──────────────┐                                    │
│     │ Integration │                                    │
│     │ Tests        │                                    │
│     └──────┬───────┘                                    │
│            │                                            │
│            ▼                                            │
│  9. Deploy to Production                               │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 12.3 Docker

### Multi-Stage Build

```dockerfile
# Stage 1: Build
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 12.4 Kubernetes

### Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: order-service
spec:
  containers:
  - name: order-service
    image: order-service:1.0.0
    ports:
    - containerPort: 8080
```

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
      - name: order-service
        image: order-service:1.0.0
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

### Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: order-ingress
spec:
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /api/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 80
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-config
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://db:5432/orders
```

### Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  username: dXNlcm5hbWU=  # base64 encoded
  password: cGFzc3dvcmQ=  # base64 encoded
```

### Probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

### HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## 12.5 Rolling Deployments

### Strategy

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### Process

1. Create new pod with new version
2. Wait for new pod to be ready
3. Remove old pod
4. Repeat until all pods updated

## 12.6 Rollbacks

```bash
# Rollback to previous version
kubectl rollout undo deployment/order-service

# Rollback to specific revision
kubectl rollout undo deployment/order-service --to-revision=2

# View rollout history
kubectl rollout history deployment/order-service
```

## 12.7 Observability

### Metrics (Prometheus)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Logging (Loki)

```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG
```

### Tracing (Zipkin)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

## 12.8 Interview Questions & Answers

### Q1: Explain Kubernetes components
**Answer:**
- **Pod:** Smallest deployable unit (container)
- **Deployment:** Manages pod replicas
- **Service:** Exposes pods (load balancing)
- **Ingress:** External access to services
- **ConfigMap:** Configuration data
- **Secret:** Sensitive data
- **HPA:** Auto-scaling based on metrics

### Q2: How do you handle secrets in Kubernetes?
**Answer:**
- **Secrets:** Base64 encoded (not encrypted)
- **External Secrets Operator:** Sync from external systems
- **Vault:** HashiCorp Vault integration
- **Best practice:** Never commit secrets, use secret management tools

### Q3: What is the difference between liveness and readiness probes?
**Answer:**
- **Liveness:** Is container running? (restarts if fails)
- **Readiness:** Is container ready? (removes from service if fails)
- **Use liveness:** Detect deadlocks, restart container
- **Use readiness:** Wait for dependencies, gradual startup

### Q4: Explain rolling deployment
**Answer:**
- Gradually replace old pods with new pods
- **Benefits:** Zero downtime, gradual rollout
- **Configuration:** maxSurge, maxUnavailable
- **Rollback:** Easy to revert if issues

### Q5: How do you monitor applications in Kubernetes?
**Answer:**
- **Metrics:** Prometheus + Grafana
- **Logging:** Loki + Grafana
- **Tracing:** Zipkin/Jaeger
- **Health:** Actuator endpoints
- **Alerts:** AlertManager

---

*[Section 12 Complete - Continuing to Section 13...]*

---

# SECTION 13: SYSTEM DESIGN

## 13.1 Async Report Generation System

### Requirements
- Generate reports asynchronously
- Support multiple report types
- Handle large datasets
- Email reports when complete
- Track report status

### APIs

```
POST /api/reports - Create report request
GET /api/reports/{id} - Get report status
GET /api/reports/{id}/download - Download report
```

### Data Flow

```
┌─────────────────────────────────────────────────────────┐
│        Async Report Generation System                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Client ───> API Gateway                               │
│  (POST /api/reports)                                    │
│                                                          │
│  API Gateway ───> Report Service                        │
│  (Create report request)                                │
│                                                          │
│  Report Service ───> Kafka                             │
│  (Publish report-request event)                         │
│                                                          │
│  Report Service ───> Database                           │
│  (Store report status: PENDING)                         │
│                                                          │
│  Report Service ───> Client                            │
│  (Return report ID)                                     │
│                                                          │
│  Report Worker ───> Kafka                              │
│  (Consume report-request)                              │
│                                                          │
│  Report Worker ───> Database                           │
│  (Query data, generate report)                          │
│                                                          │
│  Report Worker ───> Object Storage (S3)                │
│  (Upload generated report)                               │
│                                                          │
│  Report Worker ───> Database                            │
│  (Update status: COMPLETED, store S3 URL)              │
│                                                          │
│  Report Worker ───> Email Service                       │
│  (Send email with download link)                        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Failure Handling
- **Retry:** Failed report generation retried
- **DLQ:** Failed reports moved to DLQ for manual review
- **Timeout:** Reports timeout after 1 hour
- **Status:** Track PENDING, PROCESSING, COMPLETED, FAILED

### Scaling
- **Horizontal:** Multiple report workers
- **Partitioning:** Partition by report type
- **Caching:** Cache frequently generated reports

## 13.2 Order / Payment System

### Requirements
- Create orders
- Process payments
- Update inventory
- Send notifications
- Handle failures

### APIs

```
POST /api/orders - Create order
GET /api/orders/{id} - Get order
POST /api/orders/{id}/pay - Process payment
POST /api/orders/{id}/cancel - Cancel order
```

### Data Flow

```
┌─────────────────────────────────────────────────────────┐
│            Order/Payment System Flow                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. Create Order                                        │
│     Client ───> Order Service                           │
│     (POST /api/orders)                                  │
│                                                          │
│  2. Validate & Reserve Inventory                       │
│     Order Service ───> Inventory Service                │
│     (Reserve items)                                     │
│                                                          │
│  3. Create Order Record                                │
│     Order Service ───> Database                         │
│     (Status: PENDING_PAYMENT)                          │
│                                                          │
│  4. Process Payment                                    │
│     Client ───> Payment Service                        │
│     (POST /api/orders/{id}/pay)                        │
│                                                          │
│  5. Payment Gateway                                     │
│     Payment Service ───> Payment Gateway                │
│     (Charge credit card)                                │
│                                                          │
│  6. Update Order                                        │
│     Payment Service ───> Order Service                 │
│     (Update status: CONFIRMED)                          │
│                                                          │
│  7. Confirm Inventory                                  │
│     Order Service ───> Inventory Service                │
│     (Confirm reservation)                               │
│                                                          │
│  8. Publish Events                                      │
│     Order Service ───> Kafka                            │
│     (OrderConfirmed event)                              │
│                                                          │
│  9. Notifications                                      │
│     Notification Service ───> Kafka                    │
│     (Consume OrderConfirmed)                            │
│     ───> Email Service                                  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Failure Handling
- **Saga Pattern:** Compensate on failure
- **Payment fails:** Release inventory, cancel order
- **Inventory fails:** Refund payment, cancel order
- **Retry:** Retry transient failures

### Scaling
- **Order Service:** Scale based on order volume
- **Payment Service:** Scale based on payment requests
- **Database:** Read replicas for queries
- **Kafka:** Partition by order ID

## 13.3 Notification System

### Requirements
- Send emails, SMS, push notifications
- Support multiple channels
- Retry failed notifications
- Rate limiting per channel
- Template support

### APIs

```
POST /api/notifications - Send notification
GET /api/notifications/{id} - Get status
```

### Data Flow

```
┌─────────────────────────────────────────────────────────┐
│            Notification System Flow                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Event Source ───> Kafka                                │
│  (OrderCreated, PaymentProcessed, etc.)                │
│                                                          │
│  Notification Service ───> Kafka                       │
│  (Consume events)                                       │
│                                                          │
│  Notification Service ───> Template Engine             │
│  (Render notification template)                        │
│                                                          │
│  Notification Service ───> Channel Router              │
│  (Route to appropriate channel)                         │
│                                                          │
│  Channel Handlers:                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│
│  │ Email        │  │ SMS          │  │ Push         ││
│  │ Handler      │  │ Handler      │  │ Handler      ││
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘│
│         │                 │                 │          │
│         ▼                 ▼                 ▼          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│
│  │ Email        │  │ SMS          │  │ Push         ││
│  │ Service      │  │ Service      │  │ Service      ││
│  └──────────────┘  └──────────────┘  └──────────────┘│
│                                                          │
│  Notification Service ───> Database                     │
│  (Store notification status)                            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Failure Handling
- **Retry:** Exponential backoff
- **DLQ:** Failed notifications after max retries
- **Rate Limiting:** Per channel to avoid throttling
- **Circuit Breaker:** Stop sending if channel down

### Scaling
- **Partitioning:** By notification type
- **Workers:** Multiple workers per channel
- **Caching:** Template caching
- **Database:** Separate read/write databases

## 13.4 Interview Questions & Answers

### Q1: How do you design a scalable notification system?
**Answer:**
- **Event-driven:** Kafka for event consumption
- **Channel abstraction:** Separate handlers per channel
- **Rate limiting:** Per channel to avoid throttling
- **Retry mechanism:** Exponential backoff
- **Monitoring:** Track success/failure rates
- **Scaling:** Horizontal scaling of workers

### Q2: How do you ensure data consistency in distributed systems?
**Answer:**
- **Saga Pattern:** Distributed transactions with compensation
- **Eventual Consistency:** Accept temporary inconsistency
- **Idempotency:** Make operations idempotent
- **Event Sourcing:** Store events, rebuild state
- **Two-Phase Commit:** For strong consistency (rarely used)

### Q3: How do you handle high traffic in system design?
**Answer:**
- **Load Balancing:** Distribute traffic
- **Caching:** Redis for frequently accessed data
- **CDN:** For static content
- **Database:** Read replicas, sharding
- **Async Processing:** Queue heavy operations
- **Auto-scaling:** Scale based on load

---

*[Section 13 Complete - Continuing to Section 14...]*

---

# SECTION 14: REAL INTERVIEW QUESTIONS

## 14.1 Publicis Sapient-Style Questions

### Q1: Design a distributed caching system
**Answer:**
- **Requirements:** High availability, low latency, consistency
- **Architecture:** Redis cluster with replication
- **Sharding:** Consistent hashing for key distribution
- **Eviction:** LRU policy
- **Consistency:** Eventual consistency acceptable
- **Failover:** Automatic failover to replicas

### Q2: How would you optimize a slow database query?
**Answer:**
1. **Analyze:** Use EXPLAIN to see execution plan
2. **Indexes:** Add indexes on frequently queried columns
3. **Query optimization:** Avoid SELECT *, use LIMIT
4. **Partitioning:** Partition large tables
5. **Caching:** Cache query results
6. **Read replicas:** Offload read queries

### Q3: Explain how you would implement idempotency in a payment system
**Answer:**
- **Idempotency Key:** Client sends unique key with request
- **Storage:** Store key in database with unique constraint
- **Check:** Before processing, check if key exists
- **Response:** If exists, return previous response
- **Expiration:** Keys expire after reasonable time
- **Implementation:** Database unique constraint + Redis for fast lookup

### Q4: How do you handle database migrations in production?
**Answer:**
- **Flyway/Liquibase:** Version-controlled migrations
- **Backward compatibility:** Maintain backward compatibility during migration
- **Rollback plan:** Always have rollback script
- **Testing:** Test on staging first
- **Gradual rollout:** Migrate in phases
- **Monitoring:** Monitor application after migration

### Q5: Design a system to handle 1 million requests per second
**Answer:**
- **Load Balancing:** Multiple load balancers
- **API Gateway:** Rate limiting, caching
- **Microservices:** Scale services independently
- **Database:** Sharding, read replicas, caching
- **Message Queue:** Async processing for heavy operations
- **CDN:** For static content
- **Auto-scaling:** Scale based on metrics
- **Monitoring:** Real-time monitoring and alerts

## 14.2 Common Mistakes to Avoid

1. **Not asking clarifying questions:** Always clarify requirements
2. **Jumping to solution:** Think through problem first
3. **Ignoring scalability:** Consider scale from start
4. **Forgetting failure cases:** Design for failures
5. **Not considering consistency:** Think about data consistency
6. **Over-engineering:** Keep it simple, add complexity only when needed

## 14.3 Follow-up Questions

### After explaining a concept:
- "Can you give a real-world example?"
- "What are the trade-offs?"
- "How would you implement this?"
- "What are the failure scenarios?"

### When stuck:
- "Let me think through this step by step"
- "I'm not sure, but here's my approach"
- "Can you give me a hint?"

---

# CONCLUSION

This handbook covers all essential topics for a Senior Java Backend Developer interview at Publicis Sapient. Key areas:

1. **Core Java:** OOP, Collections, Concurrency
2. **Spring Framework:** IoC, DI, Spring Boot
3. **REST APIs:** Design, versioning, error handling
4. **Databases:** JPA, transactions, optimization
5. **Microservices:** Architecture, communication, patterns
6. **Kafka:** Critical for distributed systems
7. **Resilience:** Circuit breaker, retry, rate limiting
8. **Security:** OAuth2, JWT, Spring Security
9. **Testing:** Unit, integration, Testcontainers
10. **DevOps:** Docker, Kubernetes, CI/CD
11. **System Design:** Real-world scenarios

**Best of luck with your interview!**

---

*End of Handbook*

