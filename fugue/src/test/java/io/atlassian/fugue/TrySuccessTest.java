package io.atlassian.fugue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TrySuccessTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  private final Integer STARTING_VALUE = 0;
  private final Try<Integer> t = Checked.now(() -> STARTING_VALUE);
  private final Function<Integer, String> f = Object::toString;
  private final Function<String, Integer> g = Integer::valueOf;
  private final Checked.Function<Integer, String, Exception> fChecked = Object::toString;
  private final Function<Integer, String> fThrows = x -> {
    throw new TestException();
  };
  private final Function<Integer, Try<String>> fTryThrows = x -> {
    throw new TestException();
  };

  @Test public void isFailure() throws Exception {
    assertThat(t.isFailure(), is(false));
  }

  @Test public void isSuccess() throws Exception {
    assertThat(t.isSuccess(), is(true));
  }

  @Test public void map() throws Exception {
    assertThat(t.map(f).map(g), is(t));
  }

  @Test public void mapThrowingFunctionRetunsFailure() throws Exception {

    Try<Integer> result = t.map(fThrows).map(g);

    assertThat(result.isFailure(), is(true));
  }

  @Test public void flatMap() throws Exception {
    Try<String> t2 = t.flatMap(i -> Checked.now(() -> fChecked.apply(i)));

    assertThat(t2, is(Checked.now(() -> "0")));
  }

  @Test public void flatMapThrowingFunctionThrows() {
    thrown.expect(TestException.class);

    t.flatMap(fTryThrows);
  }

  @Test public void peekDoesNotChangeValue() {
    final Try<Integer> t2 = t.peek(value -> {});
    assertThat(t2, is(t));
  }

  @Test public void peekDispatchValueToConsumer() {
    AtomicInteger counter = new AtomicInteger(666);
    t.peek(counter::set);
    assertThat(counter.get(), is(STARTING_VALUE));
  }

  @Test public void recover() throws Exception {
    assertThat(t.recover(e -> 1), is(t));
  }

  @Test public void recoverExceptionType() throws Exception {
    assertThat(t.recover(Exception.class, e -> 1), is(t));
  }

  @Test public void recoverWith() throws Exception {
    assertThat(t.recoverWith(e -> Checked.now(() -> 1)), is(t));
  }

  @Test public void recoverWithExceptionType() throws Exception {
    assertThat(t.recoverWith(Exception.class, e -> Checked.now(() -> 1)), is(t));
  }

  @Test public void getOrElse() throws Exception {
    assertThat(t.getOrElse(() -> 1), is(STARTING_VALUE));
  }

  @Test public void fold() throws Exception {
    Integer i = t.fold(v -> {
      throw new RuntimeException();
    }, identity());

    assertThat(i, is(STARTING_VALUE));
  }

  @Test public void foldPassedThrowingFunctionThrows() throws Exception {
    thrown.expect(TestException.class);

    t.fold(x -> "x", fThrows);
  }

  @Test public void toEither() throws Exception {
    assertThat(t.toEither(), is(Either.right(STARTING_VALUE)));
  }

  @Test public void toOption() throws Exception {
    assertThat(t.toOption(), is(Option.some(STARTING_VALUE)));
  }

  @Test public void liftingFunctionReturnsSuccessIfNoExceptionThrow() {
    Try<Integer> result = Checked.lift(String::length).apply("test");

    assertThat(result.isSuccess(), is(true));

    final int val = result.fold(f -> {
      throw new NoSuchElementException();
    }, identity());
    assertThat(val, is(4));
  }

  private class TestException extends RuntimeException {}

}