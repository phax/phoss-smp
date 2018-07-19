/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

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
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.dao.DAOException;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.basic.app.dao.AbstractPhotonMapBasedWALDAO;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * Manager for all {@link SMPBusinessCard} objects.
 *
 * @author Philip Helger
 */
public final class XMLBusinessCardManager extends AbstractPhotonMapBasedWALDAO <ISMPBusinessCard, SMPBusinessCard>
                                          implements
                                          ISMPBusinessCardManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (XMLBusinessCardManager.class);

  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  public XMLBusinessCardManager (@Nonnull @Nonempty final String sFilename) throws DAOException
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
    AuditHelper.onAuditCreateSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      aSMPBusinessCard.getServiceGroupID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _updateSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard)
  {
    m_aRWLock.writeLocked ( () -> {
      internalUpdateItem (aSMPBusinessCard);
    });
    AuditHelper.onAuditModifySuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      aSMPBusinessCard.getServiceGroupID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return aSMPBusinessCard;
  }

  /**
   * Create or update a business card for a service group.
   *
   * @param aServiceGroup
   *        Service group
   * @param aEntities
   *        The entities of the business card. May not be <code>null</code>.
   * @return The new or updated {@link ISMPBusinessCard}. Never
   *         <code>null</code>.
   */
  @Nonnull
  public ISMPBusinessCard createOrUpdateSMPBusinessCard (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                         @Nonnull final Collection <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aEntities, "Entities");

    LOGGER.info ("createOrUpdateSMPBusinessCard (" +
                    aServiceGroup.getParticpantIdentifier ().getURIEncoded () +
                    ", " +
                    CollectionHelper.getSize (aEntities) +
                    " entities)");

    final ISMPBusinessCard aOldBusinessCard = getSMPBusinessCardOfServiceGroup (aServiceGroup);
    final SMPBusinessCard aNewBusinessCard = new SMPBusinessCard (aServiceGroup, aEntities);
    if (aOldBusinessCard != null)
    {
      // Reuse old ID
      _updateSMPBusinessCard (aNewBusinessCard);
      LOGGER.info ("createOrUpdateSMPBusinessCard update successful");
    }
    else
    {
      // Create new ID
      _createSMPBusinessCard (aNewBusinessCard);
      LOGGER.info ("createOrUpdateSMPBusinessCard create successful");
    }

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onCreateOrUpdateSMPBusinessCard (aNewBusinessCard));

    return aNewBusinessCard;
  }

  @Nonnull
  public EChange deleteSMPBusinessCard (@Nullable final ISMPBusinessCard aSMPBusinessCard)
  {
    if (aSMPBusinessCard == null)
      return EChange.UNCHANGED;

    LOGGER.info ("deleteSMPBusinessCard (" + aSMPBusinessCard.getID () + ")");

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

    // Invoke generic callbacks
    m_aCBs.forEach (x -> x.onDeleteSMPBusinessCard (aSMPBusinessCard));

    AuditHelper.onAuditDeleteSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      aSMPBusinessCard.getServiceGroupID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    LOGGER.info ("deleteSMPBusinessCard successful");

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

    return getSMPBusinessCardOfID (aServiceGroup.getID ());
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }

  @Nonnegative
  public int getSMPBusinessCardCount ()
  {
    return size ();
  }
}
