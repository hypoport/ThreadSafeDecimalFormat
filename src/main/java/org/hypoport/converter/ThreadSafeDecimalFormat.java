package org.hypoport.converter;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ThreadSafeDecimalFormat {

  private final ThreadLocal<DecimalFormat> decimalFormat;

  private final List<Consumer<DecimalFormat>> callBacks = new ArrayList<>();

  private boolean formatOrParseAlreadyCalled;

  public ThreadSafeDecimalFormat() {
    decimalFormat = new ThreadLocal<DecimalFormat>() {
      @Override
      protected DecimalFormat initialValue() {
        return setup(new DecimalFormat());
      }
    };
  }

  public ThreadSafeDecimalFormat(final String pattern) {
    decimalFormat = new ThreadLocal<DecimalFormat>() {
      @Override
      protected DecimalFormat initialValue() {
        return setup(new DecimalFormat(pattern));
      }
    };
  }

  public ThreadSafeDecimalFormat(final Locale locale) {
    decimalFormat = new ThreadLocal<DecimalFormat>() {
      @Override
      protected DecimalFormat initialValue() {
        return setup((DecimalFormat) DecimalFormat.getInstance(locale));
      }
    };
  }

  public ThreadSafeDecimalFormat(final String pattern, final DecimalFormatSymbols symbols) {
    decimalFormat = new ThreadLocal<DecimalFormat>() {
      @Override
      protected DecimalFormat initialValue() {
        return setup(new DecimalFormat(pattern, symbols));
      }
    };
  }

  private DecimalFormat setup(DecimalFormat decimalFormat) {
    callBacks.stream().forEach(consumer -> consumer.accept(decimalFormat));
    return decimalFormat;
  }

  public void setRoundingMode(RoundingMode roundingMode) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setRoundingMode(roundingMode));
  }

  public void setMinimumIntegerDigits(int minimumIntegerDigits) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setMinimumIntegerDigits(minimumIntegerDigits));
  }

  public void setMaximumIntegerDigits(int maximumIntegerDigits) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setMaximumIntegerDigits(maximumIntegerDigits));
  }

  public void setMinimumFractionDigits(int minimumFractionDigits) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setMinimumFractionDigits(minimumFractionDigits));
  }

  public void setMaximumFractionDigits(int maximumFractionDigits) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setMaximumFractionDigits(maximumFractionDigits));
  }

  public void setParseBigDecimal(boolean parseBigDecimal) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setParseBigDecimal(parseBigDecimal));
  }

  public void setGroupingUsed(boolean groupingUsed) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setGroupingUsed(groupingUsed));
  }

  public void setGroupingSize(int groupingSize) {
    ensureSetterIsAvailable();
    callBacks.add(decimalFormat -> decimalFormat.setGroupingSize(groupingSize));
  }

  private void ensureSetterIsAvailable() {
    if (formatOrParseAlreadyCalled) {
      throw new IllegalStateException("format or parse already called");
    }
  }

  public Number parse(String source) throws ParseException {
    formatOrParseAlreadyCalled = true;
    return decimalFormat.get().parse(source);
  }

  public String format(Object obj) {
    formatOrParseAlreadyCalled = true;
    return decimalFormat.get().format(obj);
  }
}
