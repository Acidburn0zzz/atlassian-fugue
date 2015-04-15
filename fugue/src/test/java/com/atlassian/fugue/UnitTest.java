package com.atlassian.fugue;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class UnitTest {
  @Test public void unitValueIsNotNull() {
    assertThat(Unit.VALUE, is(not(nullValue())));
  }
}