package io.github.cowwoc.bill.toml;

import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.requireThat;

/**
 * A dependency from the {@code [dependencies]} section of bill.toml.
 *
 * @param version the version
 * @param scope   the dependency scope (defaults to {@link DependencyScope#COMPILE})
 */
public record Dependency(
  String version,
  DependencyScope scope)
{
  /**
   * Creates a dependency with default scope of {@link DependencyScope#COMPILE} if not specified.
   *
   * @param version the version
   * @param scope   the scope (nullable, defaults to {@link DependencyScope#COMPILE})
   * @throws NullPointerException     if {@code version} is {@code null}
   * @throws IllegalArgumentException if {@code version} is blank
   */
  public Dependency
  {
    requireThat(version, "version").isNotBlank();
    if (scope == null)
    {
      scope = DependencyScope.COMPILE;
    }
  }
}
