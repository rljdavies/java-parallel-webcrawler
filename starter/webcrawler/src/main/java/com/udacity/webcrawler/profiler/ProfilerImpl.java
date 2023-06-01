package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Arrays;
import java.nio.file.StandardOpenOption;
import java.lang.reflect.Proxy;
import java.nio.file.Files;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    //Profiler.wrap() should throw an IllegalArgumentException if the wrapped interface does not contain a @Profiled method.
    if (!Arrays.stream(klass.getDeclaredMethods()).anyMatch(m -> m.getAnnotation(Profiled.class) != null)) {
      throw new IllegalArgumentException();
    }
    T proxy = (T) Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class<?>[] {klass},
            new ProfilingMethodInterceptor(this.clock, this.state, delegate)
    );
    return proxy;
  }

  @Override
  public void writeData(Path path) throws IOException {
    try (Writer writer = Files.newBufferedWriter(path,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND)) {
      writeData(writer);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
