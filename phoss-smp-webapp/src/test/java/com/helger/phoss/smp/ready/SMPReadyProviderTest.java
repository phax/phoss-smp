/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.ready;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.helger.base.timing.StopWatch;

/**
 * Test class for class {@link SMPReadyProvider}.
 *
 * @author vinit-thummar
 */
public final class SMPReadyProviderTest
{
  @Test
  public void testReadinessAggregation ()
  {
    // A backend that provides no readiness check at all is NOT ready
    assertFalse (SMPReadyProvider.areAllReady (List.of ()));

    assertTrue (SMPReadyProvider.areAllReady (List.of ( () -> true)));
    assertTrue (SMPReadyProvider.areAllReady (List.of ( () -> true, () -> true)));
    assertFalse (SMPReadyProvider.areAllReady (List.of ( () -> false)));
    assertFalse (SMPReadyProvider.areAllReady (List.of ( () -> true, () -> false)));
    assertFalse (SMPReadyProvider.areAllReady (List.of ( () -> false, () -> true)));
  }

  @Test
  public void testExceptionMeansNotReady ()
  {
    assertFalse (SMPReadyProvider.areAllReady (List.of ( () -> {
      throw new IllegalStateException ("Expected test exception");
    })));
    // The exception of the second check must not be swallowed either
    assertFalse (SMPReadyProvider.areAllReady (List.of ( () -> true, () -> {
      throw new IllegalStateException ("Expected test exception");
    })));
  }

  @Test
  public void testNoTimeout ()
  {
    // A non-positive or missing timeout means "no time limit"
    assertTrue (SMPReadyProvider.areAllReady (List.of ( () -> true), (Duration) null));
    assertTrue (SMPReadyProvider.areAllReady (List.of ( () -> true), Duration.ZERO));
    assertTrue (SMPReadyProvider.areAllReady (List.of ( () -> true), Duration.ofSeconds (-1)));
    assertFalse (SMPReadyProvider.areAllReady (List.of ( () -> false), Duration.ofSeconds (10)));
  }

  @Test
  public void testTimeoutIsRespected () throws InterruptedException
  {
    // Ensure the blocking check is released, even if the assertions fail
    final CountDownLatch aLatch = new CountDownLatch (1);
    try
    {
      final StopWatch aSW = StopWatch.createdStarted ();
      final boolean bReady = SMPReadyProvider.areAllReady (List.of ( () -> {
        try
        {
          // Way longer than the timeout below
          aLatch.await (30, TimeUnit.SECONDS);
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
        }
        return true;
      }), Duration.ofMillis (200));
      final long nMillis = aSW.stopAndGetMillis ();

      // A check that does not finish in time means "not ready"
      assertFalse (bReady);
      // And it must not have blocked the calling thread for the full duration
      assertTrue ("The readiness check took " + nMillis + " milliseconds", nMillis < 10_000);
    }
    finally
    {
      aLatch.countDown ();
    }
  }
}
