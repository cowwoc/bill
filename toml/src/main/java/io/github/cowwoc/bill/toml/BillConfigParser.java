package io.github.cowwoc.bill.toml;

import io.github.cowwoc.bill.core.JvmScope;
import tools.jackson.dataformat.toml.TomlMapper;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.requireThat;

/**
 * Parses bill.toml configuration files into {@code BillConfig} objects.
 */
public final class BillConfigParser
{
  private final TomlMapper mapper;

  /**
   * Creates a new parser.
   *
   * @param scope the JVM scope providing dependencies
   * @throws NullPointerException if {@code scope} is {@code null}
   */
  public BillConfigParser(JvmScope scope)
  {
    requireThat(scope, "scope").isNotNull();
    this.mapper = scope.getTomlMapper();
  }

  /**
   * Parses a bill.toml file into a {@code BillConfig} object.
   *
   * @param path the path to the bill.toml file
   * @return the parsed configuration
   * @throws NullPointerException if {@code path} is {@code null}
   * @throws ConfigParseException if {@code path} does not exist or contains invalid TOML
   * @throws IOException          if an I/O error occurs
   */
  public BillConfig parse(Path path) throws ConfigParseException, IOException
  {
    requireThat(path, "path").isNotNull();
    try
    {
      requireThat(path, "path").exists();
    }
    catch (IllegalArgumentException e)
    {
      throw new ConfigParseException(e.getMessage(), e);
    }
    try
    {
      return mapper.readValue(path.toFile(), BillConfig.class);
    }
    catch (tools.jackson.core.JacksonException e)
    {
      throw new ConfigParseException("Failed to parse " + path + ": " + e.getOriginalMessage(), e);
    }
  }
}
