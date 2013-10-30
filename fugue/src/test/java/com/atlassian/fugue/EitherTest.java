/*
   Copyright 2011 Atlassian

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.atlassian.fugue;

import static com.atlassian.fugue.Either.left;
import static com.atlassian.fugue.Either.right;
import static com.atlassian.fugue.Eithers.cond;
import static com.atlassian.fugue.Eithers.merge;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Function;

public class EitherTest {
  @Test(expected = NullPointerException.class) public void testNullLeft() {
    Either.left(null);
  }

  @Test public void testLeftCreation() {
    final Either<Boolean, Integer> left = left(true);
    assertThat(left.isLeft(), is(true));
    assertThat(left.left().get(), is(true));
  }

  @Test(expected = NullPointerException.class) public void testNullRight() {
    right(null);
  }

  @Test public void testRightCreation() {
    final Either<Boolean, Integer> right = right(1);
    assertThat(right.isRight(), is(true));
    assertThat(right.right().get(), is(1));
  }

  @Test public void testLeftMerge() {
    assertThat(merge(Either.<String, String> left("Ponies.")), is("Ponies."));
  }

  @Test public void testRightMerge() {
    assertThat(merge(Either.<String, String> right("Unicorns.")), is("Unicorns."));
  }

  @Test public void testCondTrue() {
    assertThat(cond(true, 7, "Pegasus."), is(Either.<Integer, String> right("Pegasus.")));
  }

  @Test public void testCondFalse() {
    assertThat(cond(false, 7, "Pegasus."), is(Either.<Integer, String> left(7)));
  }

  static class GenericTest<A> {
    private final Either<Exception, A> either;

    public GenericTest(Either<Exception, A> either) {
      this.either = either;
    }

    public Exception getException() {
      return either.left().get();
    }

    public A getValue() {
      return either.right().get();
    }

    public <B> GenericTest<B> map(Function<A, B> func) {
      return new GenericTest<B>(either.right().map(func));
    }

    public <B> GenericTest<B> flatMap(Function<A, GenericTest<B>> func) {
      @SuppressWarnings("unchecked")
      GenericTest<B> left = (GenericTest<B>) this;
      return either.fold(Functions.<Exception, GenericTest<B>> constant(left), func);
    }
  }
}