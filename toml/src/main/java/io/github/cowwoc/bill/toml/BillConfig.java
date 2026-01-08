package io.github.cowwoc.bill.toml;

import java.util.Map;

import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.requireThat;

/**
 * Complete configuration parsed from bill.toml.
 *
 * @param project      project information from {@code [project]} section
 * @param dependencies dependencies from {@code [dependencies]} section (key is coordinates)
 * @param build        build settings from {@code [build]} section (nullable)
 */
public record BillConfig(
  ProjectInfo project,
  Map<String, Dependency> dependencies,
  BuildSettings build)
{
  /**
   * Creates a configuration ensuring dependencies is never {@code null}.
   *
   * @param project      project information
   * @param dependencies dependencies map (nullable, defaults to empty map)
   * @param build        build settings (nullable)
   * @throws NullPointerException if {@code project} is {@code null}
   */
  public BillConfig
  {
    requireThat(project, "project").isNotNull();
    if (dependencies == null)
    {
      dependencies = Map.of();
    }
  }
}
