# Coding Conventions

## Code Formatting

### Indentation

Use **2 spaces** for indentation in all source files (Java, XML, TOML, etc.).

**Rationale:** Consistent with modern Java conventions and keeps code compact while maintaining readability.

### Brace Style

Use **Allman style** (braces on new lines) for all control structures and class/method declarations.

**Good:**
```java
public class Example
{
  public void method()
  {
    if (condition)
    {
      doSomething();
    }
  }
}
```

**Bad:**
```java
public class Example {
  public void method() {
    if (condition) {
      doSomething();
    }
  }
}
```

**Rationale:** Allman style improves visual clarity and makes code structure easier to scan.

### Avoid Ternary Statements

Prefer explicit if/else blocks over ternary operators for clarity.

**Good:**
```java
String result;
if (condition)
{
  result = "yes";
}
else
{
  result = "no";
}
```

**Bad:**
```java
String result = condition ? "yes" : "no";
```

**Rationale:** Ternary operators reduce readability, especially when nested or combined with complex expressions. Explicit if/else blocks are easier to read, debug, and modify.

## Code Comments

### Avoid Redundant Project References

Comments should focus on what the code does, not redundantly mention the project name.

**Good:**
```java
/**
 * TOML parser for bill.toml configuration files.
 */
```

**Bad:**
```java
/**
 * TOML parser for Bill build system.
 */
```

**Rationale:** Readers already know which project the source code belongs to. Comments should provide useful information about the code's purpose or behavior.

### Avoid Retrospective/Timeline Comments

Comments should describe the current state of the code, not reference when features will be or were implemented.

**Good:**
```java
/**
 * TOML parser for bill.toml configuration files.
 */
package io.github.cowwoc.bill.toml;
```

**Bad:**
```java
/**
 * TOML parser for bill.toml configuration files.
 * <p>
 * Implementation in Phase 2.
 */
package io.github.cowwoc.bill.toml;
```

**Rationale:** Timeline comments become dated immediately and don't reflect the current state of the code. Comments should be timeless and describe what the code does, not when it was or will be implemented.

## Project Structure

### Module Naming

Module directories and artifactIds use simple names without the "bill-" prefix.

**Good:**
- `core/` with artifactId `core`
- `toml/` with artifactId `toml`
- `maven/` with artifactId `maven`

**Bad:**
- `bill-core/` with artifactId `bill-core`
- `bill-toml/` with artifactId `bill-toml`

**Rationale:** The parent POM groupId already identifies these as Bill modules. Shorter names are cleaner.

## Dependency Injection

### Scope-Based IoC Pattern

Use the [pouch](https://github.com/cowwoc/pouch) Inversion of Control pattern for managing dependencies. Scopes represent contexts with specific lifetimes and provide dependencies to classes that need them.

**Core Concepts:**
- **Scope interface**: Defines what dependencies are available (e.g., `JvmScope` provides `getTomlMapper()`)
- **Main implementation**: Production implementation that creates real dependencies (e.g., `MainJvmScope`)
- **Test implementation**: Test implementation that can provide mocks or test-specific configurations

**Good (class receives dependencies via scope):**
```java
public final class BillConfigParser
{
  private final TomlMapper mapper;

  public BillConfigParser(JvmScope scope)
  {
    requireThat(scope, "scope").isNotNull();
    this.mapper = scope.getTomlMapper();
  }
}
```

**Bad (class creates its own dependencies):**
```java
public final class BillConfigParser
{
  private final TomlMapper mapper;

  public BillConfigParser()
  {
    this.mapper = TomlMapper.builder().build();  // Hard to test, configure
  }
}
```

**Scope Hierarchy:**
- `JvmScope`: Application-level values lasting the JVM's lifetime
- Child scopes extend parent scopes, enabling delegation

**Rationale:** Scopes make dependencies explicit, enable testing without mocks, and provide compile-time verification of dependency graphs. No reflection, no magic - just plain Java interfaces.

### Package Naming for Tests

Test packages must have unique names by appending `.test` to the main module's package name.

**Good:**
- Main module: `package io.github.cowwoc.bill.core;`
- Test module: `package io.github.cowwoc.bill.core.test;`

**Good:**
- Main module: `package io.github.cowwoc.bill.toml;`
- Test module: `package io.github.cowwoc.bill.toml.test;`

**Bad:**
- Main module: `package io.github.cowwoc.bill.core;`
- Test module: `package io.github.cowwoc.bill.core;` ❌ Same as main

**Rationale:** Unique package names prevent naming conflicts and make it clear which code is production vs. test code. This follows the convention that test packages should be distinct from their corresponding main packages.

## Build Configuration

### Java Compiler Settings

Use `maven.compiler.release` instead of separate `maven.compiler.source` and `maven.compiler.target` properties.

**Good:**
```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

**Bad:**
```xml
<properties>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
</properties>
```

**Rationale:** `maven.compiler.release` ensures consistent compilation for the target JDK version and is the recommended modern approach.

### Dependency Version Management

All dependency versions (both external and internal/inter-module) must be defined in the root `pom.xml` in the `<dependencyManagement>` section. Child module POMs should never specify versions.

**Good (root pom.xml):**
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.testng</groupId>
      <artifactId>testng</artifactId>
      <version>7.10.2</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.github.cowwoc.bill</groupId>
      <artifactId>core</artifactId>
      <version>${project.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**Good (child module pom.xml):**
```xml
<dependencies>
  <dependency>
    <groupId>io.github.cowwoc.bill</groupId>
    <artifactId>core</artifactId>
  </dependency>
  <dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Bad (child module with versions):**
```xml
<dependencies>
  <dependency>
    <groupId>io.github.cowwoc.bill</groupId>
    <artifactId>core</artifactId>
    <version>${project.version}</version>  <!-- Version should be in parent -->
  </dependency>
</dependencies>
```

**Rationale:** Centralizing all dependency versions in the root POM ensures consistency across modules, simplifies version updates, and follows Maven best practices.

## Git Commits

### Commit Message Format

Use full conventional commit types without scope identifiers.

**Format:** `{type}: {description}`

**Good:**
- `feature: create toml module skeleton`
- `docs: update roadmap for Phase 2`
- `refactor: simplify dependency resolution`

**Bad:**
- `feat(01-02): create toml module skeleton`
- `docs(phase-2): update roadmap`

**Types:**
- `feature:` - New features or functionality
- `fix:` - Bug fixes
- `refactor:` - Code changes that neither fix bugs nor add features
- `docs:` - Documentation changes
- `test:` - Test additions or modifications
- `chore:` - Maintenance tasks, dependency updates, tooling

**Rationale:** Simpler format, easier to read in git log, consistent with project preferences.

## Data Modeling

### Prefer Empty Over Null

For optional string fields, prefer empty strings over null when possible.

**Good:**
```java
public record ProjectInfo(
  String name,
  String version,
  String description)  // Empty string "" if not specified
{
  public ProjectInfo
  {
    requireThat(name, "name").isNotBlank();
    requireThat(version, "version").isNotBlank();
    if (description == null)
    {
      description = "";
    }
  }
}
```

**Bad:**
```java
public record ProjectInfo(
  String name,
  String version,
  String description)  // null if not specified - forces null checks everywhere
{
}
```

**Rationale:** Empty strings reduce null checks throughout the codebase. Callers can simply use the value without checking for null first. This makes code cleaner and reduces NullPointerException risks.

**When to use null instead:**
- When the field represents a complex object (not a string)
- When absence has different semantic meaning than "empty"
- When the field is truly optional and absence should be explicit

### Serialization Without Annotations

Avoid Jackson annotations on data classes. Instead:
1. Add explicit conversion methods (`fromToml()`/`toToml()`) to enums and classes
2. Register custom serializers/deserializers in the scope that provides the mapper

**Good (enum with conversion methods, no annotations):**
```java
import static java.util.Locale.ROOT;

public enum DependencyScope
{
  COMPILE,
  TEST,
  RUNTIME,
  PROVIDED;

  public String toToml()
  {
    return name().toLowerCase(ROOT);
  }

  public static DependencyScope fromToml(String toml)
  {
    return valueOf(toml.toUpperCase(ROOT));
  }
}
```

**Good (register serializer in scope):**
```java
public final class MainJvmScope implements JvmScope
{
  public MainJvmScope()
  {
    SimpleModule module = new SimpleModule();
    module.addSerializer(DependencyScope.class, new DependencyScopeSerializer());
    module.addDeserializer(DependencyScope.class, new DependencyScopeDeserializer());

    this.tomlMapper = TomlMapper.builder()
      .addModule(module)
      .build();
  }
}
```

**Bad (annotations scattered on data class):**
```java
public enum DependencyScope
{
  @JsonProperty("compile")
  COMPILE,

  @JsonProperty("test")
  TEST
}
```

**Rationale:** Keeping data classes annotation-free makes them framework-agnostic. Centralizing serialization logic in the scope makes it easier to maintain and test.

## Validation

### Runtime Validation with requirements.java

Use [requirements.java](https://github.com/cowwoc/requirements.java) (version 13.1) for all runtime validations instead of manual if/throw checks.

**Import:**
```java
import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.*;
```

**Dependency (defined in root pom.xml):**
```xml
<dependency>
  <groupId>io.github.cowwoc.requirements</groupId>
  <artifactId>requirements-java</artifactId>
  <version>13.1</version>
</dependency>
```

**Entry points:**
- `requireThat(value, name)` - For method preconditions (throws IllegalArgumentException immediately)
- `that(value, name)` - For assertions, class invariants, postconditions (throws AssertionError)
- `checkIf(value, name)` - For collecting multiple failures (returns List<String> via elseGetFailures())

**Use checkIf() for error accumulation:**
```java
// Accumulate all validation errors
List<String> errors = checkIf(name, "project.name").isNotBlank()
  .and(checkIf(version, "project.version").isNotBlank())
  .and(checkIf(version, "project.version").matches(MAVEN_VERSION_PATTERN))
  .elseGetFailures();

if (!errors.isEmpty())
{
  throw new ConfigParseException("Invalid configuration", errors);
}
```

**Common validation methods:**
- Strings: `isNotBlank()`, `isNotEmpty()`, `matches(String regex)`, `matches(Pattern pattern)`, `isTrimmed()`
- Objects: `isNotNull()`, `isEqualTo()`, `isInstanceOf()`
- Collections: `isEmpty()`, `isNotEmpty()`, `size().*`
- Numbers: `isPositive()`, `isNotNegative()`, `isGreaterThan()`, etc.
- Paths: `exists()`, `isDirectory()`, `isRegularFile()`, `isAbsolute()`, `isRelative()`

### Path Existence Checks

Use `requireThat(path, "path").exists()` instead of `path.toFile().exists()`. When a custom exception type is needed, catch and translate the `IllegalArgumentException`.

**Good:**
```java
public BillConfig parse(Path path) throws ConfigParseException, IOException
{
  requireThat(path, "path").isNotNull();
  try
  {
    requireThat(path, "path").exists();
  }
  catch (IllegalArgumentException e)
  {
    throw new ConfigParseException(e.getMessage(), e);
  }
  // ...
}
```

**Bad:**
```java
public BillConfig parse(Path path) throws ConfigParseException, IOException
{
  requireThat(path, "path").isNotNull();
  if (!path.toFile().exists())
  {
    throw new ConfigParseException("File not found: " + path);
  }
  // ...
}
```

**Rationale:** requirements.java provides consistent error messages and integrates path validation with other precondition checks. The `exists()` method works directly on `Path` without needing conversion to `File`.

**Rationale:** requirements.java provides automatic error message generation, fluent API for readability, and built-in error accumulation. It's more maintainable than manual if/throw checks and produces consistent, helpful error messages.

### Test Assertions

In test code, prefer requirements.java first, then fall back to TestNG assertions if functionality doesn't exist.

**Prefer (requirements.java in tests):**
```java
requireThat(config.name(), "name").isEqualTo("my-app");
requireThat(config.dependencies(), "dependencies").isNotEmpty();
```

**Fall back to TestNG if needed:**
```java
import static org.testng.Assert.*;
assertEquals(config.name(), "my-app");
assertNotNull(config.dependencies());
```

**Rationale:** Consistent validation style across production and test code. requirements.java provides better error messages than TestNG's assertEquals. Only use TestNG for features that requirements.java doesn't support.

## Testing

### Test-Driven Development (TDD)

**Default approach:** Use TDD (Red-Green-Refactor) for all functionality implementation.

**TDD Process:**
1. **RED:** Write a failing test that specifies desired behavior
2. **GREEN:** Write minimal code to make the test pass
3. **REFACTOR:** Clean up code while keeping tests green

**When to use TDD (default for most code):**
- Business logic with defined inputs/outputs
- Data transformations, parsing, formatting
- Validation rules and constraints
- Algorithms with testable behavior
- API endpoints and public interfaces
- State management and workflows

**When TDD is optional:**
- Simple data structures (POJOs, records with no logic)
- Pure configuration (no behavior to test)
- Trivial getters/setters
- Generated code

### Test Method Documentation

Test methods must have Javadoc explaining what behavior is being tested.

**Good:**
```java
/**
 * Parses a minimal bill.toml with only required fields.
 */
@Test
public void parseMinimalConfig() throws ConfigParseException, IOException
{
  // ...
}

/**
 * Parsing a non-existent file throws {@code ConfigParseException}.
 */
@Test(expectedExceptions = ConfigParseException.class)
public void parseNonExistentFile() throws ConfigParseException, IOException
{
  // ...
}
```

**Bad:**
```java
@Test
public void parseMinimalConfig() throws ConfigParseException, IOException
{
  // No documentation - unclear what behavior is being verified
}
```

**Rationale:** Test documentation serves as living specification. It clarifies what behavior is expected and helps future developers understand the test's purpose without reading the implementation.

### Thread-Safety and Parallel Execution

Tests must be thread-safe and run in parallel. Each test should be self-contained.

**Requirements:**
- Test classes and methods must be thread-safe
- Tests run in parallel by default
- Tests should create their own input data rather than reading shared files from disk
- Prefer in-memory data over disk I/O when possible

**Good (self-contained, in-memory input):**
```java
/**
 * Parses a minimal bill.toml with only required fields.
 */
@Test
public void parseMinimalConfig() throws ConfigParseException, IOException
{
  String toml = """
    [project]
    name = "my-app"
    version = "1.0.0"
    """;
  Path path = Files.writeString(Files.createTempFile("bill", ".toml"), toml);
  try
  {
    BillConfig config = parser.parse(path);
    requireThat(config.project().name(), "name").isEqualTo("my-app");
  }
  finally
  {
    Files.deleteIfExists(path);
  }
}
```

**Bad (shared file dependency, not thread-safe):**
```java
private static final Path SHARED_CONFIG = Path.of("src/test/resources/test-config.toml");

@Test
public void parseMinimalConfig() throws ConfigParseException, IOException
{
  // Multiple tests sharing the same file - not thread-safe
  BillConfig config = parser.parse(SHARED_CONFIG);
}
```

**Rationale:** Parallel test execution improves build speed. Self-contained tests avoid race conditions and flaky failures. In-memory data is faster and more reliable than disk I/O.

**Test Coverage Philosophy**

Focus on **business requirements coverage**, not implementation-details coverage.

**What to test (business requirements):**
- "Given valid input, returns expected output"
- "Given invalid input, reports clear error"
- "Handles edge cases correctly (empty, null, boundary values)"
- "Meets functional requirements from specification"

**What NOT to test (implementation details):**
- "Method X calls method Y"
- "Uses library Z internally"
- "Field A is set before field B"
- "Code coverage percentage" (useful metric, but not the goal)

**Good (requirements coverage):**
```java
// Tests business requirement: "Parser handles valid TOML"
@Test
public void parseValidBillToml()
{
  BillConfig config = parser.parse(validTomlPath);
  requireThat(config.project().name(), "name").isEqualTo("my-app");
  requireThat(config.project().version(), "version").isEqualTo("1.0.0");
}

// Tests business requirement: "Parser reports all errors at once"
@Test
public void parseInvalidTomlShowsAllErrors()
{
  ConfigParseException e = assertThrows(() -> parser.parse(invalidTomlPath));
  requireThat(e.getErrors(), "errors").isNotEmpty();
  requireThat(e.getErrors().size(), "error count").isGreaterThan(1);
}
```

**Bad (implementation details):**
```java
// Don't test how the code works internally
@Test
public void parserCallsJacksonReadValue()
{
  verify(mockMapper).readValue(any(), eq(BillConfig.class)); // Brittle
}

@Test
public void parserSetsFieldsInCorrectOrder()
{
  // Testing internal sequencing, not requirements
  InOrder order = inOrder(config);
  order.verify(config).setName(any());
  order.verify(config).setVersion(any());
}
```

**Coverage Metrics:**
- **Target:** 100% business requirements coverage (every requirement has tests)
- **Not a target:** 100% line/branch coverage (useful signal, but not the goal)
- **Philosophy:** If a business requirement exists, it must have tests. If code has no tests, question if it's a real requirement.

**Rationale:**
- Tests that verify business requirements survive refactoring and provide long-term value
- Tests of implementation details break when you change how code works (even when behavior is correct)
- Requirements-focused tests serve as living documentation of what the system does
- Implementation-detail tests create maintenance burden without safety benefit

## Documentation

### Package Documentation

Package-level documentation (package-info.java) should describe the package's purpose without redundant project references.

### Inline Comments

Use comments sparingly. Prefer self-documenting code with clear variable and method names. Add comments when:
- The logic is complex or non-obvious
- There's a specific reason for an implementation choice
- Edge cases need explanation

### Javadoc

Use `{@code}` to surround identifiers in Javadoc comments. This applies to:
- Parameter names
- Method names
- Class names
- Field names
- Literal values (strings, numbers)
- TOML section names, keys, etc.

**Good:**
```java
/**
 * Parses a bill.toml file into a {@code BillConfig} object.
 *
 * @param path the path to the bill.toml file
 * @return the parsed configuration
 * @throws NullPointerException if {@code path} is {@code null}
 * @throws ConfigParseException if {@code path} does not exist
 */
public BillConfig parse(Path path) throws ConfigParseException
```

```java
/**
 * Project information from the {@code [project]} section of bill.toml.
 *
 * @param name    the project name
 * @param version the project version (defaults to {@code "1.0.0"})
 */
public record ProjectInfo(String name, String version)
```

**Bad:**
```java
/**
 * Parses a bill.toml file into a BillConfig object.
 *
 * @param path the path to the bill.toml file
 * @throws NullPointerException if path is null
 */
public BillConfig parse(Path path)
```

**Rationale:** `{@code}` renders identifiers in monospace font, making them visually distinct from prose and easier to identify as code elements.

## Method Contracts

### Parameter Validation

Methods must validate their parameters at the start of execution using `requireThat()` from requirements.java.

**Good:**
```java
public BillConfig parse(Path path) throws ConfigParseException, IOException
{
  requireThat(path, "path").isNotNull();
  // ... method body
}
```

```java
public ConfigParseException(String message)
{
  super(requireThat(message, "message").isNotBlank().getValue());
}
```

**Bad:**
```java
public BillConfig parse(Path path) throws ConfigParseException, IOException
{
  // No validation - NullPointerException will occur later with unclear message
  return mapper.readValue(path.toFile(), BillConfig.class);
}
```

### Exception Documentation

Document all exceptions that a method can throw using `@throws` in Javadoc:

**Required documentation:**
- Checked exceptions (always document)
- `NullPointerException` when parameters are validated with `requireThat().isNotNull()`
- `IllegalArgumentException` when parameters are validated with other constraints

**Good:**
```java
/**
 * Parses a bill.toml file.
 *
 * @param path the path to the bill.toml file
 * @return the parsed configuration
 * @throws NullPointerException if {@code path} is {@code null}
 * @throws ConfigParseException if {@code path} does not exist or contains invalid TOML
 * @throws IOException          if an I/O error occurs
 */
public BillConfig parse(Path path) throws ConfigParseException, IOException
```

**Rationale:** Parameter validation fails fast with clear error messages. Documenting exceptions helps callers understand what can go wrong and handle errors appropriately.

## Build Configuration

### Parameter Names Preservation

Enable `-parameters` compiler flag to preserve constructor parameter names at runtime. This allows Jackson to deserialize records without `@JsonProperty` annotations.

**Configuration (in root pom.xml):**
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.13.0</version>
  <configuration>
    <parameters>true</parameters>
  </configuration>
</plugin>
```

**Good (with -parameters flag):**
```java
public record ProjectInfo(String name, String version, String description)
{
}
```

**Bad (without -parameters flag, requires annotations):**
```java
public record ProjectInfo(
  @JsonProperty("name") String name,
  @JsonProperty("version") String version,
  @JsonProperty("description") String description)
{
}
```

**Rationale:** Cleaner code without annotation noise. The `-parameters` flag makes constructor parameter names available via reflection, which Jackson uses for deserialization.

---

*Last updated: 2026-01-07*
