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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

import static java.util.Arrays.stream;
import static java.util.Locale.GERMAN;
import static java.util.Optional.of;
import static org.hypoport.converter.ThreadUtils.runInNewThread;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@SuppressWarnings({"MagicNumber", "ClassWithTooManyMethods"})
@Test
public class ThreadSafeNumberFormatTest {

  private ThreadSafeNumberFormat underTest;

  private final Random random = new Random();

  @BeforeMethod
  public void setup() {
    underTest = new ThreadSafeNumberFormat("##0.00", new DecimalFormatSymbols(GERMAN));
  }

  public void testConstructors() {
    new ThreadSafeNumberFormat().format(createRandomBigDecimal());
    new ThreadSafeNumberFormat(DecimalFormat.getCurrencyInstance(Locale.GERMANY));
    new ThreadSafeNumberFormat(GERMAN).format(createRandomBigDecimal());
    new ThreadSafeNumberFormat("0,00").format(createRandomBigDecimal());
    new ThreadSafeNumberFormat("0,00", DecimalFormatSymbols.getInstance(GERMAN)).format(createRandomBigDecimal());
  }

  public void testSetterTwiceAndEuro() {
    final ThreadSafeNumberFormat numberFormat = new ThreadSafeNumberFormat(DecimalFormat.getCurrencyInstance(Locale.GERMANY));
    numberFormat.setGroupingUsed(true);
    numberFormat.setGroupingUsed(false);
    numberFormat.setGroupingUsed(true);
    final String actual = numberFormat.format(new BigDecimal("1234567.894"));
    assertEquals(actual.replace('\u00A0', ' '), "1.234.567,89 €");
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
    assertEquals(underTest.format(10.149), "10,15");
    assertEquals(underTest.format(10.151), "10,15");
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
    assertEquals(underTest.parse("10,15"), 10.15);
    assertEquals(underTest.parse("11,16"), 11.16);
  }

  private Runnable createParseRunnable(ThreadSafeNumberFormat decimalFormat, String value, Holder<Number> holder) {
    return () -> {
      try {
        holder.setValue(of(decimalFormat.parse(value)));
      }
      catch (ParseException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public void hinweisTestSetterMethod() {
    assertEquals(stream(underTest.getClass().getMethods()).filter(method -> method.getName().startsWith("set")).count(), 6, " Methode 'testSetter' und 'assertSetterFailed' erweitern");
  }

  public void testSetter() throws Throwable {
    RoundingMode roundingMode = createRandomRoundingMode();
    int minimumIntegerDigits = random.nextInt(5) + 1;
    int maximumIntegerDigits = minimumIntegerDigits + random.nextInt(5);
    int minimumFractionDigits = random.nextInt(5) + 1;
    int maximumFractionDigits = minimumFractionDigits + random.nextInt(5);
    boolean groupingUsed = true;

    underTest.setRoundingMode(roundingMode);
    underTest.setMinimumIntegerDigits(minimumIntegerDigits);
    underTest.setMaximumIntegerDigits(maximumIntegerDigits);
    underTest.setMinimumFractionDigits(minimumFractionDigits);
    underTest.setMaximumFractionDigits(maximumFractionDigits);
    underTest.setGroupingUsed(groupingUsed);

    runInNewThread(() -> {
      ThreadLocal<NumberFormat> numberFormatThreadLocal = getPrivateField(underTest, "numberFormat");
      assertEquals(numberFormatThreadLocal.get().getRoundingMode(), roundingMode, " roundingMode failed");
      assertEquals(numberFormatThreadLocal.get().getMinimumIntegerDigits(), minimumIntegerDigits, " minimumIntegerDigits failed");
      assertEquals(numberFormatThreadLocal.get().getMaximumIntegerDigits(), maximumIntegerDigits, " maximumIntegerDigits failed");
      assertEquals(numberFormatThreadLocal.get().getMinimumFractionDigits(), minimumFractionDigits, " minimumFractionDigits failed");
      assertEquals(numberFormatThreadLocal.get().getMaximumFractionDigits(), maximumFractionDigits, " maximumFractionDigits failed");
      assertEquals(numberFormatThreadLocal.get().isGroupingUsed(), groupingUsed, " groupingUsed failed");
    });
  }

  public void setterCallAfterFormatFailed() throws Throwable {
    underTest.format(createRandomBigDecimal());
    assertSetterFailed();

    runInNewThread(() -> {
      underTest.format(createRandomBigDecimal());
      assertSetterFailed();
    });
  }

  public void setterCallAfterParseFailed() throws Exception {
    underTest.parse(Integer.valueOf(random.nextInt()).toString());
    assertSetterFailed();
  }

  private void assertSetterFailed() {
    assertEnsureExceptionErwartet(underTest -> underTest.setRoundingMode(createRandomRoundingMode()));
    assertEnsureExceptionErwartet(underTest -> underTest.setMinimumIntegerDigits(random.nextInt()));
    assertEnsureExceptionErwartet(underTest -> underTest.setMaximumIntegerDigits(random.nextInt()));
    assertEnsureExceptionErwartet(underTest -> underTest.setMinimumFractionDigits(random.nextInt()));
    assertEnsureExceptionErwartet(underTest -> underTest.setMaximumFractionDigits(random.nextInt()));
    assertEnsureExceptionErwartet(underTest -> underTest.setGroupingUsed(random.nextBoolean()));
  }

  private void assertEnsureExceptionErwartet(Consumer<ThreadSafeNumberFormat> consumer) {
    try {
      consumer.accept(underTest);
      fail("EnsureException erwartet");
    }
    catch (IllegalStateException e) {
      // expected
    }
  }

  private BigDecimal createRandomBigDecimal() {
    return BigDecimal.valueOf(random.nextInt());
  }

  private RoundingMode createRandomRoundingMode() {
    final int randomIndex = random.nextInt(RoundingMode.values().length);
    return RoundingMode.values()[randomIndex];
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