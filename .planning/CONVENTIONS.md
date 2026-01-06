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

## Validation

### Runtime Validation with requirements.java

Use [requirements.java](https://github.com/cowwoc/requirements.java) (version 12.0) for all runtime validations instead of manual if/throw checks.

**Import:**
```java
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.*;
```

**Dependency (defined in root pom.xml):**
```xml
<dependency>
  <groupId>io.github.cowwoc.requirements</groupId>
  <artifactId>requirements-java</artifactId>
  <version>12.0</version>
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

---

*Last updated: 2026-01-06*
