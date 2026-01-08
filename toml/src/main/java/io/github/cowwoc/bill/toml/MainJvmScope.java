package io.github.cowwoc.bill.toml;

import io.github.cowwoc.bill.core.JvmScope;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.dataformat.toml.TomlMapper;

/**
 * The main implementation of {@code JvmScope} for production use.
 */
public final class MainJvmScope implements JvmScope
{
  private final TomlMapper tomlMapper;

  /**
   * Creates a new JVM scope.
   */
  public MainJvmScope()
  {
    SimpleModule module = new SimpleModule();
    module.addSerializer(DependencyScope.class, new DependencyScopeSerializer());
    module.addDeserializer(DependencyScope.class, new DependencyScopeDeserializer());

    this.tomlMapper = TomlMapper.builder()
      .addModule(module)
      .build();
  }

  @Override
  public TomlMapper getTomlMapper()
  {
    return tomlMapper;
  }

  private static final class DependencyScopeSerializer extends StdSerializer<DependencyScope>
  {
    DependencyScopeSerializer()
    {
      super(DependencyScope.class);
    }

    @Override
    public void serialize(DependencyScope value, JsonGenerator gen, SerializationContext ctxt)
    {
      gen.writeString(value.toToml());
    }
  }

  private static final class DependencyScopeDeserializer extends StdDeserializer<DependencyScope>
  {
    DependencyScopeDeserializer()
    {
      super(DependencyScope.class);
    }

    @Override
    public DependencyScope deserialize(JsonParser p, DeserializationContext ctxt)
    {
      String value = p.getValueAsString();
      return DependencyScope.fromToml(value);
    }
  }
}
