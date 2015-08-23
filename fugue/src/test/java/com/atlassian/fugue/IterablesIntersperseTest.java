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

import static com.atlassian.fugue.Iterables.intersperse;
import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Supplier;

public class IterablesIntersperseTest {

  @Test public void interspersed() {
    assertThat(intersperse(of("a", "b", "c"), "-"), contains("a", "-", "b", "-", "c"));
  }

  @Test public void interspersedSupplier() {
    Supplier<Integer> count = new Supplier<Integer>() {
      int count = 0;

      @Override public Integer get() {
        try {
          return count;
        } finally {
          count += 1;
        }
      }
    };
    assertThat(intersperse(of(100, 200, 300), count), contains(100, 0, 200, 1, 300));
  }
}
