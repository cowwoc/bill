# Phase 1: Foundation - Context

**Gathered:** 2026-01-06
**Status:** Ready for planning

<vision>
## How This Should Work

Bill's foundation should establish a clean modular structure with clear separation of responsibilities. The structure should create only the modules needed for the immediate phases (Phase 1-3): core utilities, TOML parser module, and Maven client module. Additional modules will be added as subsequent phases require them.

The project should follow standard Maven conventions throughout - typical directory layouts (src/main/java), standard parent POM patterns, and nothing unconventional. The goal is to make the structure immediately familiar to any Java developer who has worked with Maven projects.

</vision>

<essential>
## What Must Be Nailed

- **Clean module boundaries** - The separation between modules must be crystal clear with well-defined responsibilities and minimal coupling. Each module should have a single, obvious purpose that won't blur as the codebase grows.

</essential>

<boundaries>
## What's Out of Scope

- Performance optimization - Don't worry about build speed, caching strategies, or optimization concerns. Phase 1 is about getting the structure right, not making it fast.

</boundaries>

<specifics>
## Specific Ideas

- Follow standard Maven conventions throughout
- Use typical Maven directory layouts (src/main/java, src/test/java, etc.)
- Standard parent POM patterns
- Nothing unconventional - keep it familiar to Maven developers

</specifics>

<notes>
## Additional Context

The user wants to create modules as they're needed rather than creating all anticipated modules upfront. Initial modules for Phase 1-3:
- Core utilities module (shared foundations)
- TOML parser module (Phase 2 work)
- Maven client module (Phase 3 work)

Additional modules (dependency resolver, compiler, test runner, JAR packager, CLI) will be added in later phases when their functionality is being implemented.

</notes>

---

*Phase: 01-foundation*
*Context gathered: 2026-01-06*
