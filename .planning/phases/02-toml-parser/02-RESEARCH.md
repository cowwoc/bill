# Phase 2: TOML Parser - Research

**Researched:** 2026-01-06
**Updated:** 2026-01-06 (corrected Jackson 3.x info and maintenance status)
**Domain:** Java TOML parsing libraries and configuration model design
**Confidence:** HIGH

<research_summary>
## Summary

Researched the Java TOML ecosystem for parsing bill.toml files into a configuration model. The landscape includes several libraries with different design philosophies: Jackson (enterprise ecosystem, active development), TomlJ (detailed error reporting, minimal maintenance), and several others with varying maintenance status.

Key finding: **Don't hand-roll TOML parsing**. The TOML 1.0 specification has complex edge cases (date/time handling, escape sequences, multi-line strings, inline vs block tables) that established libraries handle correctly. Custom parsers invariably miss edge cases and fail on valid TOML files.

After evaluating API ergonomics, error reporting quality, **active maintenance** (critical for production use), and ecosystem fit, the clear choice is **Jackson 3.0.3** for this project. It provides enterprise-grade support, very active development (3 patch releases in 2 months), full TOML 1.0.0 compliance, and ecosystem integration benefits. The upcoming Jackson 3.1 release will add multiple error collection, addressing the main advantage TomlJ previously held.

**Primary recommendation:** Use Jackson 3.0.3 (`tools.jackson.dataformat:jackson-dataformat-toml`) for TOML parsing. Design immutable configuration model POJOs with Jackson annotations. Plan to upgrade to Jackson 3.1 when released for multiple error collection feature.
</research_summary>

<standard_stack>
## Standard Stack

The established libraries/tools for Java TOML parsing:

### Core (Recommended)
| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| jackson-dataformat-toml | 3.0.3 | Jackson-based TOML support | **Active development** (Nov 2025), enterprise backing, ecosystem integration, Java 17+ focus, Jackson 3.1 will add error collection |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Jackson 3.0.3 | TomlJ 1.1.1 | TomlJ has better error reporting (error recovery + positions), but **stagnant maintenance** (last release Dec 2023, minimal 2024 activity), single maintainer, uncertain future |
| Jackson 3.0.3 | toml4j | **Don't use** - unmaintained since 2016, only supports TOML 0.4.0, no TOML 1.0 support |
| Jackson 3.0.3 | TOMLy | Newer (less battle-tested), performance-focused, comment preservation if needed |

**Why Jackson 3.0.3 is recommended for Bill:**
1. **Active maintenance** (most critical): 3 patch releases in 2 months (3.0.0 GA Oct 2025, 3.0.3 Nov 2025), large team, enterprise backing
2. **Long-term viability**: Backed by Spring Boot, FasterXML org, industry-wide adoption
3. **Modern Java**: Requires Java 17+, supports Records, matches Bill's JDK 25 target
4. **Ecosystem integration**: Works seamlessly with Jackson JSON/YAML if Bill adds APIs later
5. **Familiar API**: Standard ObjectMapper pattern, annotation-based, less boilerplate
6. **Future-proof**: Jackson 3.1 will add error collection (PR #5364 merged Nov 2025), addressing TomlJ's main advantage

**Installation:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>tools.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>3.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>tools.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-toml</artifactId>
        <!-- Version managed by BOM -->
    </dependency>
</dependencies>
```

**Dependencies:**
- Jackson core, databind, annotations (all managed by BOM)
- Requires Java 17+ (Jackson 3.x requirement)

**Package change in Jackson 3.x:**
- Old (2.x): `com.fasterxml.jackson.*`
- New (3.x): `tools.jackson.*`
</standard_stack>

<architecture_patterns>
## Architecture Patterns

### Recommended Project Structure
```
bill-toml/
├── src/main/java/
│   ├── model/          # Immutable configuration POJOs with Jackson annotations
│   │   ├── BillConfig.java
│   │   ├── Dependency.java
│   │   └── PackageInfo.java
│   ├── parser/         # TOML parsing facade
│   │   ├── BillConfigParser.java
│   │   └── ConfigValidator.java (optional - for additional validation)
│   └── exception/      # Custom exceptions
│       └── ConfigParseException.java
└── src/test/java/
    └── parser/
        └── BillConfigParserTest.java
```

### Pattern 1: Annotation-Based POJO Mapping (Recommended)
**What:** Use Jackson annotations on POJOs for automatic TOML → Java object mapping
**When to use:** Default approach - less boilerplate, type-safe, familiar Jackson pattern
**Example:**
```java
import tools.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BillConfig.Builder.class)
public final class BillConfig {
    private final String name;
    private final String version;
    private final String description;
    private final List<Dependency> dependencies;

    private BillConfig(Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.description = builder.description;
        this.dependencies = List.copyOf(builder.dependencies);
    }

    public static class Builder {
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        @JsonProperty("description")
        private String description;

        @JsonProperty("dependencies")
        private List<Dependency> dependencies = new ArrayList<>();

        public BillConfig build() {
            return new BillConfig(this);
        }
    }

    // Getters only, no setters
    public String name() { return name; }
    public String version() { return version; }
    public String description() { return description; }
    public List<Dependency> dependencies() { return dependencies; }
}

// Dependency POJO
public record Dependency(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("scope") String scope
) {
    // Java 17+ record - immutable by default
    public Dependency {
        scope = scope != null ? scope : "compile"; // default scope
    }
}
```

### Pattern 2: Parse with Jackson TomlMapper
**What:** Use `TomlMapper` to deserialize TOML files into POJOs
**When to use:** Always - this is the entry point
**Example:**
```java
import tools.jackson.dataformat.toml.TomlMapper;
import java.nio.file.Path;

public class BillConfigParser {
    private final TomlMapper mapper;

    public BillConfigParser() {
        this.mapper = TomlMapper.builder().build();
    }

    public BillConfig parse(Path tomlPath) throws ConfigParseException {
        try {
            BillConfig config = mapper.readValue(tomlPath.toFile(), BillConfig.class);

            // Additional validation (semantic checks beyond Jackson's type mapping)
            validateConfig(config);

            return config;
        } catch (JsonProcessingException e) {
            throw new ConfigParseException("Failed to parse bill.toml: " + e.getMessage(), e);
        }
    }

    private void validateConfig(BillConfig config) throws ConfigParseException {
        List<String> errors = new ArrayList<>();

        if (config.name() == null || config.name().isBlank()) {
            errors.add("package.name is required");
        }
        if (config.version() == null || !config.version().matches("\\d+\\.\\d+\\.\\d+")) {
            errors.add("package.version must follow semantic versioning (e.g., 1.0.0)");
        }

        if (!errors.isEmpty()) {
            throw new ConfigParseException("Invalid bill.toml configuration", errors);
        }
    }
}
```

### Pattern 3: Validation Accumulator (For Additional Checks)
**What:** Collect semantic validation errors after Jackson parsing
**When to use:** For validation beyond type mapping (semver format, dependency scopes, etc.)
**Example:**
```java
public class ConfigValidator {
    private final List<String> errors = new ArrayList<>();

    public ConfigValidator validateRequired(String field, String value) {
        if (value == null || value.isBlank()) {
            errors.add(field + " is required");
        }
        return this;
    }

    public ConfigValidator validateVersion(String version) {
        if (!Pattern.matches("\\d+\\.\\d+\\.\\d+", version)) {
            errors.add("version must follow semantic versioning (e.g., 1.0.0)");
        }
        return this;
    }

    public ConfigValidator validateScope(String scope) {
        Set<String> validScopes = Set.of("compile", "runtime", "test", "provided");
        if (!validScopes.contains(scope)) {
            errors.add("scope must be one of: " + validScopes);
        }
        return this;
    }

    public void throwIfInvalid() throws ConfigParseException {
        if (!errors.isEmpty()) {
            throw new ConfigParseException("Configuration validation failed", errors);
        }
    }
}

// Usage:
new ConfigValidator()
    .validateRequired("package.name", config.name())
    .validateRequired("package.version", config.version())
    .validateVersion(config.version())
    .throwIfInvalid();
```

### Anti-Patterns to Avoid
- **Hand-rolling TOML parser:** TOML has complex grammar (date/time, escape sequences, inline tables). Use Jackson.
- **Using unmaintained libraries:** Don't use toml4j (2016) or minimally-maintained TomlJ. Use actively developed Jackson 3.x.
- **Mutable configuration objects:** Use immutable POJOs (records or final classes) to prevent accidental modification.
- **Using `Map<String, Object>` for everything:** Define typed domain model instead of generic maps. Type safety catches errors.
- **Skipping validation:** Jackson handles types, but you need semantic validation (semver format, dependency scopes, etc.)
</architecture_patterns>

<dont_hand_roll>
## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| TOML parsing | Custom recursive descent parser | Jackson 3.0.3 TOML module | TOML grammar is deceptively complex: date/time with timezones, escape sequences, multi-line strings, inline vs block tables, dotted keys. Jackson has extensive test coverage and enterprise backing. |
| Object mapping | Manual field extraction | Jackson annotations + TomlMapper | Automatic deserialization reduces boilerplate, type-safe, handles nested objects and arrays correctly. |
| Configuration validation | Ad-hoc if/throw checks | Validation accumulator pattern + Jackson validation | Accumulating errors provides better UX. Jackson 3.1 will add built-in error collection. |
| Semantic versioning regex | Hand-rolled pattern | Existing semver library (if needed beyond regex) | Edge cases: pre-release tags, build metadata, version ranges. |

**Key insight:** Configuration file parsing is error-prone domain work. TOML 1.0 specification is 40+ pages covering edge cases that custom parsers invariably miss. Jackson 3.x is enterprise-grade, actively maintained, and battle-tested across thousands of projects. The cost of using an unmaintained or custom parser (bug reports, security issues, parser rewrites) far exceeds dependency cost.
</dont_hand_roll>

<common_pitfalls>
## Common Pitfalls

### Pitfall 1: Using Wrong Jackson Version (2.x vs 3.x)
**What goes wrong:** Mixing Jackson 2.x and 3.x dependencies causes ClassNotFoundException or NoSuchMethodError
**Why it happens:** Jackson 3.x changed package from `com.fasterxml.jackson` to `tools.jackson`
**How to avoid:** Use Jackson BOM to manage all Jackson dependencies, ensure all use 3.x
**Warning signs:** Import errors, class loading failures, incompatible type errors
**Code:**
```xml
<!-- WRONG: Mixing 2.x and 3.x -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId> <!-- 2.x package -->
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>tools.jackson.dataformat</groupId> <!-- 3.x package -->
    <artifactId>jackson-dataformat-toml</artifactId>
</dependency>

<!-- RIGHT: Use BOM for version management -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>tools.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>3.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Pitfall 2: TOML Version Mismatch
**What goes wrong:** Assuming TOML 0.x features work with TOML 1.0 parsers or vice versa
**Why it happens:** TOML 1.0 (released 2021) has breaking changes from 0.x versions
**How to avoid:** Use TOML 1.0.0-compliant parser (Jackson 3.x) and test with TOML 1.0 syntax
**Warning signs:** Parse errors on valid-looking TOML, unexpected behavior with dates/times
**Key TOML 1.0 features:** Local date/time types, dotted keys in inline tables, heterogeneous arrays removed

### Pitfall 3: File Encoding Issues
**What goes wrong:** Parser fails with cryptic errors on Windows-created files or files with BOM
**Why it happens:** TOML spec requires UTF-8, but systems may use other encodings or add BOM
**How to avoid:** Jackson auto-detects UTF-8, but ensure file is UTF-8 encoded
**Warning signs:** Parse errors only on certain platforms, "invalid character" errors at file start

### Pitfall 4: Confusing Tables and Inline Tables
**What goes wrong:** Mixing table header syntax `[section]` with inline table syntax `section = {}`
**Why it happens:** Both define tables but have different scoping rules
**How to avoid:** Pick one style per table. Use `[section]` for top-level, `{}` for small nested structures
**Warning signs:** "table already defined" errors, unexpected value overwrites
**Example:**
```toml
# WRONG: Can't reopen inline table
package = { name = "bill", version = "1.0.0" }
[package]  # ERROR: table already defined as inline

# RIGHT: Use block tables
[package]
name = "bill"
version = "1.0.0"

# OR: Use inline table only
package = { name = "bill", version = "1.0.0" }
```

### Pitfall 5: Java 17+ Requirement
**What goes wrong:** Build fails with "class file version" error
**Why it happens:** Jackson 3.x requires Java 17+, won't work with Java 11 or older
**How to avoid:** Ensure Bill targets Java 17+ (already done - Bill uses JDK 25)
**Warning signs:** `UnsupportedClassVersionError`, `major version 61` errors
</common_pitfalls>

<code_examples>
## Code Examples

Verified patterns from official sources:

### Basic Jackson TOML Usage
```java
// Source: Jackson 3.x documentation
import tools.jackson.dataformat.toml.TomlMapper;
import java.nio.file.Path;
import java.nio.file.Paths;

TomlMapper mapper = TomlMapper.builder().build();
BillConfig config = mapper.readValue(Paths.get("bill.toml").toFile(), BillConfig.class);

// Access strongly-typed values
String name = config.name();
String version = config.version();
List<Dependency> deps = config.dependencies();
```

### Complete bill.toml Parser Example
```java
// Comprehensive example for Bill project
import tools.jackson.dataformat.toml.TomlMapper;
import tools.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import java.util.*;

public class BillConfigParser {
    private final TomlMapper mapper;

    public BillConfigParser() {
        this.mapper = TomlMapper.builder().build();
    }

    public BillConfig parse(Path tomlPath) throws ConfigParseException {
        try {
            // Parse TOML into POJO
            BillConfig config = mapper.readValue(tomlPath.toFile(), BillConfig.class);

            // Validate semantics (Jackson handles types, we validate business rules)
            validate(config);

            return config;
        } catch (JsonProcessingException e) {
            // Jackson parsing failed (syntax error, type mismatch, etc.)
            throw new ConfigParseException(
                "Failed to parse bill.toml: " + e.getOriginalMessage(),
                e
            );
        }
    }

    private void validate(BillConfig config) throws ConfigParseException {
        List<String> errors = new ArrayList<>();

        // Validate required fields
        if (config.name() == null || config.name().isBlank()) {
            errors.add("package.name is required");
        }

        if (config.version() == null || !config.version().matches("\\d+\\.\\d+\\.\\d+")) {
            errors.add("package.version is required and must follow semver (e.g., 1.0.0)");
        }

        // Validate dependencies
        if (config.dependencies() != null) {
            for (int i = 0; i < config.dependencies().size(); i++) {
                Dependency dep = config.dependencies().get(i);
                if (dep.name() == null || dep.version() == null) {
                    errors.add("dependencies[" + i + "] must have 'name' and 'version'");
                }

                Set<String> validScopes = Set.of("compile", "runtime", "test", "provided");
                if (!validScopes.contains(dep.scope())) {
                    errors.add("dependencies[" + i + "] has invalid scope: " + dep.scope());
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ConfigParseException("Invalid bill.toml configuration", errors);
        }
    }
}
```

### Example bill.toml Structure
```toml
# Bill configuration file
[package]
name = "my-java-app"
version = "1.0.0"
description = "A Java application built with Bill"

[[dependencies]]
name = "com.google.guava:guava"
version = "33.0.0-jre"
scope = "compile"

[[dependencies]]
name = "org.testng:testng"
version = "7.10.2"
scope = "test"

[build]
target = "25"
source = "25"
```

### POJO Definition with Jackson Annotations
```java
import tools.jackson.annotation.JsonProperty;

// Using Java 17+ records for immutability
public record BillConfig(
    @JsonProperty("package") PackageInfo packageInfo,
    @JsonProperty("dependencies") List<Dependency> dependencies,
    @JsonProperty("build") BuildSettings build
) {
    // Default constructor for Jackson
    public BillConfig {
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }
}

public record PackageInfo(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("description") String description
) {}

public record Dependency(
    @JsonProperty("name") String name,
    @JsonProperty("version") String version,
    @JsonProperty("scope") String scope
) {
    public Dependency {
        scope = scope != null ? scope : "compile"; // default scope
    }
}

public record BuildSettings(
    @JsonProperty("source") String source,
    @JsonProperty("target") String target
) {
    public BuildSettings {
        source = source != null ? source : "25";
        target = target != null ? target : "25";
    }
}
```

### Custom Exception with Multiple Errors
```java
// Provide helpful error messages with all problems at once
public class ConfigParseException extends Exception {
    private final List<String> errors;

    public ConfigParseException(String message, List<String> errors) {
        super(formatMessage(message, errors));
        this.errors = List.copyOf(errors);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of(message);
    }

    private static String formatMessage(String message, List<String> errors) {
        StringBuilder sb = new StringBuilder(message);
        sb.append(":\n");
        for (String error : errors) {
            sb.append("  - ").append(error).append("\n");
        }
        return sb.toString();
    }

    public List<String> getErrors() {
        return errors;
    }
}
```
</code_examples>

<sota_updates>
## State of the Art (2025-2026)

What's changed recently:

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Jackson 2.x (com.fasterxml.package) | Jackson 3.x (tools.jackson package) | Oct 2025 (3.0.0 GA) | Java 17+ required, cleaner architecture, Records support |
| toml4j (TOML 0.4.0) | Jackson 3.x (TOML 1.0.0) | 2021+ | TOML 1.0 is stable spec; 0.4.0 parsers miss features |
| Fail-fast parsing | Error collection (Jackson 3.1+) | Planned 2026 | Users get all errors at once (PR #5364 merged Nov 2025) |
| TomlJ for error recovery | Jackson 3.1 error collection | Switching in 3.1 | Jackson will match TomlJ's error collection capability |

**New tools/patterns to consider:**
- **Jackson 3.0.3 (Nov 2025):** Latest stable, full TOML 1.0.0 compliance, enterprise backing, very active development
- **Jackson 3.1 (planned 2026):** Will add multiple error collection via `problemCollectingReader()` (PR #5364), configurable limit (default 100), thread-safe
- **Java 17+ Records:** Perfect for immutable config POJOs, less boilerplate than builders
- **Validation accumulator pattern:** Gaining popularity for CLI tools (inspired by Rust's error reporting philosophy)

**Deprecated/outdated:**
- **toml4j:** Unmaintained since 2016, only TOML 0.4.0, don't use
- **Jackson 2.x for new projects:** Use 3.x for modern Java features (Records, Java 17+)
- **TomlJ for production use:** Minimal maintenance (last release Dec 2023, 1 commit in 2024), uncertain future, single maintainer

**Migration path:**
1. **Phase 2 (now):** Use Jackson 3.0.3 (fail-fast parsing)
2. **Phase 2 update (when 3.1 releases):** Upgrade to Jackson 3.1 for error collection
3. **Production:** Benefit from active maintenance, security patches, and ecosystem evolution
</sota_updates>

<open_questions>
## Open Questions

Things that couldn't be fully resolved:

1. **Jackson 3.1 release date**
   - What we know: PR #5364 merged Nov 25, 2025, targeted for 3.1
   - What's unclear: Specific release date (Jackson 3.0.0 released Oct 2025, so 3.1 might be Q1-Q2 2026)
   - Recommendation: Start with Jackson 3.0.3 in Phase 2, plan to upgrade to 3.1 when released for error collection

2. **Error collection API details**
   - What we know: Opt-in via `problemCollectingReader()`, configurable limit (default 100), thread-safe
   - What's unclear: Exact API surface, how errors are accessed, integration with TOML parsing
   - Recommendation: Monitor Jackson 3.1 release notes, prepare to refactor error handling when upgrading

3. **Schema validation beyond manual checks**
   - What we know: TOML has no official schema language (unlike JSON Schema)
   - What's unclear: Whether a third-party schema validation library exists and is worth using
   - Recommendation: Manual validation with accumulator pattern is sufficient for Phase 2. Revisit if bill.toml structure becomes complex.
</open_questions>

<sources>
## Sources

### Primary (HIGH confidence)
- [Jackson 3.0.0 GA Release](https://cowtowncoder.medium.com/jackson-3-0-0-ga-released-1f669cda529a) - Oct 3, 2025 announcement
- [Jackson Release 3.0.3](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0.3) - Nov 28, 2025
- [Jackson Release 3.1](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1) - Planned features
- [Jackson PR #5364: Error collection](https://github.com/FasterXML/jackson-databind/pull/5364) - Merged Nov 25, 2025
- [Jackson dataformats-text 3.x](https://github.com/FasterXML/jackson-dataformats-text/tree/3.x/toml) - TOML module
- [TOML official specification v1.1.0](https://toml.io/en/v1.1.0) - Format specification
- [TOML v1.0.0 specification](https://toml.io/en/v1.0.0) - Stable spec features

### Secondary (MEDIUM confidence)
- [TomlJ GitHub repository](https://github.com/tomlj/tomlj) - Library documentation, commit history shows minimal 2024 activity
- [TomlJ Maven Central](https://central.sonatype.com/artifact/org.tomlj/tomlj) - Version 1.1.1, Dec 2023 (2+ years old)
- [Spring Jackson 3 support](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/) - Oct 2025

### Tertiary (LOW confidence - noted for validation during implementation)
- TOMLy performance claims - Benchmarks not independently verified
- Jackson 3.1 exact release timeline - Only "planned for 3.1", no specific date
</sources>

<metadata>
## Metadata

**Research scope:**
- Core technology: Java TOML parsing (Jackson 3.x, TomlJ, toml4j, others)
- Ecosystem: Available libraries, **maintenance status** (critical factor), API comparison
- Patterns: Configuration model design, validation strategies, error reporting
- Pitfalls: Package migration (2.x → 3.x), encoding issues, TOML version compat

**Confidence breakdown:**
- Standard stack: HIGH - Jackson 3.0.3 verified on Maven Central (Nov 2025), TomlJ 1.1.1 (Dec 2023) confirmed stagnant
- Architecture: HIGH - Patterns derived from Jackson 3.x docs, Java 17+ Records, Spring Boot practices
- Pitfalls: HIGH - Documented in Jackson migration guide, TOML spec, and cross-referenced with community discussions
- Code examples: HIGH - Based on Jackson 3.x official documentation and Java 17+ best practices

**Research date:** 2026-01-06
**Valid until:** 2026-02-06 (30 days - Jackson ecosystem evolves quickly, monitor for 3.1 release)

**Key decisions for planning:**
1. **Library choice:** Jackson 3.0.3 (`tools.jackson.dataformat:jackson-dataformat-toml`) - NOT TomlJ, NOT toml4j
2. **Error strategy:** Fail-fast (Jackson 3.0.x), upgrade to error collection when 3.1 releases
3. **Model pattern:** Immutable POJOs using Java 17+ Records with Jackson annotations
4. **Validation approach:** Jackson handles types, custom validator for semantic checks (semver, scopes)
5. **Upgrade path:** Plan for Jackson 3.1 upgrade when released for error collection feature

**Maintenance status (critical):**
- **Jackson 3.0.3:** Very active (3 releases in 2 months), enterprise backing, long-term support
- **TomlJ 1.1.1:** Stagnant (last release Dec 2023, 1 commit in 2024), single maintainer, uncertain future

</metadata>

---

*Phase: 02-toml-parser*
*Research completed: 2026-01-06*
*Updated: 2026-01-06 (corrected Jackson 3.x info)*
*Ready for planning: yes*
