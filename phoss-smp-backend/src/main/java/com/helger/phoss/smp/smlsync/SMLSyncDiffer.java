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

import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppol.smlclient.participant.ParticipantIdentifierPageType;
import com.helger.peppolid.CIdentifier;
import com.helger.xsds.peppol.id1.ParticipantIdentifierType;

/**
 * Compares the participants this SMP holds locally with the participants the SML holds for this
 * SMP, as returned by the <code>List()</code> operation of chapter 3.1.2.7 of the SML
 * specification.<br>
 * The comparison is performed on URI encoded participant identifiers, because
 * <code>ISMPServiceGroupManager.getAllSMPServiceGroupIDs ()</code> and the SML return the very same
 * representation, for example <code>iso6523-actorid-upis::0225:api-global-france-eb2b</code>.<br>
 * The class is a streaming accumulator, so that a large SML participant list never has to be held
 * in memory at once: the local identifiers are held in a set from which every matched entry is
 * removed, and the identifiers that are not held locally are handed to a consumer one by one. What
 * remains in the set afterwards are the participants that are missing at the SML.
 *
 * @author Philip Helger
 * @since 8.6.0
 */
@NotThreadSafe
public final class SMLSyncDiffer
{
  private final ICommonsSet <String> m_aRemainingLocalIDs;
  private final Consumer <String> m_aOrphanConsumer;
  private int m_nSMLCount = 0;
  private final int m_nLocalCount;

  /**
   * @param aLocalIDs
   *        All URI encoded participant identifiers this SMP holds locally. May not be
   *        <code>null</code>. The provided collection is not modified - a copy is taken.
   * @param aOrphanConsumer
   *        Invoked for every participant identifier the SML holds that is not held locally. May
   *        not be <code>null</code>. Note that these are only <em>candidates</em> - see
   *        {@link #getVerifiedOrphansInSML(Iterable, Set, Consumer)}.
   */
  public SMLSyncDiffer (@NonNull final Set <String> aLocalIDs, @NonNull final Consumer <String> aOrphanConsumer)
  {
    ValueEnforcer.notNull (aLocalIDs, "LocalIDs");
    ValueEnforcer.notNull (aOrphanConsumer, "OrphanConsumer");

    m_aRemainingLocalIDs = new CommonsHashSet <> (aLocalIDs);
    m_nLocalCount = m_aRemainingLocalIDs.size ();
    m_aOrphanConsumer = aOrphanConsumer;
  }

  /**
   * Add a single participant identifier the SML holds for this SMP.
   *
   * @param sParticipantID
   *        The URI encoded participant identifier. May neither be <code>null</code> nor empty.
   */
  public void addSMLParticipant (@NonNull @Nonempty final String sParticipantID)
  {
    ValueEnforcer.notEmpty (sParticipantID, "ParticipantID");

    m_nSMLCount++;
    if (!m_aRemainingLocalIDs.remove (sParticipantID))
    {
      // The SML holds a participant this SMP does not know
      m_aOrphanConsumer.accept (sParticipantID);
    }
  }

  /**
   * Add all participant identifiers of a single page of the SML <code>List()</code> operation.
   *
   * @param aPage
   *        The page as returned by the SML. May not be <code>null</code>.
   */
  public void addSMLPage (@NonNull final ParticipantIdentifierPageType aPage)
  {
    ValueEnforcer.notNull (aPage, "Page");

    for (final ParticipantIdentifierType aPI : aPage.getParticipantIdentifier ())
      addSMLParticipant (CIdentifier.getURIEncoded (aPI));
  }

  /**
   * @return The number of participant identifiers that were added from the SML so far. Always
   *         &ge; 0.
   */
  @Nonnegative
  public int getSMLParticipantCount ()
  {
    return m_nSMLCount;
  }

  /**
   * @return The number of participant identifiers this SMP holds locally. Always &ge; 0.
   */
  @Nonnegative
  public int getLocalParticipantCount ()
  {
    return m_nLocalCount;
  }

  /**
   * @return All local participant identifiers that were not contained in the SML list. These are
   *         only <em>candidates</em>, because a Service Group that was created while the SML list
   *         was being read shows up here without being a real difference - see
   *         {@link #getVerifiedMissingInSML(Set, Set)}. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getMissingInSMLCandidates ()
  {
    return m_aRemainingLocalIDs.getClone ();
  }

  /**
   * Remove the false positives from the "missing at the SML" candidates.<br>
   * Chapter 3.1.2.7 of the SML specification states that the underlying data may change between
   * two page reads, and a live SMP additionally creates and deletes Service Groups while the list
   * is being read. A Service Group that was created during the run is therefore not contained in
   * the SML list although nothing is wrong with it. Re-reading the local identifiers after the run
   * and keeping only the candidates that are still held locally removes exactly those cases.<br>
   * Note that the SML offers no way to read a single participant - <code>List()</code> is the only
   * read operation of the <code>ManageBusinessIdentifier</code> interface - so the verification can
   * only be performed against the local state.
   *
   * @param aCandidates
   *        The result of {@link #getMissingInSMLCandidates()}. May not be <code>null</code>.
   * @param aLocalIDsNow
   *        All URI encoded participant identifiers this SMP holds locally, read <em>after</em> the
   *        SML list was read. May not be <code>null</code>.
   * @return The candidates that are still held locally. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsSet <String> getVerifiedMissingInSML (@NonNull final Set <String> aCandidates,
                                                              @NonNull final Set <String> aLocalIDsNow)
  {
    ValueEnforcer.notNull (aCandidates, "Candidates");
    ValueEnforcer.notNull (aLocalIDsNow, "LocalIDsNow");

    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    for (final String sID : aCandidates)
      if (aLocalIDsNow.contains (sID))
        ret.add (sID);
    return ret;
  }

  /**
   * Remove the false positives from the "registered at the SML but unknown here" candidates.<br>
   * A Service Group that was deleted while the SML list was being read shows up as an orphan
   * although nothing is wrong with it. Only the candidates that are still not held locally are
   * real orphans.
   *
   * @param aCandidates
   *        The orphan candidates collected during the run, in the order in which they were found.
   *        May not be <code>null</code>.
   * @param aLocalIDsNow
   *        All URI encoded participant identifiers this SMP holds locally, read <em>after</em> the
   *        SML list was read. May not be <code>null</code>.
   * @param aVerifiedConsumer
   *        Invoked for every candidate that is a real orphan, in the order of the candidates. May
   *        not be <code>null</code>.
   * @return The number of real orphans. Always &ge; 0.
   */
  @Nonnegative
  public static int getVerifiedOrphansInSML (@NonNull final Iterable <String> aCandidates,
                                             @NonNull final Set <String> aLocalIDsNow,
                                             @NonNull final Consumer <String> aVerifiedConsumer)
  {
    ValueEnforcer.notNull (aCandidates, "Candidates");
    ValueEnforcer.notNull (aLocalIDsNow, "LocalIDsNow");
    ValueEnforcer.notNull (aVerifiedConsumer, "VerifiedConsumer");

    int ret = 0;
    for (final String sID : aCandidates)
      if (!aLocalIDsNow.contains (sID))
      {
        aVerifiedConsumer.accept (sID);
        ret++;
      }
    return ret;
  }
}
