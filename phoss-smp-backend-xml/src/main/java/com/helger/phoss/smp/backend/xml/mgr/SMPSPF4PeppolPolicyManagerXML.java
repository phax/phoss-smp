/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.xml.mgr;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.ELockType;
import com.helger.annotation.concurrent.IsLocked;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.dao.DAOException;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicy;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicyCallback;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicyManager;
import com.helger.phoss.smp.domain.spf.SMPSPF4PeppolPolicy;
import com.helger.phoss.smp.domain.spf.SPF4PeppolTerm;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * Manager for all {@link SMPSPF4PeppolPolicy} objects using XML file storage.
 *
 * @author Steven Noels
 */
public final class SMPSPF4PeppolPolicyManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPSPF4PeppolPolicy, SMPSPF4PeppolPolicy>
                                                  implements ISMPSPF4PeppolPolicyManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPSPF4PeppolPolicyManagerXML.class);

  private final CallbackList <ISMPSPF4PeppolPolicyCallback> m_aCBs = new CallbackList <> ();

  public SMPSPF4PeppolPolicyManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPSPF4PeppolPolicy.class, sFilename);
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList<ISMPSPF4PeppolPolicyCallback> spfCallbacks ()
  {
    return m_aCBs;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPSPF4PeppolPolicy _createSPFPolicy (@NonNull final SMPSPF4PeppolPolicy aPolicy)
  {
    m_aRWLock.writeLocked ( () -> { internalCreateItem (aPolicy); });
    AuditHelper.onAuditCreateSuccess (SMPSPF4PeppolPolicy.OT,
                                      aPolicy.getID (),
                                      Integer.valueOf (aPolicy.getTermCount ()));
    return aPolicy;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPSPF4PeppolPolicy _updateSPFPolicy (@NonNull final SMPSPF4PeppolPolicy aPolicy)
  {
    m_aRWLock.writeLocked ( () -> { internalUpdateItem (aPolicy); });
    AuditHelper.onAuditModifySuccess (SMPSPF4PeppolPolicy.OT,
                                      "set-all",
                                      aPolicy.getID (),
                                      Integer.valueOf (aPolicy.getTermCount ()));
    return aPolicy;
  }

  @Nullable
  public ISMPSPF4PeppolPolicy createOrUpdateSPFPolicy (@NonNull final IParticipantIdentifier aParticipantID,
                                                       @NonNull final Collection <SPF4PeppolTerm> aTerms,
                                                       @Nullable final Integer aTTL,
                                                       @Nullable final String sExplanation)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSPFPolicy (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    aTerms.size () +
                    " terms)");

    final ISMPSPF4PeppolPolicy aOldPolicy = getSPFPolicyOfID (aParticipantID);
    final SMPSPF4PeppolPolicy aNewPolicy = new SMPSPF4PeppolPolicy (aParticipantID, aTerms, aTTL, sExplanation);

    if (aOldPolicy != null)
    {
      // Update existing
      _updateSPFPolicy (aNewPolicy);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSPFPolicy update successful");
    }
    else
    {
      // Create new
      _createSPFPolicy (aNewPolicy);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSPFPolicy create successful");
    }

    // Invoke callbacks
    m_aCBs.forEach (x -> x.onSMPSPFPolicyCreatedOrUpdated (aNewPolicy));

    return aNewPolicy;
  }

  @NonNull
  public EChange deleteSPFPolicy (@Nullable final ISMPSPF4PeppolPolicy aPolicy)
  {
    if (aPolicy == null)
      return EChange.UNCHANGED;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSPFPolicy (" + aPolicy.getID () + ")");

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPSPF4PeppolPolicy aRealPolicy = internalDeleteItem (aPolicy.getID ());
      if (aRealPolicy == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPSPF4PeppolPolicy.OT, aPolicy.getID (), "no-such-id");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditDeleteSuccess (SMPSPF4PeppolPolicy.OT,
                                      aPolicy.getID (),
                                      Integer.valueOf (aPolicy.getTermCount ()));

    // Invoke callbacks
    m_aCBs.forEach (x -> x.onSMPSPFPolicyDeleted (aPolicy));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSPFPolicy successful");

    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPSPF4PeppolPolicy> getAllSPFPolicies ()
  {
    return getAll ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllSPFPolicyIDs ()
  {
    return getAllIDs ();
  }

  public boolean containsSPFPolicyOfID (@Nullable final IParticipantIdentifier aID)
  {
    return aID != null && containsWithID (aID.getURIEncoded ());
  }

  @Nullable
  public ISMPSPF4PeppolPolicy getSPFPolicyOfID (@Nullable final IParticipantIdentifier aID)
  {
    if (aID == null)
      return null;

    return getOfID (aID.getURIEncoded ());
  }

  @Nonnegative
  public long getSPFPolicyCount ()
  {
    return size ();
  }
}
