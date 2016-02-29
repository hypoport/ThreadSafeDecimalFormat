package org.hypoport.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.Executors.newFixedThreadPool;

final class ThreadUtils {

  private ThreadUtils() {
  }

  public static <T> List<T> executeParallel(final List<Callable<T>> callables, final int maxThreadCount) throws InterruptedException, ExecutionException {
    final int threadCount = callables.size() > 0 && callables.size() < maxThreadCount ? callables.size() : maxThreadCount;
    ExecutorService executor = newFixedThreadPool(threadCount);
    List<T> results = new ArrayList<>();
    try {
      for (Future<T> future : executor.invokeAll(callables)) {
        results.add(future.get());
      }
    }
    finally {
      executor.shutdown();
    }
    return results;
  }

  public static void runInNewThread(Runnable runnable) throws Throwable {
    Holder<Throwable> exceptionHolder = new Holder<>();
    try {
      final Thread thread = new Thread(runnable);
      thread.setUncaughtExceptionHandler((t, exception) -> exceptionHolder.setValue(of(exception)));
      thread.start();
      thread.join();
      if (exceptionHolder.isPresent()) {
        throw exceptionHolder.get();
      }
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static class Holder<T> {

    private Optional<T> value = empty();

    public void setValue(Optional<T> value) {
      if (value == null) {
        throw new AssertionError("value must be non null");
      }
      this.value = value;
    }

    public T get() {
      return value.get();
    }

    public boolean isPresent() {
      return value.isPresent();
    }
  }
}
