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
