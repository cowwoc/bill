package io.github.cowwoc.bill.toml.test;

import io.github.cowwoc.bill.core.JvmScope;
import io.github.cowwoc.bill.toml.BillConfig;
import io.github.cowwoc.bill.toml.BillConfigParser;
import io.github.cowwoc.bill.toml.ConfigParseException;
import io.github.cowwoc.bill.toml.DependencyScope;
import io.github.cowwoc.bill.toml.MainJvmScope;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.requireThat;

/**
 * Tests for {@code BillConfigParser}.
 */
public class BillConfigParserTest
{
  private final JvmScope scope = new MainJvmScope();

  /**
   * Parses a minimal bill.toml with only required fields.
   */
  @Test
  public void parseMinimalConfig() throws ConfigParseException, IOException
  {
    String toml = """
      [project]
      name = "my-app"
      version = "1.0.0"
      """;
    Path path = Files.writeString(Files.createTempFile("bill", ".toml"), toml);
    try
    {
      BillConfigParser parser = new BillConfigParser(scope);
      BillConfig config = parser.parse(path);

      requireThat(config.project().name(), "name").isEqualTo("my-app");
      requireThat(config.project().version(), "version").isEqualTo("1.0.0");
      requireThat(config.dependencies(), "dependencies").isEmpty();
      requireThat(config.build(), "build").isNull();
    }
    finally
    {
      Files.deleteIfExists(path);
    }
  }

  /**
   * Parses a full bill.toml with all optional fields populated.
   */
  @Test
  public void parseFullConfig() throws ConfigParseException, IOException
  {
    String toml = """
      [project]
      name = "my-app"
      version = "1.0.0"
      description = "A sample application"

      [dependencies]
      "org.slf4j:slf4j-api" = { version = "2.0.9" }
      "com.google.guava:guava" = { version = "33.0.0", scope = "compile" }

      [build]
      release = 21
      """;
    Path path = Files.writeString(Files.createTempFile("bill", ".toml"), toml);
    try
    {
      BillConfigParser parser = new BillConfigParser(scope);
      BillConfig config = parser.parse(path);

      requireThat(config.project().name(), "name").isEqualTo("my-app");
      requireThat(config.project().version(), "version").isEqualTo("1.0.0");
      requireThat(config.project().description(), "description").isEqualTo("A sample application");
      requireThat(config.dependencies().size(), "dependenciesSize").isEqualTo(2);
      requireThat(config.dependencies().get("org.slf4j:slf4j-api").version(), "slf4jVersion")
        .isEqualTo("2.0.9");
      requireThat(config.dependencies().get("com.google.guava:guava").scope(), "guavaScope")
        .isEqualTo(DependencyScope.COMPILE);
      requireThat(config.build().release(), "release").isEqualTo(21);
    }
    finally
    {
      Files.deleteIfExists(path);
    }
  }

  /**
   * Parsing a non-existent file throws {@code ConfigParseException}.
   */
  @Test(expectedExceptions = ConfigParseException.class)
  public void parseNonExistentFile() throws ConfigParseException, IOException
  {
    BillConfigParser parser = new BillConfigParser(scope);
    parser.parse(Path.of("does-not-exist.toml"));
  }

  /**
   * Parsing invalid TOML syntax throws {@code ConfigParseException}.
   */
  @Test(expectedExceptions = ConfigParseException.class)
  public void parseInvalidSyntax() throws ConfigParseException, IOException
  {
    String toml = "this is not valid toml [[[";
    Path path = Files.writeString(Files.createTempFile("bill", ".toml"), toml);
    try
    {
      BillConfigParser parser = new BillConfigParser(scope);
      parser.parse(path);
    }
    finally
    {
      Files.deleteIfExists(path);
    }
  }

  /**
   * Parsing a {@code null} path throws {@code NullPointerException}.
   */
  @Test(expectedExceptions = NullPointerException.class)
  public void parseNullPath() throws ConfigParseException, IOException
  {
    BillConfigParser parser = new BillConfigParser(scope);
    parser.parse(null);
  }
}
