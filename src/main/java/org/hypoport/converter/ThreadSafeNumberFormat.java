package org.hypoport.converter;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ThreadSafeNumberFormat {

  private final ThreadLocal<NumberFormat> numberFormat;

  private final List<Consumer<NumberFormat>> callBacks = new ArrayList<>();

  private boolean formatOrParseAlreadyCalled;

  public ThreadSafeNumberFormat() {
    numberFormat = ThreadLocal.withInitial(() -> setup(new DecimalFormat()));
  }

  public ThreadSafeNumberFormat(final NumberFormat format) {
    numberFormat = ThreadLocal.withInitial(() -> setup(format));
  }

  public ThreadSafeNumberFormat(final String pattern) {
    numberFormat = ThreadLocal.withInitial(() -> setup(new DecimalFormat(pattern)));
  }

  public ThreadSafeNumberFormat(final Locale locale) {
    numberFormat = ThreadLocal.withInitial(() -> setup(DecimalFormat.getInstance(locale)));
  }

  public ThreadSafeNumberFormat(final String pattern, final DecimalFormatSymbols symbols) {
    numberFormat = ThreadLocal.withInitial(() -> setup(new DecimalFormat(pattern, symbols)));
  }

  private NumberFormat setup(NumberFormat numberFormat) {
    callBacks.forEach(consumer -> consumer.accept(numberFormat));
    return numberFormat;
  }

  public void setRoundingMode(RoundingMode roundingMode) {
    ensureSetterIsAvailable();
    callBacks.add(numberFormat -> numberFormat.setRoundingMode(roundingMode));
  }

  public void setMinimumIntegerDigits(int minimumIntegerDigits) {
    ensureSetterIsAvailable();
    callBacks.add(numberFormat -> numberFormat.setMinimumIntegerDigits(minimumIntegerDigits));
  }

  public void setMaximumIntegerDigits(int maximumIntegerDigits) {
    ensureSetterIsAvailable();
    callBacks.add(numberFormat -> numberFormat.setMaximumIntegerDigits(maximumIntegerDigits));
  }

  public void setMinimumFractionDigits(int minimumFractionDigits) {
    ensureSetterIsAvailable();
    callBacks.add(numberFormat -> numberFormat.setMinimumFractionDigits(minimumFractionDigits));
  }

  public void setMaximumFractionDigits(int maximumFractionDigits) {
    ensureSetterIsAvailable();
    callBacks.add(numberFormat -> numberFormat.setMaximumFractionDigits(maximumFractionDigits));
  }

  public void setGroupingUsed(boolean groupingUsed) {
    ensureSetterIsAvailable();
    callBacks.add(numberFormat -> numberFormat.setGroupingUsed(groupingUsed));
  }

  private void ensureSetterIsAvailable() {
    if (formatOrParseAlreadyCalled) {
      throw new IllegalStateException("format or parse already called");
    }
  }

  public Number parse(String source) throws ParseException {
    formatOrParseAlreadyCalled = true;
    return numberFormat.get().parse(source);
  }

  public String format(Number obj) {
    formatOrParseAlreadyCalled = true;
    return numberFormat.get().format(obj);
  }
}