# Bill

## What This Is

Bill is a modern build system for Java that combines Cargo's philosophy of opinionated simplicity with Java's ecosystem requirements. It enforces a sharp boundary between declarative configuration (bill.toml) and imperative build logic, prioritizing IDE analyzability, fast feedback loops, and predictable builds.

## Core Value

Dependency resolution must work flawlessly — fail-fast on conflicts, handle all scopes correctly, and maintain 100% Maven interoperability. Everything else can be refined, but dependencies are the foundation.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] bill.toml parser (TOML format, IDE-parseable)
- [ ] Dependency resolution from Maven Central
- [ ] Fail-fast on transitive version ranges
- [ ] Dependency scopes (default, compile-only, runtime-only, test-only, build-only)
- [ ] Basic compilation (javac wrapper)
- [ ] Test execution (TestNG)
- [ ] `bill build`, `bill test`, `bill run` commands
- [ ] Reproducible JAR creation

### Out of Scope

- src/bill/java/ build commands — Phase 2
- Multi-project builds — Phase 3
- Incremental compilation — Phase 4
- `bill fmt`, `bill lint` — Phase 4
- `bill pull versions` command — Phase 4
- Publishing support — Phase 4
- Remote/shared caching — future version

## Context

The design is documented in `docs/design.md`. Key design principles:

1. **Static analyzability first** — IDEs must understand projects without executing build code
2. **Opinionated defaults** — Zero configuration for standard projects
3. **Declarative/imperative boundary** — bill.toml for what, src/bill/java/ for how
4. **Composition over extension** — External tools over plugin systems
5. **Fast feedback** — Incremental compilation, parallel builds by default
6. **100% reproducible** — Same inputs always produce identical outputs
7. **Maven interoperability** — Full compatibility with existing Maven-published artifacts

Project structure follows Maven conventions for src/main/ and src/test/, with bill.toml replacing pom.xml for declarative configuration.

## Constraints

- **Language**: Java — Bill is written in Java (eating our own dogfood)
- **Target JDK**: 25+ — Using latest JDK features
- **Test Framework**: TestNG — For Bill's own test suite
- **Bootstrap**: Maven — Used to build Bill until it can build itself
- **CI**: GitHub Actions — Automated testing and releases

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Java for implementation | Familiar ecosystem, dogfooding potential | — Pending |
| JDK 25+ target | Access to latest language features | — Pending |
| TestNG for testing | User preference | — Pending |
| Maven for bootstrap | Standard, reliable, temporary until self-hosting | — Pending |
| Phase 1 scope only | Ship MVP fast, validate core assumptions | — Pending |
| Dependency resolution as core priority | Foundation everything else builds on | — Pending |

---
*Last updated: 2026-01-06 after initialization*
