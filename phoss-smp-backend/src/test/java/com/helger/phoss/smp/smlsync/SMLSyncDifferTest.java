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
package com.helger.phoss.smp.smlsync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppol.smlclient.participant.ParticipantIdentifierPageType;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;

/**
 * Test class for class {@link SMLSyncDiffer}.
 *
 * @author Philip Helger
 */
public final class SMLSyncDifferTest
{
  private static final String PI1 = "iso6523-actorid-upis::0088:1";
  private static final String PI2 = "iso6523-actorid-upis::0088:2";
  private static final String PI3 = "iso6523-actorid-upis::0088:3";
  private static final String PI4 = "iso6523-actorid-upis::0088:4";

  @Test
  public void testAllMatch ()
  {
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (PI1, PI2, PI3), aOrphans::add);
    aDiffer.addSMLParticipant (PI1);
    aDiffer.addSMLParticipant (PI2);
    aDiffer.addSMLParticipant (PI3);

    assertEquals (3, aDiffer.getSMLParticipantCount ());
    assertEquals (3, aDiffer.getLocalParticipantCount ());
    assertTrue (aDiffer.getMissingInSMLCandidates ().isEmpty ());
    assertTrue (aOrphans.isEmpty ());
  }

  @Test
  public void testMissingInSMLOnly ()
  {
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (PI1, PI2, PI3), aOrphans::add);
    aDiffer.addSMLParticipant (PI1);

    assertEquals (1, aDiffer.getSMLParticipantCount ());
    assertEquals (new CommonsHashSet <> (PI2, PI3), aDiffer.getMissingInSMLCandidates ());
    assertTrue (aOrphans.isEmpty ());
  }

  @Test
  public void testOrphansOnly ()
  {
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (PI1), aOrphans::add);
    aDiffer.addSMLParticipant (PI1);
    aDiffer.addSMLParticipant (PI2);
    aDiffer.addSMLParticipant (PI3);

    assertEquals (3, aDiffer.getSMLParticipantCount ());
    assertTrue (aDiffer.getMissingInSMLCandidates ().isEmpty ());
    assertEquals (new CommonsArrayList <> (PI2, PI3), aOrphans);
  }

  @Test
  public void testBothDirections ()
  {
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (PI1, PI2), aOrphans::add);
    aDiffer.addSMLParticipant (PI2);
    aDiffer.addSMLParticipant (PI3);

    assertEquals (new CommonsHashSet <> (PI1), aDiffer.getMissingInSMLCandidates ());
    assertEquals (new CommonsArrayList <> (PI3), aOrphans);
  }

  @Test
  public void testEmptySML ()
  {
    // This is the signature of an SMP that was unregistered and re-registered at the SML
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (PI1, PI2, PI3), aOrphans::add);

    assertEquals (0, aDiffer.getSMLParticipantCount ());
    assertEquals (3, aDiffer.getMissingInSMLCandidates ().size ());
    assertTrue (aOrphans.isEmpty ());
  }

  @Test
  public void testEmptyLocal ()
  {
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (), aOrphans::add);
    aDiffer.addSMLParticipant (PI1);

    assertEquals (0, aDiffer.getLocalParticipantCount ());
    assertTrue (aDiffer.getMissingInSMLCandidates ().isEmpty ());
    assertEquals (new CommonsArrayList <> (PI1), aOrphans);
  }

  @Test
  public void testProvidedSetIsNotModified ()
  {
    final ICommonsSet <String> aLocal = new CommonsHashSet <> (PI1, PI2);
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (aLocal, x -> {});
    aDiffer.addSMLParticipant (PI1);

    // The caller keeps its set intact
    assertEquals (new CommonsHashSet <> (PI1, PI2), aLocal);
    assertEquals (new CommonsHashSet <> (PI2), aDiffer.getMissingInSMLCandidates ());
  }

  @Test
  public void testAddSMLPage ()
  {
    final ICommonsList <String> aOrphans = new CommonsArrayList <> ();
    final SMLSyncDiffer aDiffer = new SMLSyncDiffer (new CommonsHashSet <> (PI1), aOrphans::add);

    final ParticipantIdentifierPageType aPage = new ParticipantIdentifierPageType ();
    aPage.addParticipantIdentifier (new SimpleParticipantIdentifier ("iso6523-actorid-upis", "0088:1"));
    aPage.addParticipantIdentifier (new SimpleParticipantIdentifier ("iso6523-actorid-upis", "0088:2"));
    aDiffer.addSMLPage (aPage);

    // The URI encoding of the SML result must match what the SMP holds locally
    assertEquals (2, aDiffer.getSMLParticipantCount ());
    assertTrue (aDiffer.getMissingInSMLCandidates ().isEmpty ());
    assertEquals (new CommonsArrayList <> (PI2), aOrphans);
  }

  @Test
  public void testVerifiedMissingDropsConcurrentlyDeleted ()
  {
    // PI2 and PI3 were not in the SML list, but PI3 was deleted locally while the list was read
    final ICommonsSet <String> aCandidates = new CommonsHashSet <> (PI2, PI3);
    final ICommonsSet <String> aLocalNow = new CommonsHashSet <> (PI1, PI2);

    assertEquals (new CommonsHashSet <> (PI2), SMLSyncDiffer.getVerifiedMissingInSML (aCandidates, aLocalNow));
  }

  @Test
  public void testVerifiedOrphansDropsConcurrentlyCreated ()
  {
    // PI3 and PI4 were in the SML but not local; PI3 was created locally while the list was read
    final ICommonsList <String> aCandidates = new CommonsArrayList <> (PI3, PI4);
    final ICommonsSet <String> aLocalNow = new CommonsHashSet <> (PI1, PI3);

    final ICommonsList <String> aVerified = new CommonsArrayList <> ();
    final int nCount = SMLSyncDiffer.getVerifiedOrphansInSML (aCandidates, aLocalNow, aVerified::add);

    assertEquals (1, nCount);
    assertEquals (new CommonsArrayList <> (PI4), aVerified);
  }

  @Test
  public void testVerificationOfAHealthySystemChangesNothing ()
  {
    final ICommonsSet <String> aLocalNow = new CommonsHashSet <> (PI1, PI2);

    assertTrue (SMLSyncDiffer.getVerifiedMissingInSML (new CommonsHashSet <> (), aLocalNow).isEmpty ());
    assertEquals (0,
                  SMLSyncDiffer.getVerifiedOrphansInSML (new CommonsArrayList <> (), aLocalNow, x -> {
                    throw new IllegalStateException ("Must not be called");
                  }));
  }
}
