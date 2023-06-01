package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Objects;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState profilingState;
  private final Object delegate;

  ProfilingMethodInterceptor(Clock clock, ProfilingState profilingState, Object delegate) {
    this.clock = Objects.requireNonNull(clock);
    this.profilingState = profilingState;
    this.delegate = delegate;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object resultObject = null;
    Instant start = null;

    //check for Profiled and set start time if true
    if (method.isAnnotationPresent(Profiled.class)) {
      start = clock.instant();
    }
    //invoke method correctly propagating exceptions whilst (hopefully) ensuring no undeclared throwable exceptions
    try {
        resultObject = method.invoke(delegate, args);
    } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
        throw e.getTargetException();
    } catch (Exception e) {
        throw e.getCause();
    } finally {
        //always record running time for profiled methods, even if e thrown
        if (start != null) {
          profilingState.record(delegate.getClass(), method, Duration.between(start, clock.instant()),Thread.currentThread().getId());
        }
    }
    return resultObject;
  }
}
