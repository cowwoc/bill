# Bill: A Modern Build System for Java

## Executive Summary

Bill is a build system for Java that combines Cargo's philosophy of opinionated simplicity with Java's ecosystem requirements. It enforces a sharp boundary between declarative configuration (bill.toml) and imperative build logic (src/bill/java/), prioritizing IDE analyzability, fast feedback loops, and predictable builds.

**Design Principles:**

1. **Static analyzability first** — IDEs must understand projects without executing build code
2. **Opinionated defaults** — Zero configuration for standard projects
3. **Declarative/imperative boundary** — bill.toml for what, src/bill/java/ for how
4. **Composition over extension** — External tools over plugin systems
5. **Fast feedback** — Incremental compilation, parallel builds by default
6. **100% reproducible** — Same inputs always produce identical outputs
7. **Maven interoperability** — Full compatibility with existing Maven-published artifacts; no Bill-specific files required in dependencies

---

## Core Concepts

### Project Structure

```
my-project/
├── bill.toml              # Declarative manifest (IDE-parseable)
├── src/
│   ├── main/
│   │   ├── java/          # Production code
│   │   └── resources/     # Production resources
│   ├── test/
│   │   ├── java/          # Test code
│   │   └── resources/     # Test resources
│   └── bill/
│       └── java/          # Build-time code (commands, code generation)
│           └── com/example/build/
│               ├── module-info.java
│               ├── Build.java
│               └── Docker.java
└── target/                # Build outputs (gitignored)
    ├── classes/
    ├── test-classes/
    ├── generated-sources/
    └── artifacts/
```

The structure mirrors Maven's conventions for `src/main/` and `src/test/`, adding `src/bill/` for build-time code. This replaces Maven's pom.xml plugin configuration with actual Java code.

### bill.toml (Declarative Manifest)

Everything an IDE needs to understand the project lives here.

```toml
[project]
name = "my-app"
version = "1.0.0"
description = "My application"
module = "com.example.myapp"           # JPMS module name
authors = ["Alice <alice@example.com>"]
license = "MIT"
repository = "https://github.com/alice/my-app"

[dependencies]
guava = { version = "33.0.0-jre" }
spring-boot-starter-web = { version = "3.2.0" }
lombok = { version = "1.18.30", scope = "compile-only" }
postgresql = { version = "42.7.0", scope = "runtime-only" }
junit-jupiter = { version = "5.10.0", scope = "test-only" }
mockito-core = { version = "5.8.0", scope = "test-only" }
jooq-codegen = { version = "3.18.0", scope = "build-only" }
flyway-core = { version = "10.0.0", scope = "build-only" }
bill-quality-plugin = { version = "1.0.0", scope = "build-only" }

# Built-in commands (build, test, run, pull, etc.) available with no config

# Override built-in command with local class
[commands.pull]
class = "com.example.build.MyPull"

[commands.pull.properties]
timeout = "30s"

# Custom command from local class
[commands.docker]
class = "com.example.build.Docker"

[commands.docker.properties]
registry = "ghcr.io"
tag = "${project.version}"

# Custom command from dependency
[commands.lint]
class = "com.quality.Lint"
dependency = "bill-quality-plugin"

[commands.lint.properties]
rules = ["checkstyle", "spotbugs"]
fail-on-warning = true

# Optional: customize paths (defaults shown)
[paths]
sources = ["src/main/java"]
resources = ["src/main/resources"]
test-sources = ["src/test/java"]
test-resources = ["src/test/resources"]
generated-sources = ["target/generated-sources"]
```

**Key constraint:** bill.toml is pure data. No conditional logic, no functions. Variable interpolation (`${project.version}`) is static text substitution resolved at parse time. IDEs parse it directly.

### Command Registration

Commands are registered in `[commands.X]` with a `class` and optional `dependency`:

```toml
# Built-in commands (build, test, run, pull, etc.) available with no config

# Override built-in command with local class
[commands.pull]
class = "com.example.build.MyPull"

[commands.pull.properties]
timeout = "30s"

# Custom command from local class (in src/bill/java/)
[commands.docker]
class = "com.example.build.Docker"

[commands.docker.properties]
registry = "ghcr.io"
tag = "${project.version}"

# Custom command from dependency
[commands.lint]
class = "com.quality.Lint"
dependency = "bill-quality-plugin"    # Must exist in [dependencies] with scope = "build-only"

[commands.lint.properties]
rules = ["checkstyle", "spotbugs"]
fail-on-warning = true
```

**Registration rules:**
- Built-in commands are always available unless overridden
- `class` specifies the fully-qualified class name (required for override/custom)
- `dependency` specifies which build-only dependency contains the class (omit for local)
- `[commands.X.properties]` contains configuration passed to the command at runtime

**Validation:**
- If `dependency` is specified, it must exist in `[dependencies]` with `scope = "build-only"`
- If `dependency` is omitted, class must exist in `src/bill/java/`
- Unknown keys under `[commands.X]` → error
- Anything under `[commands.X.properties]` → passed through to the command

### Build Commands (src/bill/java/)

Build-time code lives in `src/bill/java/` as a separate JPMS module.

**Module definition:**
```java
// src/bill/java/com/example/build/module-info.java
module com.example.build {
    requires bill.api;
    
    exports com.example.build;
}
```

**Implementing a command:**
```java
// src/bill/java/com/example/build/Build.java
package com.example.build;

import bill.api.Bill;

public class Build {
    public static void main(String[] args) {
        var ctx = Bill.context(args);
        var opts = ctx.options();
        
        // Access typed configuration from [commands.build.properties]
        boolean release = opts.get("release").asBoolean(false);
        int parallel = opts.get("parallel").asInt(4);
        
        // Optional: orchestrate external services
        try (var cleanup = ctx.startService("docker-compose", "up", "-d", "db")) {
            ctx.waitForPort(5432, Duration.ofSeconds(30));
            
            // Run Flyway migrations
            Flyway.configure()
                .dataSource("jdbc:postgresql://localhost/mydb", "user", "pass")
                .load()
                .migrate();
            
            // Generate jOOQ code
            GenerationTool.generate(loadJooqConfig());
            ctx.addGeneratedSources("target/generated-sources/jooq");
        }
        
        // After main() returns, Bill proceeds with compilation
    }
}
```

**Custom commands:**
```java
// src/bill/java/com/example/build/Docker.java
package com.example.build;

import bill.api.Bill;

public class Docker {
    public static void main(String[] args) {
        var ctx = Bill.context(args);
        var opts = ctx.options();
        
        // Get typed configuration from [commands.docker.properties]
        String registry = opts.get("registry").asString("ghcr.io");
        String tag = opts.get("tag").asString(ctx.project().version());
        List<String> buildArgs = opts.get("build-args").asList(List.of());
        
        // Entirely custom — no default behavior
        var cmd = new ArrayList<>(List.of("docker", "build", "-t", registry + "/" + ctx.project().name() + ":" + tag));
        for (String arg : buildArgs) {
            cmd.add("--build-arg");
            cmd.add(arg);
        }
        cmd.add(".");
        
        new ProcessBuilder(cmd)
            .inheritIO()
            .start()
            .waitFor();
    }
}
```

**Delegating to built-in or other commands:**
```java
// src/bill/java/com/example/build/MyPull.java
package com.example.build;

import bill.api.Bill;
import bill.commands.Pull;  // Built-in command

public class MyPull {
    public static void main(String[] args) {
        var ctx = Bill.context(args);
        
        // Handle custom subcommand
        if (args.length > 0 && args[0].equals("custom")) {
            doCustomThing(args);
            return;
        }
        
        // Delegate to built-in implementation
        Pull.main(args);
    }
}
```

```bash
bill docker    # Runs Docker.main()
```

**Internal utilities:**
```java
// src/bill/java/com/example/build/internal/Helpers.java
package com.example.build.internal;

public class Helpers {
    public static void runMigrations() { /* ... */ }
}
```

All files in `src/bill/java/` compile together, so commands can share code.

**Key constraint:** Build commands cannot affect anything the IDE needs (dependencies, source paths). They only perform build-time side effects like code generation.

---

## Command Line Interface

### Multi-Command Execution

Bill supports running multiple commands in a single invocation with pipeline parallelism:

```bash
bill build test package
```

This enables efficient multi-project builds where one subproject can start testing while another is still compiling.

### Property Overrides (-D)

Override command properties from the command line:

```bash
bill build test -Dbuild.release=true -Dtest.parallel=8
```

**Property resolution:**
- `-Dcommand.property=value` maps to `[commands.command.properties]` property
- Values are strings; commands convert to typed representations
- Unknown properties cause immediate failure with suggestions
- Case-sensitive for reproducibility

**Nested properties via dotted expansion:**
```bash
bill deploy -Ddeploy.server.host=prod.example.com -Ddeploy.server.port=8080
```

**File references for complex config:**
```bash
bill deploy -Ddeploy.server=@config/prod-server.json
```

The `@` prefix loads from file. If combined with dotted properties, file loads first, then properties override.

### Core Commands

```bash
bill build              # Compile (debug, incremental)
bill build test         # Compile then test (pipelined across subprojects)
bill build test package # Compile, test, package (pipelined)
bill check              # Type-check without producing artifacts (fast)
bill test               # Compile + run tests
bill run                # Compile + run main class
bill clean              # Remove target/
bill doc                # Generate Javadoc
bill fmt                # Format code (google-java-format)
bill lint               # Run static analysis (Error Prone, etc.)
```

### Dependency Commands

```bash
bill deps               # Show dependency tree
bill deps --outdated    # Show available updates
bill update             # Check for newer versions (interactive)
bill update guava       # Update specific dependency in bill.toml
bill fetch              # Download dependencies without building
```

### Project Commands

```bash
bill build                    # Build all subprojects
bill build -p api             # Build specific subproject (+ its deps)
bill test --all               # Test all subprojects
bill publish -p core          # Publish specific subproject
bill package                  # Create JAR + generate POM
bill publish                  # Package + sign + upload to repository
```

### Parallelism Control

```bash
bill build              # Parallel (default: all cores)
bill build -j 4         # Limit to 4 threads
bill build -j 1         # Sequential (for debugging)
```

---

## Command Options API

Commands receive configuration as strings and convert to typed values:

```java
public interface OptionValue {
    // Check presence
    boolean isPresent();
    
    // Primitives
    String asString(String defaultValue);
    int asInt(int defaultValue);
    long asLong(long defaultValue);
    double asDouble(double defaultValue);
    boolean asBoolean(boolean defaultValue);
    
    // Common types
    Path asPath(Path defaultValue);
    File asFile(File defaultValue);
    URI asUri(URI defaultValue);
    Duration asDuration(Duration defaultValue);    // "30s", "5m", "1h"
    Instant asInstant(Instant defaultValue);       // ISO-8601
    
    // Enums (case-sensitive)
    <E extends Enum<E>> E asEnum(Class<E> enumType, E defaultValue);
    
    // Collections (comma-separated)
    List<String> asList(List<String> defaultValue);
    Set<String> asSet(Set<String> defaultValue);
    Map<String, String> asMap(Map<String, String> defaultValue);  // "k1=v1,k2=v2"
    
    // Complex objects (from @file reference)
    <T> T asJson(Class<T> type, T defaultValue);
    
    // Raw access
    String raw();  // Original string, null if not present
}
```

**Usage example:**
```java
public class Deploy {
    public static void main(String[] args) {
        var ctx = Bill.context(args);
        var opts = ctx.options();
        
        String serverHost = opts.get("server.host").asString("localhost");
        int serverPort = opts.get("server.port").asInt(8080);
        Duration timeout = opts.get("timeout").asDuration(Duration.ofMinutes(5));
        List<String> targets = opts.get("targets").asList(List.of("production"));
        LogLevel level = opts.get("log-level").asEnum(LogLevel.class, LogLevel.INFO);
    }
}
```

---

## Dependency Management

### Version Resolution Philosophy

Bill uses explicit version resolution with fail-fast conflict detection:

1. **No version ranges** — Only exact version numbers
2. **No automatic resolution** — Bill doesn't pick versions for you
3. **Fail on conflict** — If transitive dependencies disagree, build fails
4. **User resolves** — Add explicit dependency to choose winning version

```
error: version conflict for 'guava'

  library-a:1.0 requires guava:32.0.0
  library-b:2.0 requires guava:33.0.0

help: add an explicit dependency to choose a version:

  guava = { version = "33.0.0-jre" }
```

### Dependency Declaration

All dependencies live in a single `[dependencies]` section with explicit scope:

```toml
[dependencies]
# Default scope (compile + package + runtime + test)
guava = { version = "33.0.0-jre" }
spring-boot-starter-web = { version = "3.2.0" }

# Compile-only: needed for compilation but not packaged (servlet-api, lombok)
lombok = { version = "1.18.30", scope = "compile-only" }
jakarta-servlet-api = { version = "6.0.0", scope = "compile-only" }

# Runtime-only: packaged but not compiled against (JDBC drivers, SLF4J impls)
postgresql = { version = "42.7.0", scope = "runtime-only" }
logback-classic = { version = "1.4.14", scope = "runtime-only" }

# Test-only: only available during testing
junit-jupiter = { version = "5.10.0", scope = "test-only" }
mockito-core = { version = "5.8.0", scope = "test-only" }

# Build-only: only available to src/bill/java/
jooq-codegen = { version = "3.18.0", scope = "build-only" }
flyway-core = { version = "10.0.0", scope = "build-only" }
bill-quality-plugin = { version = "1.0.0", scope = "build-only" }

# With exclusions
spring-boot-starter-logging = { version = "3.2.0", exclude = ["logback-classic"] }

# Local path dependency (for multi-project)
core = { path = "../core" }
```

### Dependency Scopes

| Scope | Compile | Package | Runtime | Test |
|-------|---------|---------|---------|------|
| *(default)* | ✅ | ✅ | ✅ | ✅ |
| `compile-only` | ✅ | ❌ | ❌ | ✅ |
| `runtime-only` | ❌ | ✅ | ✅ | ✅ |
| `test-only` | ❌ | ❌ | ❌ | ✅ |
| `build-only` | src/bill/ only |

**When to use each scope:**

- **default** — Normal libraries (guava, spring, jackson)
- **compile-only** — Container provides at runtime (servlet-api) or compile-time tooling (lombok)
- **runtime-only** — ServiceLoader implementations (JDBC drivers, SLF4J backends)
- **test-only** — Testing frameworks (JUnit, Mockito)
- **build-only** — Code generation tools, command plugins

### Version Alignment

For frameworks like Spring Boot that coordinate many dependency versions, use `bill pull versions`:

```bash
bill pull versions https://spring.io/boot/3.2.0/bill.toml
```

This updates your bill.toml, filling in versions for matching dependencies:

**Before:**
```toml
[dependencies]
spring-boot-starter-web = {}
jackson-databind = {}
```

**After:**
```toml
[dependencies]
spring-boot-starter-web = { version = "3.2.0" }
jackson-databind = { version = "2.16.0" }
```

For corporate standards:
```bash
bill pull versions https://internal.company.com/standards/2024.1/bill.toml
```

### Transitive Dependencies from Maven POMs

Bill resolves transitive dependencies from standard Maven POMs. No Bill-specific files are required in dependencies.

**Version ranges in POMs:**

Most Maven POMs use exact versions, but some contain ranges:

```xml
<version>[1.0,2.0)</version>
```

Bill fails fast when encountering a version range in a transitive dependency:

```
error: version range in transitive dependency

  guava:33.0.0-jre depends on error-prone-annotations:[2.0,3.0)

help: add an explicit version to bill.toml:

  error-prone-annotations = { version = "2.18.0" }
```

This ensures bill.toml remains the single source of truth for all version decisions.

### Repository Configuration

```toml
[repositories]
maven-central = { url = "https://repo1.maven.org/maven2", default = true }
spring-milestones = { url = "https://repo.spring.io/milestone" }
company-internal = { url = "https://nexus.company.com/repository/maven-releases", auth = "env" }
```

---

## Multi-Project Builds

Bill uses Maven 4 terminology: **project** and **subprojects**.

### Structure

```
my-project/
├── bill.toml              # Root project manifest
├── core/
│   ├── bill.toml
│   └── src/
├── api/
│   ├── bill.toml
│   └── src/
└── cli/
    ├── bill.toml
    └── src/
```

### Root bill.toml

```toml
[project]
name = "my-project"
subprojects = ["core", "api", "cli"]

# Shared dependency versions
[project.dependencies]
guava = { version = "33.0.0-jre" }
spring-boot-starter-web = { version = "3.2.0" }
junit-jupiter = { version = "5.10.0", scope = "test-only" }
lombok = { version = "1.18.30", scope = "compile-only" }

# Internal subprojects (available as path dependencies)
core = { path = "core" }
api = { path = "api" }

# Shared metadata
[project.metadata]
authors = ["Team <team@example.com>"]
license = "Apache-2.0"
repository = "https://github.com/company/my-project"
```

### Subproject bill.toml

```toml
[project]
name = "api"
version = "1.0.0"
# Inherit from root project
authors.project = true
license.project = true

[dependencies]
core.project = true              # Path dependency from root
guava.project = true             # Version from root
spring-boot-starter-web.project = true

# Subproject-specific dependencies
caffeine = { version = "3.1.8" }
junit-jupiter.project = true     # test-only scope inherited from root
```

### Cross-Subproject References

Subprojects can reference values from sibling subprojects within the same project:

```toml
# cli/bill.toml
[project]
name = "cli"
version = "${core.version}"      # Match core's version
```

**Rules:**
- References use `${subproject.field}` syntax
- Only works within the same project (not external dependencies)
- Bill validates for circular references at parse time and fails fast

```
error: circular configuration reference detected
  
  core/bill.toml:3 → version references 'api.version'
  api/bill.toml:3  → version references 'core.version'
  
  Move shared values to the root bill.toml instead.
```

### Pipeline Parallelism

Bill pipelines command execution across subprojects. A subproject can advance to the next phase as soon as its dependencies have completed that phase:

```
Time ────────────────────────────────────────────────────►

core:   [compile][test    ]
api:            [compile][test    ]
cli:                    [compile][test]
```

This is faster than phase-locked execution (all compiles, then all tests):

```
# Phase-locked (slower)
core:   [compile]             [test    ]
api:            [compile]            [test    ]
cli:                   [compile]            [test]
```

**Rule:** Subproject X can run phase P when all dependencies of X have completed phase P.

### Build Order

Bill computes a topological sort of the dependency graph:

```
core (no internal deps)  →  builds first
api (depends on core)    →  builds second  
cli (depends on api)     →  builds third
```

Circular dependencies between subprojects are forbidden:

```
error: circular dependency detected

  core → api → cli → core
  
  Refactor to break the cycle.
```

---

## Build Pipeline

### Fixed Pipeline

Bill has a fixed build pipeline:

```
┌─────────────────────────────────────────────────────────────────┐
│                        BILL BUILD PIPELINE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. RESOLVE          Parse bill.toml, resolve dependency graph   │
│         │                                                        │
│         ▼                                                        │
│  2. FETCH            Download missing dependencies               │
│         │                                                        │
│         ▼                                                        │
│  3. BUILD COMMAND    Run Build.main() if present                 │
│         │            (code generation, migrations, etc.)         │
│         ▼                                                        │
│  4. COMPILE          javac on src + generated-sources            │
│         │                                                        │
│         ▼                                                        │
│  5. OUTPUT           Produce artifacts in target/                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Command Execution

Built-in commands (build, test, run) have default behavior. If you provide a matching class with main(), your code runs first:

| Command | Your Code | Then Bill |
|---------|-----------|-----------|
| `bill build` | Build.main() | Compiles src/main/java/ |
| `bill test` | Test.main() | Compiles and runs tests |
| `bill run` | Run.main() | Runs main class |
| `bill docker` | Docker.main() | (nothing — fully custom) |

Custom commands have no default behavior — your main() is the entire implementation.

---

## IDE Integration

### Project Import

IDEs parse bill.toml directly without executing any code:

```
IDE reads bill.toml
    │
    ├─► Dependencies → Configure classpath
    ├─► [commands.build] → Language level hints
    ├─► Source paths → Configure source roots
    │       ├─► src/main/java/
    │       ├─► src/test/java/
    │       └─► src/bill/java/ (build-time code)
    └─► Subproject structure → Configure modules
```

### Source Roots

IDEs should recognize three source root types:

| Directory | Scope | Classpath |
|-----------|-------|-----------|
| src/main/java/ | Production | Main |
| src/test/java/ | Test | Test |
| src/bill/java/ | Build | Build (separate) |

### Generated Sources

Build commands output to `target/generated-sources/`. IDEs should:

1. Mark `target/generated-sources/` as a generated source root
2. Trigger generation on project import (optional)
3. Re-trigger on explicit user action

---

## Configuration Inheritance Model

### Maven Conventions Reused

Bill adopts Maven's directory structure for source and resource directories:

| Maven | Bill | Purpose |
|-------|------|---------|
| src/main/java/ | src/main/java/ | Production code |
| src/main/resources/ | src/main/resources/ | Production resources |
| src/test/java/ | src/test/java/ | Test code |
| src/test/resources/ | src/test/resources/ | Test resources |
| pom.xml | bill.toml + src/bill/java/ | Build definition |

### What Can Be Inherited

| Configuration | Inheritable | Syntax |
|---------------|-------------|--------|
| Authors | ✅ | `authors.project = true` |
| License | ✅ | `license.project = true` |
| Repository | ✅ | `repository.project = true` |
| Dependency versions | ✅ | `guava.project = true` |
| Dependency with override | ✅ | `spring = { project = true, exclude = ["logging"] }` |
| Cross-subproject values | ✅ | `version = "${core.version}"` |
| Source paths | ❌ | Per-subproject only |
| Build commands | ❌ | Per-subproject only |

### What Cannot Be Inherited

- **Source paths** — Each subproject defines its own structure
- **Build commands** — Each subproject has its own src/bill/java/
- **Arbitrary properties** — No general property interpolation beyond defined fields

---

## Publishing

Bill provides one-command publishing that generates Maven-compatible artifacts:

```bash
bill publish                           # Publish to default repository
bill publish --repository company      # Publish to specific repository
```

Under the hood:
1. Builds JAR with embedded bill.toml (for provenance)
2. Generates POM from bill.toml (Maven ecosystem compatibility)
3. Signs with GPG (if configured)
4. Uploads to configured repository

For manual control:
```bash
bill package                           # Create JAR + POM locally
# Then use existing tools if needed
```

### Publishing Configuration

```toml
[publish]
repository = "maven-central"           # Default target
gpg-key = "env:GPG_KEY_ID"            # Key for signing

[repositories.maven-central]
url = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
auth = "env"                           # Read from BILL_REPO_USER / BILL_REPO_PASS
```

---

## Reproducibility

Bill guarantees 100% reproducible builds across different machines.

### Requirements

**Explicit versions:**
- All dependencies in bill.toml have exact versions
- No version ranges in bill.toml
- Transitive version ranges fail fast — user must specify explicit version

**Immutable artifacts:**
- Maven POMs are immutable once published
- Same bill.toml always resolves to same dependency tree

**Deterministic outputs:**
- Fixed timestamps in JARs (epoch or project-defined)
- Deterministic file ordering in archives
- Normalized line endings

**Verification:**
```bash
bill build --verify-reproducible
# Builds twice, compares outputs byte-for-byte
```

### Local Cache (v1)

```
target/
├── .bill-cache/           # Incremental compilation state
├── classes/
└── ...
```

Remote/shared caching deferred to future versions.

---

## Error Messages

Bill prioritizes human-readable error messages without error codes:

```
error: dependency version conflict

  --> bill.toml:15:1
   |
15 | guava = { version = "33.0.0-jre" }
   | ^^^^^ requires guava 33.x
   |
  --> core/bill.toml:8:1
   |
 8 | guava = { version = "31.0-jre" }
   | ^^^^^ requires guava 31.x

help: use [project.dependencies] to declare a single version for all subprojects
```

**Principles:**
- Color-coded output
- ASCII art pointers to exact location
- Explain what, where, and why
- Suggest fixes when possible
- No error codes (searchable prose instead)

---

## Open Questions

### 1. Subproject Terminology

**Status: RESOLVED**

Using Maven 4 terminology:
- `[project]` for metadata
- `subprojects = [...]` for listing child projects
- Avoids confusion with JPMS "modules"

---

### 2. Cross-Subproject Configuration References

**Status: RESOLVED**

References use `${subproject.field}` syntax within a project:
```toml
version = "${core.version}"
```

Bill validates for circular references at parse time. External dependencies cannot be referenced (they're already built).

---

### 3. Multiple Commands / Task Composition

**Status: RESOLVED**

Space-separated commands with pipeline parallelism:
```bash
bill build test package -Dbuild.release=true
```

Property overrides use `-Dcommand.property=value` syntax.

---

### 4. Pre-Compilation Task Ordering

**Status: RESOLVED**

Build commands are plain Java code. Order is controlled by writing code in order:

```java
public class Build {
    public static void main(String[] args) {
        runFlyway();      // Runs first
        generateJooq();   // Runs second (after Flyway completes)
        // After main() returns, Bill compiles
    }
}
```

No task DAG, no dependency declarations. If you need parallelism, use Java's concurrency primitives.

---

### 5. Database/External Service Dependencies

**Status: RESOLVED**

Bill provides optional helpers but doesn't require them:

```java
try (var cleanup = ctx.startService("docker-compose", "up", "-d", "db")) {
    ctx.waitForPort(5432, Duration.ofSeconds(30));
    runFlyway();
    generateJooq();
}  // cleanup runs "docker-compose down"
```

Users can also manage services externally — Bill doesn't care.

---

### 6. Publishing and Artifact Distribution

**Status: RESOLVED**

One-command publish that generates Maven-compatible artifacts:
```bash
bill publish
```

Also supports `bill package` for local artifact creation without upload.

---

### 7. Annotation Processor Handling

**Status: RESOLVED (Low Priority)**

Auto-detection via `META-INF/services/javax.annotation.processing.Processor`. Explicit configuration available for edge cases:

```toml
[annotation-processors]
mapstruct = { version = "1.5.5", options = { defaultComponentModel = "spring" } }
```

---

### 8. JPMS Support

**Status: RESOLVED**

- `src/bill/java/` requires module-info.java (for command discovery)
- `src/main/java/` is optional — if module-info.java exists, use module path; otherwise, classpath

---

### 9. Build Cache and Reproducibility

**Status: RESOLVED**

- Local cache only for v1
- 100% reproducible builds required
- Remote cache deferred to future versions

---

### 10. Error Message Quality

**Status: RESOLVED**

Invest in high-quality error messages early. Human-readable prose without error codes. Searchable, actionable, with fix suggestions.

---

## Implementation Phases

### Phase 1: Core Build (MVP)

- [ ] bill.toml parser
- [ ] Dependency resolution from Maven Central
- [ ] Fail-fast on transitive version ranges
- [ ] Dependency scopes (default, compile-only, runtime-only, test-only, build-only)
- [ ] Basic compilation (javac wrapper)
- [ ] Test execution (JUnit 5)
- [ ] `bill build`, `bill test`, `bill run` commands
- [ ] Reproducible JAR creation

### Phase 2: Build Commands (src/bill/)

- [ ] src/bill/java/ compilation
- [ ] Command registration via `[commands.X]`
- [ ] `class` and `dependency` attributes
- [ ] `[commands.X.properties]` parsing
- [ ] Options API with type conversion
- [ ] Generated sources integration
- [ ] Multi-command execution with -D overrides

### Phase 3: Multi-Project Support

- [ ] Root bill.toml parsing with subprojects
- [ ] Dependency graph across subprojects
- [ ] Pipeline parallelism
- [ ] Cross-subproject references with cycle detection
- [ ] project.dependencies inheritance

### Phase 4: Polish

- [ ] Excellent error messages
- [ ] Incremental compilation
- [ ] IDE integration documentation
- [ ] `bill fmt`, `bill lint`
- [ ] `bill pull versions` command
- [ ] Publishing support (bill publish)

---

## Appendix A: Comparison with Existing Tools

| Feature | Maven | Gradle | Bill |
|---------|-------|--------|------|
| Config format | XML | Groovy/Kotlin DSL | TOML + Java |
| IDE parseable | ✅ | ⚠️ (requires execution) | ✅ |
| Parallel default | ❌ | ✅ | ✅ |
| Pipeline parallel | ❌ | ⚠️ | ✅ |
| Incremental | ⚠️ | ✅ | ✅ (planned) |
| Lock file | ❌ | ✅ | ❌ (not needed) |
| Reproducible | ⚠️ | ⚠️ | ✅ (required) |
| Learning curve | Medium | High | Low (goal) |
| Extensibility | Plugins | Plugins + scripts | src/bill/java/ |
| Build speed | Slow | Medium | Fast (goal) |
| JPMS integration | Optional | Optional | Encouraged |
| Version catalogs | ✅ (BOM) | ✅ | ✅ (via `bill pull versions`) |
| Version ranges | ✅ (discouraged) | ✅ | ❌ (explicit only) |
| Maven interop | N/A | ✅ | ✅ (100%) |

## Appendix B: Dependency Coordinate Mapping

Bill uses simplified coordinates. Mapping to Maven coordinates:

```toml
# bill.toml
[dependencies]
guava = { version = "33.0.0-jre" }
```

Resolves to Maven coordinate: `com.google.guava:guava:33.0.0-jre`

**Mapping rules:**
1. Check Bill registry for known mappings (guava → com.google.guava:guava)
2. Check Maven Central search API
3. Allow explicit coordinates: `"com.example:artifact:1.0.0"`

```toml
# Explicit coordinates when needed
[dependencies]
my-internal-lib = { version = "com.company:my-internal-lib:1.0.0" }
```

---

## Appendix C: Glossary

| Term | Definition |
|------|------------|
| **Project** | A Bill build unit, either standalone or containing subprojects |
| **Subproject** | A child project within a multi-project build (Maven 4 terminology) |
| **bill.toml** | Declarative manifest file (IDE-parseable) |
| **src/bill/** | Build-time code directory containing commands |
| **Command** | A class with main() registered via `[commands.X]` in bill.toml |
| **Target** | Build output directory |
| **Scope** | Dependency visibility: default, compile-only, runtime-only, test-only, build-only |
| **Pipeline Parallelism** | Subprojects advance phases independently once dependencies complete |
| **compile-only** | Dependency needed for compilation but not packaged (servlet-api, lombok) |
| **runtime-only** | Dependency packaged but not compiled against (JDBC drivers, SLF4J impls) |
| **test-only** | Dependency only available during testing (JUnit, Mockito) |
| **build-only** | Dependency only available to src/bill/java/ (code generators, command plugins) |
