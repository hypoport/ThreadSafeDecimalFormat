package org.hypoport.converter;

import org.hypoport.converter.ThreadUtils.Holder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import static java.util.Arrays.stream;
import static java.util.Locale.GERMAN;
import static java.util.Optional.of;
import static org.hypoport.converter.ThreadUtils.runInNewThread;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@SuppressWarnings("MagicNumber")
@Test
public class ThreadSafeDecimalFormatTest {

  private final Random random = new Random();

  private ThreadSafeDecimalFormat underTest;

  @BeforeMethod
  public void setup() {
    underTest = new ThreadSafeDecimalFormat("##0.00", new DecimalFormatSymbols(GERMAN));
  }

  public void testConstructors() {
    new ThreadSafeDecimalFormat().format(new BigDecimal(random.nextDouble()));

    new ThreadSafeDecimalFormat().format(new BigDecimal(random.nextDouble()));
    new ThreadSafeDecimalFormat(GERMAN).format(new BigDecimal(random.nextDouble()));
    new ThreadSafeDecimalFormat("0,00").format(new BigDecimal(random.nextDouble()));
    new ThreadSafeDecimalFormat("0,00", DecimalFormatSymbols.getInstance(GERMAN)).format(new BigDecimal(random.nextDouble()));
  }

  @DataProvider
  public Object[][] dp_format() {
    return new Object[][] {
        {new Double(10.149), "10,15"},
        {new Double(10.151), "10,15"},
        {new Double(11), "11,00"}
    };
  }

  @Test(dataProvider = "dp_format")
  public void format(Double src, String expectedValue) throws Throwable {

    assertEquals(underTest.format(src), expectedValue);
  }

  @Test(dataProvider = "dp_format")
  public void format_InNeuemThread(Double src, String expectedValue) throws Throwable {
    Holder<String> result = new Holder<>();
    runInNewThread(() -> result.setValue(of(underTest.format(src))));

    assertTrue(result.isPresent());
    assertEquals(result.get(), expectedValue);
  }

  public void format2xHintereinander() {
    assertEquals(underTest.format(new Double(10.149)), "10,15");
    assertEquals(underTest.format(new Double(10.151)), "10,15");
  }

  @DataProvider
  public Object[][] dp_parse() {
    return new Object[][] {
        {"10,15", new Double(10.15)},
        {"10,161", new Double(10.161)},
        {"11,01", new Double(11.01)}
    };
  }

  @Test(dataProvider = "dp_parse")
  public void parse(String src, Number expectedValue) throws Throwable {
    assertEquals(underTest.parse(src), expectedValue);
  }

  @Test(dataProvider = "dp_parse")
  public void parse_InNeuemThread(String src, Number expectedValue) throws Throwable {
    Holder<Number> result = new Holder<>();
    runInNewThread(createParseRunnable(underTest, src, result));

    assertTrue(result.isPresent());
    assertEquals(result.get(), expectedValue);
  }

  public void parse2xHintereinander() throws ParseException {
    assertEquals(underTest.parse("10,15"), new Double(10.15));
    assertEquals(underTest.parse("11,16"), new Double(11.16));
  }

  private Runnable createParseRunnable(ThreadSafeDecimalFormat decimalFormat, String value, Holder<Number> holder) {
    return () -> {
      try {
        holder.setValue(of(decimalFormat.parse(value)));
      }
      catch (ParseException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public void hintTestSetterMethod() {
    assertEquals(stream(ThreadSafeDecimalFormat.class.getMethods()).filter(method -> method.getName().startsWith("set")).count(), 8, "complement method 'testSetter' und 'assertSetterFailed'");
  }

  public void testSetter() throws Throwable {
    RoundingMode roundingMode = nextRoundingMode();
    int minimumIntegerDigits = random.nextInt(5) + 1;
    int maximumIntegerDigits = minimumIntegerDigits + random.nextInt(5);
    int minimumFractionDigits = random.nextInt(5) + 1;
    int maximumFractionDigits = minimumFractionDigits + random.nextInt(5);
    int groupingSize = random.nextInt(5) + 1;
    boolean groupingUsed = true;
    boolean parseBigDecimal = true;

    underTest.setRoundingMode(roundingMode);
    underTest.setMinimumIntegerDigits(minimumIntegerDigits);
    underTest.setMaximumIntegerDigits(maximumIntegerDigits);
    underTest.setMinimumFractionDigits(minimumFractionDigits);
    underTest.setMaximumFractionDigits(maximumFractionDigits);
    underTest.setGroupingSize(groupingSize);
    underTest.setParseBigDecimal(parseBigDecimal);
    underTest.setGroupingUsed(groupingUsed);

    runInNewThread(() -> {
      ThreadLocal<DecimalFormat> decimalFormat = getPrivateField(underTest, "decimalFormat");
      assertEquals(decimalFormat.get().getRoundingMode(), roundingMode, " roundingMode failed");
      assertEquals(decimalFormat.get().getMinimumIntegerDigits(), minimumIntegerDigits, " minimumIntegerDigits failed");
      assertEquals(decimalFormat.get().getMaximumIntegerDigits(), maximumIntegerDigits, " maximumIntegerDigits failed");
      assertEquals(decimalFormat.get().getMinimumFractionDigits(), minimumFractionDigits, " minimumFractionDigits failed");
      assertEquals(decimalFormat.get().getMaximumFractionDigits(), maximumFractionDigits, " maximumFractionDigits failed");
      assertEquals(decimalFormat.get().getGroupingSize(), groupingSize, " groupingSize failed");
      assertEquals(decimalFormat.get().isParseBigDecimal(), parseBigDecimal, " parseBigDecimal failed");
      assertEquals(decimalFormat.get().isGroupingUsed(), groupingUsed, " groupingUsed failed");
    });
  }

  public void setterCallAfterFormatFailed() throws Throwable {
    underTest.format(new BigDecimal(random.nextDouble()));
    assertSetterFailed();

    runInNewThread(() -> {
      underTest.format(new BigDecimal(random.nextDouble()));
      assertSetterFailed();
    });
  }

  public void setterCallAfterParseFailed() throws Exception {
    underTest.parse(String.valueOf(random.nextInt()));
    assertSetterFailed();
  }

  private void assertSetterFailed() {
    assertIllegalStateExceptionExpected(underTest -> underTest.setRoundingMode(nextRoundingMode()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setMinimumIntegerDigits(random.nextInt()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setMaximumIntegerDigits(random.nextInt()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setMinimumFractionDigits(random.nextInt()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setMaximumFractionDigits(random.nextInt()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setParseBigDecimal(random.nextBoolean()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setGroupingUsed(random.nextBoolean()));
    assertIllegalStateExceptionExpected(underTest -> underTest.setGroupingSize(random.nextInt()));
  }

  private void assertIllegalStateExceptionExpected(Consumer<ThreadSafeDecimalFormat> consumer) {
    try {
      consumer.accept(underTest);
      fail("IllegalStateException expected");
    }
    catch (IllegalStateException e) {
      // expected
    }
  }

  private RoundingMode nextRoundingMode() {
    return Arrays.asList(RoundingMode.values()).get(random.nextInt(RoundingMode.values().length));
  }

  @SuppressWarnings("unchecked")
  private static <T> T getPrivateField(Object object, String fieldName) {
    try {
      final Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return (T) field.get(object);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}