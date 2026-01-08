package io.github.cowwoc.bill.toml;

import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.requireThat;

/**
 * Project information from the {@code [project]} section of bill.toml.
 *
 * @param name        the project name
 * @param version     the project version
 * @param description the project description (empty string if not specified)
 */
public record ProjectInfo(
  String name,
  String version,
  String description)
{
  /**
   * Creates project information.
   *
   * @param name        the project name
   * @param version     the project version
   * @param description the project description (nullable, defaults to empty string)
   * @throws NullPointerException     if {@code name} or {@code version} is {@code null}
   * @throws IllegalArgumentException if {@code name} or {@code version} is blank
   */
  public ProjectInfo
  {
    requireThat(name, "name").isNotBlank();
    requireThat(version, "version").isNotBlank();
    if (description == null)
    {
      description = "";
    }
  }
}
