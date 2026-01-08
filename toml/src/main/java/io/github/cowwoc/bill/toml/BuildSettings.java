package io.github.cowwoc.bill.toml;

/**
 * Build settings from the {@code [build]} section of bill.toml.
 *
 * @param release the JDK version (defaults to {@code 25})
 */
public record BuildSettings(Integer release)
{
  private static final int DEFAULT_RELEASE = 25;

  /**
   * Creates build settings with default release of {@code 25} if not specified.
   *
   * @param release the JDK version (nullable, defaults to {@code 25})
   */
  public BuildSettings
  {
    if (release == null)
    {
      release = DEFAULT_RELEASE;
    }
  }
}
