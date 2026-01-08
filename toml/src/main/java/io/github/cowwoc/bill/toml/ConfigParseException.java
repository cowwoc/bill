package io.github.cowwoc.bill.toml;

import static io.github.cowwoc.requirements13.java.DefaultJavaValidators.requireThat;

/**
 * Exception thrown when parsing bill.toml fails.
 */
public class ConfigParseException extends Exception
{
  /**
   * Creates an exception with a message.
   *
   * @param message the error message
   * @throws NullPointerException     if {@code message} is {@code null}
   * @throws IllegalArgumentException if {@code message} is blank
   */
  public ConfigParseException(String message)
  {
    super(requireThat(message, "message").isNotBlank().getValue());
  }

  /**
   * Creates an exception with a message and cause.
   *
   * @param message the error message
   * @param cause   the underlying cause
   * @throws NullPointerException     if {@code message} or {@code cause} is {@code null}
   * @throws IllegalArgumentException if {@code message} is blank
   */
  public ConfigParseException(String message, Throwable cause)
  {
    super(requireThat(message, "message").isNotBlank().getValue(),
      requireThat(cause, "cause").isNotNull().getValue());
  }
}
