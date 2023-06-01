package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class that records method performance data from the method interceptor.
 */
final class ProfilingState {
  private final Map<String, Duration> data = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> invocationCounter = new ConcurrentHashMap<>();
  /**
   * Records the given method invocation data.  Invocation counter added with help from
   * https://stackoverflow.com/questions/3339801/atomically-incrementing-counters-stored-in-concurrenthashmap and
   * slide 33 of https://www.slideshare.net/sjlee0/robust-and-scalable-concurrent-programming-lesson-from-the-trenches
   *
   * @param callingClass the Java class of the object that called the method.
   * @param method       the method that was called.
   * @param elapsed      the amount of time that passed while the method was called.
   */
  void record(Class<?> callingClass, Method method, Duration elapsed, Long threadId) {
    Objects.requireNonNull(callingClass);
    Objects.requireNonNull(method);
    Objects.requireNonNull(elapsed);
    Objects.requireNonNull(threadId);
    if (elapsed.isNegative()) {
      throw new IllegalArgumentException("negative elapsed time");
    }
    String key = formatMethodCall(callingClass, method, threadId);
    data.compute(key, (k, v) -> (v == null) ? elapsed : v.plus(elapsed));
    incrementInvocationCount(callingClass.getName() + "#" + method.getName());
  }

  /**
   * Writes the method invocation data to the given {@link Writer}.
   *
   * <p>Recorded data is aggregated across calls to the same method. For example, suppose
   * {@link #record(Class, Method, Duration) record} is called three times for the same method
   * {@code M()}, with each invocation taking 1 second. The total {@link Duration} reported by
   * this {@code write()} method for {@code M()} should be 3 seconds.
   */
  void write(Writer writer) throws IOException {
    List<String> entries =
        data.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + " took " + formatDuration(e.getValue()) + System.lineSeparator())
            .collect(Collectors.toList());

    // We have to use a for-loop here instead of a Stream API method because Writer#write() can
    // throw an IOException, and lambdas are not allowed to throw checked exceptions.
    for (String entry : entries) {
      writer.write(entry);
    }

    List<String> counters =
            invocationCounter.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + " was invoked " + e.getValue() + " times " + System.lineSeparator())
                    .collect(Collectors.toList());

    // We have to use a for-loop here instead of a Stream API method because Writer#write() can
    // throw an IOException, and lambdas are not allowed to throw checked exceptions.
    for (String count : counters) {
      writer.write(count);
    }
  }

  /**
   * Formats the given method call for writing to a text file.
   *
   * @param callingClass the Java class of the object whose method was invoked.
   * @param method       the Java method that was invoked.
   * @return a string representation of the method call.
   */
  private static String formatMethodCall(Class<?> callingClass, Method method, Long threadId) {
    return String.format("%s#%s on threadId %s", callingClass.getName(), method.getName(), threadId);
  }

  /**
   * Formats the given {@link Duration} for writing to a text file.
   */
  private static String formatDuration(Duration duration) {
    return String.format(
        "%sm %ss %sms", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
  }

  /**
   * Increments the counter if present, adds the key beforehand if not.
   * Invocation counter added with help from
   * https://stackoverflow.com/questions/3339801/atomically-incrementing-counters-stored-in-concurrenthashmap and
   * slide 33 of https://www.slideshare.net/sjlee0/robust-and-scalable-concurrent-programming-lesson-from-the-trenches
   *
   * @param key String representation of the Java class and method name to count invokes against.
   */
  public void incrementInvocationCount(String key) {
    AtomicInteger value = invocationCounter.get(key);
    if (value == null) {
      value = new AtomicInteger(0);
      AtomicInteger old = invocationCounter.putIfAbsent(key, value);
      if (old != null) { value = old;}
    }
    value.incrementAndGet();
  }
}
