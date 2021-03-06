/*
   Copyright 2010 Atlassian

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
package io.atlassian.fugue.retry;

/**
 * A backoff task for use in a retry -function, -supplier, or -task. This should
 * be used as the beforeRetry hook. Upon each execution, the current thread
 * sleeps for the time specified at construction.
 *
 * This class is thread safe and contains no internal state, hence instances can
 * be reused and the same instance can be used on multiple threads.
 *
 */
public class BeforeRetryLinearBackoffTask implements Runnable {
  private final long backoff;

  /**
   * <p>
   * Constructor for BeforeRetryLinearBackoffTask.
   * </p>
   *
   * @param backoffMillis the time to wait whenever run is executed
   */
  public BeforeRetryLinearBackoffTask(long backoffMillis) {
    if (backoffMillis <= 0) {
      throw new IllegalArgumentException("Backoff time must not be negative.");
    }
    this.backoff = backoffMillis;
  }

  /**
   * This method causes the current thread to sleep for the time specified when
   * the instance is constructed. InterruptedExceptions are wrapped before being
   * rethrown in a RuntimeException.
   */
  public void run() {
    try {
      Thread.sleep(backoff);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  long currentBackoff() {
    return backoff;
  }
}
