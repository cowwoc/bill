# Roadmap: Bill

## Overview

Bill transforms Java development by bringing Cargo-style simplicity to the Java ecosystem. This roadmap covers the v1.0 MVP: a working build system that can parse bill.toml, resolve dependencies from Maven Central, compile code, run tests, and produce reproducible JARs.

## Domain Expertise

None

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation** - Maven bootstrap, project structure, module setup
- [ ] **Phase 2: TOML Parser** - bill.toml parsing, configuration model
- [ ] **Phase 3: Maven Client** - HTTP client for Maven Central, artifact downloading/caching
- [ ] **Phase 4: Dependency Resolution** - Core algorithm, transitive dependencies, fail-fast conflicts
- [ ] **Phase 5: Dependency Scopes** - Scope handling (compile-only, runtime-only, test-only, build-only)
- [ ] **Phase 6: Compilation** - javac wrapper, source compilation, classpath assembly
- [ ] **Phase 7: Test Execution** - TestNG integration, test runner
- [ ] **Phase 8: JAR Packaging** - Reproducible JAR creation with deterministic output
- [ ] **Phase 9: CLI Interface** - `bill build`, `bill test`, `bill run` commands
- [ ] **Phase 10: Integration** - End-to-end testing, error handling, documentation

## Phase Details

### Phase 1: Foundation
**Goal**: Bootstrap project with Maven, establish module structure, basic project skeleton
**Depends on**: Nothing (first phase)
**Research**: Unlikely (standard Maven project setup)
**Plans**: TBD

### Phase 2: TOML Parser
**Goal**: Parse bill.toml files into configuration model
**Depends on**: Phase 1
**Research**: Likely (library evaluation)
**Research topics**: Java TOML libraries (toml4j, jackson-dataformat-toml), API ergonomics, error reporting quality
**Plans**: TBD

### Phase 3: Maven Client
**Goal**: Download artifacts from Maven Central, implement local caching
**Depends on**: Phase 1
**Research**: Likely (Maven repository protocol)
**Research topics**: Maven repository layout, POM schema, HTTP client options, checksum verification
**Plans**: TBD

### Phase 4: Dependency Resolution
**Goal**: Resolve transitive dependencies, detect and fail-fast on conflicts
**Depends on**: Phase 2, Phase 3
**Research**: Likely (resolution algorithms)
**Research topics**: Maven dependency mediation, version range handling, conflict resolution strategies, nearest-wins vs first-wins
**Plans**: TBD

### Phase 5: Dependency Scopes
**Goal**: Handle all scope types, construct correct classpaths per scope
**Depends on**: Phase 4
**Research**: Unlikely (internal logic building on phase 4)
**Plans**: TBD

### Phase 6: Compilation
**Goal**: Compile Java sources using javac, manage classpath assembly
**Depends on**: Phase 5
**Research**: Unlikely (javac tooling well-documented)
**Plans**: TBD

### Phase 7: Test Execution
**Goal**: Run TestNG tests programmatically, report results
**Depends on**: Phase 6
**Research**: Likely (TestNG API)
**Research topics**: TestNG programmatic execution API, result listeners, parallel execution options
**Plans**: TBD

### Phase 8: JAR Packaging
**Goal**: Create reproducible JARs with deterministic output
**Depends on**: Phase 6
**Research**: Likely (reproducible builds)
**Research topics**: Reproducible JAR creation, timestamp normalization, manifest best practices, entry ordering
**Plans**: TBD

### Phase 9: CLI Interface
**Goal**: Implement `bill build`, `bill test`, `bill run` commands
**Depends on**: Phase 7, Phase 8
**Research**: Unlikely (standard CLI patterns)
**Plans**: TBD

### Phase 10: Integration
**Goal**: End-to-end testing, error handling polish, user documentation
**Depends on**: Phase 9
**Research**: Unlikely (internal testing and polish)
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 0/? | Not started | - |
| 2. TOML Parser | 0/? | Not started | - |
| 3. Maven Client | 0/? | Not started | - |
| 4. Dependency Resolution | 0/? | Not started | - |
| 5. Dependency Scopes | 0/? | Not started | - |
| 6. Compilation | 0/? | Not started | - |
| 7. Test Execution | 0/? | Not started | - |
| 8. JAR Packaging | 0/? | Not started | - |
| 9. CLI Interface | 0/? | Not started | - |
| 10. Integration | 0/? | Not started | - |
