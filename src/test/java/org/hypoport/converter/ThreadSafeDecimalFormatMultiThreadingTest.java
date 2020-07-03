package org.hypoport.converter;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.hypoport.converter.ThreadUtils.executeParallel;
import static org.testng.Assert.assertTrue;

@Test
public class ThreadSafeDecimalFormatMultiThreadingTest {

  private final int threadCount = 20;
  private final int instanceCount = 4;
  private final int testCount = 1000;

  private final Random random = new Random();

  private final List<ThreadSafeNumberFormat> underTest = new LinkedList<>();

  @BeforeClass
  public void setupClass() {
    IntStream.rangeClosed(1, instanceCount).forEach(v -> underTest.add(new ThreadSafeNumberFormat()));
  }

  public void multithreaded_singleInstance_ok() throws Exception {
    for (int i = 0; i < testCount; i++) {
      final List<Optional<String>> optionals = executeParallel(createCallables(true), threadCount);
      optionals.forEach(value -> assertTrue(value.isPresent()));
    }
  }

  public void multithreaded_multipleInstances_ok() throws Exception {
    for (int i = 0; i < testCount * 10; i++) {
      final List<Optional<String>> optionals = executeParallel(createCallables(false), threadCount);
      optionals.forEach(value -> assertTrue(value.isPresent()));
    }
  }

  private List<Callable<Optional<String>>> createCallables(final boolean isSingleInstance) {
    return IntStream.rangeClosed(1, threadCount).mapToObj(v -> createCallable(isSingleInstance)).collect(toList());
  }

  private Callable<Optional<String>> createCallable(final boolean isSingleInstance) {
    return () -> {
      try {
        final ThreadSafeNumberFormat numberFormat = isSingleInstance ? underTest.get(0) : underTest.get(random.nextInt(instanceCount));
        return of(numberFormat.format(new BigDecimal(random.nextInt())));
      }
      catch (Exception e) {
        return empty();
      }
    };
  }
}