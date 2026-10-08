/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.restapi.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.base.exception.InitializationException;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for class {@link SMPRestResponseCacheHandler} and the default implementation
 * {@link SMPRestResponseCacheManualCache}.
 *
 * @author Philip Helger
 */
public final class SMPRestResponseCacheHandlerTest
{
  private static final PeppolIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IParticipantIdentifier PID1 = IF.createParticipantIdentifierWithDefaultScheme ("9915:test1");
  private static final IParticipantIdentifier PID2 = IF.createParticipantIdentifierWithDefaultScheme ("9915:test2");
  private static final IDocumentTypeIdentifier DT1 = IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:test::Test##urn:test:1.0::2.1");
  private static final IDocumentTypeIdentifier DT2 = IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:test::Test2##urn:test:1.0::2.1");

  private SMPRestResponseCacheHandler m_aHandler;

  @Before
  public void before ()
  {
    m_aHandler = new SMPRestResponseCacheHandler (new SMPRestResponseCacheManualCache (Duration.ofMinutes (1), 100));
  }

  @After
  public void after ()
  {
    m_aHandler.shutdown ();
  }

  private static byte [] _bytes (final String s)
  {
    return s.getBytes (StandardCharsets.UTF_8);
  }

  @Test
  public void testKey ()
  {
    final SMPRestResponseCacheKey aKey1 = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1);
    assertEquals (aKey1, SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1));
    assertEquals (aKey1.hashCode (), SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1).hashCode ());
    assertNotEquals (aKey1, SMPRestResponseCacheKey.forServiceMetadata (PID1, DT2));
    assertNotEquals (aKey1, SMPRestResponseCacheKey.forServiceMetadata (PID2, DT1));
    assertNotEquals (SMPRestResponseCacheKey.forServiceGroup (PID1, "http://a/x"),
                     SMPRestResponseCacheKey.forServiceGroup (PID1, "http://b/x"));
    assertEquals (PID1.getURIEncoded (), aKey1.getParticipantID ());
  }

  @Test
  public void testGetOrCreate ()
  {
    final AtomicInteger aCount = new AtomicInteger (0);
    final SMPRestResponseCacheKey aKey = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1);

    final byte [] aFirst = m_aHandler.getOrCreate (aKey, () -> {
      aCount.incrementAndGet ();
      return _bytes ("sm1");
    });
    assertArrayEquals (_bytes ("sm1"), aFirst);
    assertEquals (1, aCount.get ());

    // Served from the cache
    final byte [] aSecond = m_aHandler.getOrCreate (aKey, () -> {
      aCount.incrementAndGet ();
      return _bytes ("other");
    });
    assertSame (aFirst, aSecond);
    assertEquals (1, aCount.get ());
  }

  @Test
  public void testExceptionIsNotCached ()
  {
    final SMPRestResponseCacheKey aKey = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1);
    try
    {
      m_aHandler.getOrCreate (aKey, () -> {
        throw new IllegalStateException ("Not found");
      });
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
    assertNull (m_aHandler.getImplementation ().get (aKey));
  }

  @Test
  public void testInvalidateParticipant ()
  {
    final SMPRestResponseCacheKey aSG1 = SMPRestResponseCacheKey.forServiceGroup (PID1, "http://smp/" + PID1.getURIPercentEncoded ());
    final SMPRestResponseCacheKey aSM11 = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1);
    final SMPRestResponseCacheKey aSM12 = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT2);
    final SMPRestResponseCacheKey aSM21 = SMPRestResponseCacheKey.forServiceMetadata (PID2, DT1);
    m_aHandler.getOrCreate (aSG1, () -> _bytes ("sg1"));
    m_aHandler.getOrCreate (aSM11, () -> _bytes ("sm11"));
    m_aHandler.getOrCreate (aSM12, () -> _bytes ("sm12"));
    m_aHandler.getOrCreate (aSM21, () -> _bytes ("sm21"));

    final ISMPRestResponseCacheSPI aImpl = m_aHandler.getImplementation ();
    m_aHandler.invalidateParticipant (PID1);
    assertNull (aImpl.get (aSG1));
    assertNull (aImpl.get (aSM11));
    assertNull (aImpl.get (aSM12));
    // Other participants are not affected
    assertArrayEquals (_bytes ("sm21"), aImpl.get (aSM21));

    m_aHandler.invalidateAll ();
    assertNull (aImpl.get (aSM21));
  }

  @Test
  public void testInvalidationDuringCreationIsNotCached ()
  {
    final SMPRestResponseCacheKey aKey = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1);

    // Simulates a modification that happens while the response is created from the old data
    final byte [] aOutdated = m_aHandler.getOrCreate (aKey, () -> {
      m_aHandler.invalidateParticipant (PID2);
      return _bytes ("outdated");
    });
    // The response itself is returned, but not cached
    assertArrayEquals (_bytes ("outdated"), aOutdated);
    assertNull (m_aHandler.getImplementation ().get (aKey));

    // The next creation is cached again
    m_aHandler.getOrCreate (aKey, () -> _bytes ("current"));
    assertArrayEquals (_bytes ("current"), m_aHandler.getImplementation ().get (aKey));
  }

  @Test
  public void testBrokenImplementationDoesNotBreak ()
  {
    final SMPRestResponseCacheHandler aHandler = new SMPRestResponseCacheHandler (new ISMPRestResponseCacheSPI ()
    {
      public byte [] get (final SMPRestResponseCacheKey aKey)
      {
        throw new IllegalStateException ("get");
      }

      public void put (final SMPRestResponseCacheKey aKey, final byte [] aPayload)
      {
        throw new IllegalStateException ("put");
      }

      public void removeAllOfParticipant (final String sParticipantID)
      {
        throw new IllegalStateException ("removeAllOfParticipant");
      }

      public void removeAll ()
      {
        throw new IllegalStateException ("removeAll");
      }
    });
    final SMPRestResponseCacheKey aKey = SMPRestResponseCacheKey.forServiceMetadata (PID1, DT1);
    assertArrayEquals (_bytes ("x"), aHandler.getOrCreate (aKey, () -> _bytes ("x")));
    aHandler.invalidateParticipant (PID1);
    aHandler.invalidateAll ();
  }

  @Test
  public void testSelectImplementation ()
  {
    final ISMPRestResponseCacheSPI aDefault = SMPRestResponseCache.selectImplementation (new CommonsArrayList <> ());
    try
    {
      assertTrue (aDefault instanceof SMPRestResponseCacheManualCache);
    }
    finally
    {
      aDefault.shutdown ();
    }

    final ISMPRestResponseCacheSPI aImpl = m_aHandler.getImplementation ();
    assertSame (aImpl, SMPRestResponseCache.selectImplementation (new CommonsArrayList <> (aImpl)));

    try
    {
      SMPRestResponseCache.selectImplementation (new CommonsArrayList <> (aImpl, aImpl));
      fail ();
    }
    catch (final InitializationException ex)
    {
      // expected
    }
  }
}
