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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Function;

public class FunctionMapNullToOptionTest {

  @Test public void testLiftingNull() {
    assertThat(Functions.mapNullToOption(FunctionMapNullToOptionTest.<String, String> nullProducer()).apply("ignored"), is(Option.<String> none()));
  }

  @Test public void testLiftingNotNull() {
    assertThat(Functions.mapNullToOption(com.google.common.base.Functions.<String> identity()).apply("mx1tr1x"), is(Option.some("mx1tr1x")));
  }

  static <A, B> Function<A, B> nullProducer() {
    return new Function<A, B>() {
      @Override public B apply(A a) {
        return null;
      }
    };
  }
}