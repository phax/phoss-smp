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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import com.helger.datetime.helper.PDTFactory;
import com.helger.phoss.smp.restapi.SMPRestLastModification;

/**
 * Test class for class {@link SMPRestHttpCacheHelper}.
 *
 * @author Philip Helger
 */
public final class SMPRestHttpCacheHelperTest
{
  @Test
  public void testIsIfNoneMatch ()
  {
    assertFalse (SMPRestHttpCacheHelper.isIfNoneMatch (null, "\"abc\""));
    assertFalse (SMPRestHttpCacheHelper.isIfNoneMatch ("", "\"abc\""));
    assertTrue (SMPRestHttpCacheHelper.isIfNoneMatch ("*", "\"abc\""));
    assertTrue (SMPRestHttpCacheHelper.isIfNoneMatch ("\"abc\"", "\"abc\""));
    assertTrue (SMPRestHttpCacheHelper.isIfNoneMatch ("W/\"abc\"", "\"abc\""));
    assertTrue (SMPRestHttpCacheHelper.isIfNoneMatch ("\"def\", \"abc\"", "\"abc\""));
    assertFalse (SMPRestHttpCacheHelper.isIfNoneMatch ("\"def\"", "\"abc\""));
  }

  @Test
  public void testIsNotModifiedSince ()
  {
    // The example of OASIS SMP 2.0, section 5.2.2
    final LocalDateTime aLastModUTC = LocalDateTime.of (2018, 12, 1, 19, 14, 44);
    // Last modification in the default time zone, as used by the SMP
    final LocalDateTime aLastMod = PDTFactory.createZonedDateTime (aLastModUTC.atOffset (ZoneOffset.UTC))
                                             .toLocalDateTime ();

    assertTrue (SMPRestHttpCacheHelper.isNotModifiedSince ("Sat, 01 Dec 2018 19:14:44 GMT", aLastMod));
    assertTrue (SMPRestHttpCacheHelper.isNotModifiedSince ("Sat, 01 Dec 2018 19:14:45 GMT", aLastMod));
    assertTrue (SMPRestHttpCacheHelper.isNotModifiedSince ("Sun, 01 Dec 2019 00:00:00 GMT", aLastMod));
    assertFalse (SMPRestHttpCacheHelper.isNotModifiedSince ("Sat, 01 Dec 2018 19:14:43 GMT", aLastMod));
    assertFalse (SMPRestHttpCacheHelper.isNotModifiedSince ("Sat, 01 Dec 2018 19:14:44 GMT",
                                                            aLastMod.plusSeconds (1)));

    // Invalid values are ignored
    assertFalse (SMPRestHttpCacheHelper.isNotModifiedSince (null, aLastMod));
    assertFalse (SMPRestHttpCacheHelper.isNotModifiedSince ("", aLastMod));
    assertFalse (SMPRestHttpCacheHelper.isNotModifiedSince ("gibberish", aLastMod));
  }

  @Test
  public void testGetLastModifiedHeaderValue ()
  {
    final LocalDateTime aGlobal = SMPRestLastModification.getGlobalLastModificationDateTime ();
    final LocalDateTime aNow = aGlobal.plusHours (1);

    // Unknown participant last modification - use the global one
    assertEquals (aGlobal.truncatedTo (ChronoUnit.SECONDS),
                  SMPRestHttpCacheHelper.getLastModifiedHeaderValue (null, aNow));
    // Older participant last modification - use the global one
    assertEquals (aGlobal.truncatedTo (ChronoUnit.SECONDS),
                  SMPRestHttpCacheHelper.getLastModifiedHeaderValue (aGlobal.minusDays (1), aNow));

    // Newer participant last modification
    final LocalDateTime aParticipant = aGlobal.plusMinutes (5).withNano (123_000_000);
    assertEquals (aParticipant.truncatedTo (ChronoUnit.SECONDS),
                  SMPRestHttpCacheHelper.getLastModifiedHeaderValue (aParticipant, aNow));

    // Modified in the current second
    assertNull (SMPRestHttpCacheHelper.getLastModifiedHeaderValue (aParticipant, aParticipant.plusNanos (1000)));
    // Modified in the previous second
    assertEquals (aParticipant.truncatedTo (ChronoUnit.SECONDS),
                  SMPRestHttpCacheHelper.getLastModifiedHeaderValue (aParticipant,
                                                                     aParticipant.truncatedTo (ChronoUnit.SECONDS)
                                                                                 .plusSeconds (1)));
  }
}
