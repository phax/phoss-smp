/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.dao.DAOException;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.audit.AuditHelper;

/**
 * Manager for all {@link SMPBusinessCard} objects.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardManagerXML extends AbstractPhotonMapBasedWALDAO <ISMPBusinessCard, SMPBusinessCard> implements
                                             ISMPBusinessCardManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBusinessCardManagerXML.class);

  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  public SMPBusinessCardManagerXML (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPBusinessCard.class, sFilename);
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPBusinessCardCallback> bcCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _createSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard)
  {
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aSMPBusinessCard);
    });
    AuditHelper.onAuditCreateSuccess (SMPBusinessCard.OT, aSMPBusinessCard.getID (), Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _updateSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard)
  {
    m_aRWLock.writeLocked ( () -> {
      internalUpdateItem (aSMPBusinessCard);
    });
    AuditHelper.onAuditModifySuccess (SMPBusinessCard.OT, aSMPBusinessCard.getID (), Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  @Nonnull
  public ISMPBusinessCard createOrUpdateSMPBusinessCard (@Nonnull final IParticipantIdentifier aParticipantID,
                                                         @Nonnull final Collection <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aEntities, "Entities");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createOrUpdateSMPBusinessCard (" + aParticipantID.getURIEncoded () + ", " + aEntities.size () + " entities)");

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

  @Nonnull
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
        AuditHelper.onAuditDeleteFailure (SMPBusinessCard.OT, "no-such-id", aSMPBusinessCard.getID ());
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditDeleteSuccess (SMPBusinessCard.OT, aSMPBusinessCard.getID (), Integer.valueOf (aSMPBusinessCard.getEntityCount ()));

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onSMPBusinessCardDeleted (aSMPBusinessCard));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPBusinessCard successful");

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    return getAll ();
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {

    if (aServiceGroup == null)
      return null;

    return getSMPBusinessCardOfID (aServiceGroup.getParticpantIdentifier ());
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
