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
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.dao.DAOException;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

/**
 * Manager for all {@link SMPBusinessCard} objects.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPBusinessCard, SMPBusinessCard>
                                             implements
                                             ISMPBusinessCardManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBusinessCardManagerXML.class);

  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  public SMPBusinessCardManagerXML (@NonNull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPBusinessCard.class, sFilename);
  }

  @NonNull
  @ReturnsMutableObject
  public CallbackList <ISMPBusinessCardCallback> bcCallbacks ()
  {
    return m_aCBs;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _createSMPBusinessCard (@NonNull final SMPBusinessCard aSMPBusinessCard)
  {
    m_aRWLock.writeLocked ( () -> { internalCreateItem (aSMPBusinessCard); });
    AuditHelper.onAuditCreateSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  @NonNull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _updateSMPBusinessCard (@NonNull final SMPBusinessCard aSMPBusinessCard)
  {
    m_aRWLock.writeLocked ( () -> { internalUpdateItem (aSMPBusinessCard); });
    AuditHelper.onAuditModifySuccess (SMPBusinessCard.OT,
                                      "set-all",
                                      aSMPBusinessCard.getID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  @NonNull
  public ISMPBusinessCard createOrUpdateSMPBusinessCard (@NonNull final IParticipantIdentifier aParticipantID,
                                                         @NonNull final Collection <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aEntities, "Entities");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPBusinessCard (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    aEntities.size () +
                    " entities)");

    final ISMPBusinessCard aOldBusinessCard = getSMPBusinessCardOfID (aParticipantID);
    final SMPBusinessCard aNewBusinessCard = new SMPBusinessCard (aParticipantID, aEntities);
    if (aOldBusinessCard != null)
    {
      // Reuse old ID
      _updateSMPBusinessCard (aNewBusinessCard);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPBusinessCard update successful");
    }
    else
    {
      // Create new ID
      _createSMPBusinessCard (aNewBusinessCard);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("createOrUpdateSMPBusinessCard create successful");
    }

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onSMPBusinessCardCreatedOrUpdated (aNewBusinessCard));

    return aNewBusinessCard;
  }

  @NonNull
  public EChange deleteSMPBusinessCard (@Nullable final ISMPBusinessCard aSMPBusinessCard)
  {
    if (aSMPBusinessCard == null)
      return EChange.UNCHANGED;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard (" + aSMPBusinessCard.getID () + ")");

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPBusinessCard aRealBusinessCard = internalDeleteItem (aSMPBusinessCard.getID ());
      if (aRealBusinessCard == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPBusinessCard.OT, aSMPBusinessCard.getID (), "no-such-id");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditDeleteSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onSMPBusinessCardDeleted (aSMPBusinessCard));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard successful");

    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    return getAll ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllSMPBusinessCardIDs ()
  {
    return getAllIDs ();
  }

  public boolean containsSMPBusinessCardOfID (@Nullable final IParticipantIdentifier aID)
  {
    return aID != null && containsWithID (aID.getURIEncoded ());
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfID (@Nullable final IParticipantIdentifier aID)
  {
    if (aID == null)
      return null;

    return getOfID (aID.getURIEncoded ());
  }

  @Nonnegative
  public long getSMPBusinessCardCount ()
  {
    return size ();
  }
}
