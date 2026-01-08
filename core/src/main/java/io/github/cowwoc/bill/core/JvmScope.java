package io.github.cowwoc.bill.core;

import tools.jackson.dataformat.toml.TomlMapper;

/**
 * A scope for values that last the lifetime of the JVM.
 */
public interface JvmScope
{
  /**
   * Returns the TOML mapper for serializing and deserializing TOML content.
   *
   * @return the TOML mapper
   */
  TomlMapper getTomlMapper();
}
