package io.github.cowwoc.bill.toml;

import static java.util.Locale.ROOT;

/**
 * The scope of a dependency, determining when it's available on the classpath.
 */
public enum DependencyScope
{
  /**
   * Available at compile time and runtime. This is the default scope.
   */
  COMPILE,

  /**
   * Available only during test compilation and execution.
   */
  TEST,

  /**
   * Available only at runtime, not needed for compilation.
   */
  RUNTIME,

  /**
   * Available at compile time but not packaged (provided by the runtime environment).
   */
  PROVIDED;

  /**
   * Returns the TOML representation of this scope.
   *
   * @return the TOML string value
   */
  public String toToml()
  {
    return name().toLowerCase(ROOT);
  }

  /**
   * Parses a TOML string into a {@code DependencyScope}.
   *
   * @param toml the TOML string value
   * @return the corresponding scope
   * @throws IllegalArgumentException if {@code toml} is not a valid scope
   */
  public static DependencyScope fromToml(String toml)
  {
    return valueOf(toml.toUpperCase(ROOT));
  }
}
