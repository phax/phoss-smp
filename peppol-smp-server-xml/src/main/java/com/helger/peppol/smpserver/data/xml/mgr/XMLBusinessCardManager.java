/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.state.EChange;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * Manager for all {@link SMPBusinessCard} objects.
 *
 * @author Philip Helger
 */
public final class XMLBusinessCardManager extends AbstractWALDAO <SMPBusinessCard> implements ISMPBusinessCardManager
{
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "businesscard";

  private final Map <String, SMPBusinessCard> m_aMap = new HashMap <> ();

  public XMLBusinessCardManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPBusinessCard.class, sFilename);
    initialRead ();
  }

  @Override
  protected void onRecoveryCreate (@Nonnull final SMPBusinessCard aElement)
  {
    _addSMPBusinessCard (aElement, EDAOActionType.CREATE);
  }

  @Override
  protected void onRecoveryUpdate (@Nonnull final SMPBusinessCard aElement)
  {
    _addSMPBusinessCard (aElement, EDAOActionType.UPDATE);
  }

  @Override
  protected void onRecoveryDelete (@Nonnull final SMPBusinessCard aElement)
  {
    m_aMap.remove (aElement.getID ());
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    for (final IMicroElement eSMPBusinessCard : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      _addSMPBusinessCard (MicroTypeConverter.convertToNative (eSMPBusinessCard, SMPBusinessCard.class),
                           EDAOActionType.CREATE);
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
    for (final ISMPBusinessCard aSMPBusinessCard : CollectionHelper.getSortedByKey (m_aMap).values ())
      eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aSMPBusinessCard, ELEMENT_ITEM));
    return aDoc;
  }

  @MustBeLocked (ELockType.WRITE)
  private void _addSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard, final EDAOActionType eActionType)
  {
    ValueEnforcer.notNull (aSMPBusinessCard, "SMPBusinessCard");

    final String sSMPBusinessCardID = aSMPBusinessCard.getID ();
    if (eActionType == EDAOActionType.CREATE)
    {
      if (m_aMap.containsKey (sSMPBusinessCardID))
        throw new IllegalArgumentException ("SMPBusinessCard ID '" + sSMPBusinessCardID + "' is already in use!");
    }
    else
    {
      if (!m_aMap.containsKey (sSMPBusinessCardID))
        throw new IllegalArgumentException ("SMPBusinessCard ID '" + sSMPBusinessCardID + "' cannot be updated!");
    }
    m_aMap.put (aSMPBusinessCard.getID (), aSMPBusinessCard);
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPBusinessCard _createSMPBusinessCard (@Nonnull final SMPBusinessCard aSMPBusinessCard)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPBusinessCard (aSMPBusinessCard, EDAOActionType.CREATE);
      markAsChanged (aSMPBusinessCard, EDAOActionType.CREATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
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
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aMap.put (aSMPBusinessCard.getID (), aSMPBusinessCard);
      markAsChanged (aSMPBusinessCard, EDAOActionType.UPDATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
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
                                                         @Nonnull final List <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aEntities, "Entities");

    final ISMPBusinessCard aOldBusinessCard = getSMPBusinessCardOfServiceGroup (aServiceGroup);
    final SMPBusinessCard aNewBusinessCard = new SMPBusinessCard (aServiceGroup, aEntities);
    if (aOldBusinessCard != null)
    {
      // Reuse old ID
      _updateSMPBusinessCard (aNewBusinessCard);
    }
    else
    {
      // Create new ID
      _createSMPBusinessCard (aNewBusinessCard);
    }
    return aNewBusinessCard;
  }

  @Nonnull
  public EChange deleteSMPBusinessCard (@Nullable final ISMPBusinessCard aSMPBusinessCard)
  {
    if (aSMPBusinessCard == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPBusinessCard aRealServiceMetadata = m_aMap.remove (aSMPBusinessCard.getID ());
      if (aRealServiceMetadata == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPBusinessCard.OT, "no-such-id", aSMPBusinessCard.getID ());
        return EChange.UNCHANGED;
      }

      markAsChanged (aRealServiceMetadata, EDAOActionType.DELETE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPBusinessCard.OT,
                                      aSMPBusinessCard.getID (),
                                      aSMPBusinessCard.getServiceGroupID (),
                                      Integer.valueOf (aSMPBusinessCard.getEntityCount ()));
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return CollectionHelper.newList (m_aMap.values ());
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nullable
  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return null;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.get (aServiceGroup.getID ());
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnegative
  public int getSMPBusinessCardCount ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.size ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }
}
