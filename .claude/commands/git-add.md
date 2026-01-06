# Git Add Command

**Purpose:** Safely stage files for commit while respecting .gitignore rules.

**Usage:** `/git-add <files...>`

## Command

This command validates that files being added are not in .gitignore before staging them.

```bash
# Function to check if file should be ignored
check_gitignore() {
  local file="$1"

  # Check if file matches any .gitignore pattern
  if git check-ignore -q "$file" 2>/dev/null; then
    echo "ERROR: '$file' matches .gitignore pattern and should not be committed"
    return 1
  fi

  return 0
}

# Get all files to add
FILES="${ARGS}"

# If no files specified, error - don't allow blanket add
if [ -z "$FILES" ]; then
  echo "ERROR: Must specify files to add. Use 'git-add <files>' instead of blanket adds."
  echo "This prevents accidentally committing ignored files."
  exit 1
fi

# Validate each file
FAILED=0
for file in $FILES; do
  if ! check_gitignore "$file"; then
    FAILED=1
  fi
done

# If any files failed validation, abort
if [ $FAILED -eq 1 ]; then
  echo ""
  echo "Some files match .gitignore patterns. Review .gitignore and the files above."
  echo "If you really need to commit these files, update .gitignore first."
  exit 1
fi

# All files passed validation, stage them
git add $FILES

echo "Staged files:"
git diff --cached --name-only
```

## Safety Features

1. **No blanket adds:** Rejects `git add .` or `git add -A` style commands without specific files
2. **Gitignore validation:** Checks each file against .gitignore patterns before staging
3. **Explicit file list:** Forces explicit file specification for visibility
4. **Clear feedback:** Shows what was staged after successful add

## Examples

**Good:**
```bash
/git-add src/main/java/MyClass.java
/git-add pom.xml .planning/CONVENTIONS.md
```

**Bad (prevented):**
```bash
/git-add .                    # Error: must specify files
/git-add core/target/         # Error: matches .gitignore pattern
/git-add -A                   # Error: must specify files
```

## Rationale

Build artifacts, IDE files, and temporary files should never be committed. This command prevents accidental commits of ignored files while still allowing explicit, intentional staging of source code and documentation.
