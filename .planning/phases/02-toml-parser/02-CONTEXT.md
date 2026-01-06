# Phase 2: TOML Parser - Context

**Gathered:** 2026-01-06
**Status:** Ready for planning

<vision>
## How This Should Work

When users write their bill.toml file, Bill should feel like Cargo - helpful and trustworthy. If something's wrong with bill.toml, users see ALL errors at once with exact line numbers and clear explanations of what's wrong and how to fix it.

No whack-a-mole. No cryptic messages. Each error should teach users through clarity: not just "invalid version" but "version must be semantic (e.g., 1.0.0) but got 'latest'".

Users should trust Bill because when parsing fails, the errors help them fix everything in one pass. The quality of error messages is what makes a build tool feel good to use.

</vision>

<essential>
## What Must Be Nailed

- **Error reporting quality** - Users trust Bill because errors help. This is the #1 priority for Phase 2.
  - Show ALL errors at once (not fail-fast whack-a-mole)
  - Each error has exact location (line:column)
  - Each error explains WHY it's wrong and suggests how to fix it
  - Users should be able to fix everything in one editing pass

- **bill.toml structure is well-defined** - This is user-facing and hard to change later, but error quality comes first

</essential>

<boundaries>
## What's Out of Scope

- **Dependency resolution** - Phase 2 just parses what dependencies are declared. Actually resolving transitive dependencies and handling conflicts happens in Phase 4.

- **Downloading artifacts** - Phase 2 reads "I need guava:33.0.0" from bill.toml but doesn't download anything. Maven client downloads in Phase 3.

- **Workspace/multi-module support** - Single-project bill.toml only for now. Multi-module workspaces are future complexity, out of scope for MVP.

- **Advanced TOML features** - No inheritance, includes, or templating. Just straightforward TOML 1.0 parsing. Keep it simple.

</boundaries>

<specifics>
## Specific Ideas

### File Structure
- **bill.toml** - Team settings, checked into git, shared by all developers
- **bill.local.toml** - User-specific overrides for environment properties, gitignored

Users can only override environment-specific properties in bill.local.toml:
- Maven repository URLs/mirrors (e.g., corporate Nexus instead of Maven Central)
- JDK path (but NOT version - version stays team-controlled for consistency)
- Compiler flags and build options

### bill.toml Structure

**[project] section** (not [package] - "package" means something else in Java):
```toml
[project]
name = "my-java-app"
version = "1.0.0"
description = "Optional description"
```

**[dependencies] section** - All dependencies in one section (no separate dev-dependencies or test-dependencies per design.md). Scope is an attribute of each dependency:
```toml
[dependencies]
"com.google.guava:guava" = { version = "33.0.0-jre", scope = "compile" }
"org.testng:testng" = { version = "7.10.2", scope = "test" }
```

Dependencies use Maven coordinates as keys (groupId:artifactId). This is familiar to Java developers and matches Maven/Gradle conventions.

**[build] section** (example - team settings):
```toml
[build]
release = "25"
```

Note: Bill initially targets JDK 25 and newer, using the modern `release` flag (not separate `source`/`target`). Support for older JDKs may be added later.

### bill.local.toml Structure (example - user overrides):
```toml
[build]
jdk_path = "/usr/local/my-custom-jdk-25"

[repositories]
maven_central_mirror = "https://my-corp-nexus.com/maven"
```

</specifics>

<notes>
## Additional Context

The vision draws heavily from Cargo's error reporting philosophy - errors that teach users and build trust in the tool. The key differentiator is error quality over everything else.

The file structure (bill.toml + bill.local.toml) is a hybrid approach:
- Not purely Cargo-style (separate manifest and config files)
- Not purely Maven-style (single pom.xml with everything)
- Instead: team settings in one file, user overrides in another, with clear boundaries on what can be overridden

This design anticipates future multi-module support (though that's out of scope for Phase 2) by keeping settings that should be shared at workspace level (version, Maven mirrors) separate from settings that vary per developer (JDK path).

</notes>

---

*Phase: 02-toml-parser*
*Context gathered: 2026-01-06*
