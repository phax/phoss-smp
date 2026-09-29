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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ETriState;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.SMLExceptionHelper;
import com.helger.peppol.smlclient.participant.ParticipantIdentifierPageType;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.smlhook.SmpSmlHelper;
import com.helger.xsds.peppol.id1.ParticipantIdentifierType;

/**
 * The first page of the SML <code>List()</code> operation of chapter 3.1.2.7 of the SML
 * specification.<br>
 * Reading only the first page is a cheap, constant time operation, in contrast to the full paged
 * read of {@link SMLSyncJob}. It answers two questions without any scaling concern:
 * <ul>
 * <li>does the SML have <em>any</em> participant registered for this SMP - relevant before
 * unregistering the SMP, because that deletes all of them</li>
 * <li>if the SML returned no next page identifier, the first page <em>is</em> the complete list, so
 * the registration of an individual participant can be answered definitively. For the majority of
 * SMPs, which hold far fewer participants than a page holds, that is always the case.</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 8.6.0
 */
@Immutable
public final class SMLFirstPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMLFirstPage.class);

  private final ICommonsSet <String> m_aParticipantIDs;
  private final boolean m_bHasMorePages;
  private final String m_sErrorMessage;

  private SMLFirstPage (@NonNull final ICommonsSet <String> aParticipantIDs,
                        final boolean bHasMorePages,
                        @Nullable final String sErrorMessage)
  {
    m_aParticipantIDs = aParticipantIDs;
    m_bHasMorePages = bHasMorePages;
    m_sErrorMessage = sErrorMessage;
  }

  /**
   * Read the first page of the participants the SML has registered for the provided SMP. This never
   * throws - a failure is reported via {@link #hasError()}, because every caller of this is a
   * safety check that must not break if the SML is unavailable.
   *
   * @param aSMLInfo
   *        The SML to be queried. May not be <code>null</code>.
   * @param sSMPID
   *        The ID of the SMP whose participants are to be read. May neither be <code>null</code>
   *        nor empty.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static SMLFirstPage read (@NonNull final ISMLInfo aSMLInfo, @NonNull @Nonempty final String sSMPID)
  {
    ValueEnforcer.notNull (aSMLInfo, "SMLInfo");
    ValueEnforcer.notEmpty (sSMPID, "SMPID");

    try
    {
      final ManageParticipantIdentifierServiceCaller aCaller = SmpSmlHelper.createSMLCallerPI (aSMLInfo);
      // An empty page ID requests the first page
      final ParticipantIdentifierPageType aPage = aCaller.list ("", sSMPID);

      final ICommonsSet <String> aIDs = new CommonsHashSet <> ();
      for (final ParticipantIdentifierType aPI : aPage.getParticipantIdentifier ())
        aIDs.add (CIdentifier.getURIEncoded (aPI));

      final boolean bHasMorePages = StringHelper.isNotEmpty (aPage.getNextPageIdentifier ());
      LOGGER.info ("The SML has " +
                   aIDs.size () +
                   (bHasMorePages ? " or more" : "") +
                   " participant(s) registered for SMP '" +
                   sSMPID +
                   "'");
      return new SMLFirstPage (aIDs, bHasMorePages, null);
    }
    catch (final Exception ex)
    {
      // Prefer the SML fault message, because it is way more specific
      String sErrorMsg = SMLExceptionHelper.getFaultMessage (ex);
      if (StringHelper.isEmpty (sErrorMsg))
        sErrorMsg = ex.getClass ().getName () + " - " + ex.getMessage ();

      LOGGER.error ("Failed to read the participants of SMP '" +
                    sSMPID +
                    "' from the SML '" +
                    aSMLInfo.getManagementServiceURL () +
                    "': " +
                    sErrorMsg);
      return new SMLFirstPage (new CommonsHashSet <> (), false, sErrorMsg);
    }
  }

  /**
   * @return <code>true</code> if the SML could not be queried at all. In that case the caller must
   *         not draw any conclusion from the other methods.
   */
  public boolean hasError ()
  {
    return m_sErrorMessage != null;
  }

  /**
   * @return The technical reason why the SML could not be queried, or <code>null</code> if it
   *         could.
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  /**
   * @return The number of participants on the first page. Always &ge; 0.
   */
  @Nonnegative
  public int getParticipantCount ()
  {
    return m_aParticipantIDs.size ();
  }

  /**
   * @return <code>true</code> if the SML referenced a next page, so that more participants exist
   *         than the ones returned here.
   */
  public boolean hasMorePages ()
  {
    return m_bHasMorePages;
  }

  /**
   * @return <code>true</code> if the participants returned here are the complete list the SML has
   *         registered for this SMP.
   */
  public boolean isCompleteList ()
  {
    return !hasError () && !m_bHasMorePages;
  }

  /**
   * @return The URI encoded participant identifiers of the first page. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllParticipantIDs ()
  {
    return m_aParticipantIDs.getClone ();
  }

  /**
   * Check whether the SML has the provided participant registered for this SMP.
   *
   * @param aParticipantID
   *        The participant to be checked. May not be <code>null</code>.
   * @return {@link ETriState#TRUE} if the SML has it registered for this SMP,
   *         {@link ETriState#FALSE} if it definitively has not, and {@link ETriState#UNDEFINED} if
   *         that cannot be decided - because the SML could not be queried, or because the
   *         participant is not on the first page while further pages exist. A caller must not treat
   *         {@link ETriState#UNDEFINED} as "not registered".
   */
  @NonNull
  public ETriState isParticipantRegistered (@NonNull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    if (hasError ())
      return ETriState.UNDEFINED;
    if (m_aParticipantIDs.contains (aParticipantID.getURIEncoded ()))
      return ETriState.TRUE;
    // Not on this page - only a complete list allows the conclusion that it is absent
    return isCompleteList () ? ETriState.FALSE : ETriState.UNDEFINED;
  }
}
