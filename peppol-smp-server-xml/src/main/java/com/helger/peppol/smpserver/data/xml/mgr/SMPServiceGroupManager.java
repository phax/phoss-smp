/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPHelper;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.security.audit.AuditHelper;

public final class SMPServiceGroupManager extends AbstractWALDAO <SMPServiceGroup>implements ISMPServiceGroupManager
{
  private static final String ELEMENT_ROOT = "servicegroups";
  private static final String ELEMENT_ITEM = "servicegroup";

  private final Map <String, SMPServiceGroup> m_aMap = new HashMap <String, SMPServiceGroup> ();

  public SMPServiceGroupManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPServiceGroup.class, sFilename);
    initialRead ();
  }

  @Override
  protected void onRecoveryCreate (final SMPServiceGroup aElement)
  {
    _addSMPServiceGroup (aElement);
  }

  @Override
  protected void onRecoveryUpdate (final SMPServiceGroup aElement)
  {
    _addSMPServiceGroup (aElement);
  }

  @Override
  protected void onRecoveryDelete (final SMPServiceGroup aElement)
  {
    m_aMap.remove (aElement.getID ());
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    for (final IMicroElement eSMPServiceGroup : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      _addSMPServiceGroup (MicroTypeConverter.convertToNative (eSMPServiceGroup, SMPServiceGroup.class));
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
    for (final ISMPServiceGroup aSMPServiceGroup : CollectionHelper.getSortedByKey (m_aMap).values ())
      eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aSMPServiceGroup, ELEMENT_ITEM));
    return aDoc;
  }

  private void _addSMPServiceGroup (@Nonnull final SMPServiceGroup aSMPServiceGroup)
  {
    ValueEnforcer.notNull (aSMPServiceGroup, "SMPServiceGroup");

    final String sSMPServiceGroupID = aSMPServiceGroup.getID ();
    if (m_aMap.containsKey (sSMPServiceGroupID))
      throw new IllegalArgumentException ("SMPServiceGroup ID '" + sSMPServiceGroupID + "' is already in use!");
    m_aMap.put (aSMPServiceGroup.getID (), aSMPServiceGroup);
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nullable @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                                final String sExtension)
  {
    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);

    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPServiceGroup (aSMPServiceGroup);
      markAsChanged (aSMPServiceGroup, EDAOActionType.CREATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT,
                                      aSMPServiceGroup.getID (),
                                      sOwnerID,
                                      aParticipantIdentifier,
                                      sExtension);
    return aSMPServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID,
                                        @Nonnull @Nonempty final String sOwnerID,
                                        @Nullable final String sExtension)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceGroup aSMPServiceGroup = m_aMap.get (sSMPServiceGroupID);
      if (aSMPServiceGroup == null)
      {
        AuditHelper.onAuditModifyFailure (SMPServiceGroup.OT, sSMPServiceGroupID, "no-such-id");
        return EChange.UNCHANGED;
      }

      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPServiceGroup.setOwnerID (sOwnerID));
      eChange = eChange.or (aSMPServiceGroup.setExtension (sExtension));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged (aSMPServiceGroup, EDAOActionType.UPDATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT, "all", sSMPServiceGroupID, sOwnerID, sExtension);
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    return deleteSMPServiceGroup (getSMPServiceGroupOfID (aParticipantIdentifier));
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final ISMPServiceGroup aSMPServiceGroup)
  {
    if (aSMPServiceGroup == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceGroup aRealServiceGroup = m_aMap.remove (aSMPServiceGroup.getID ());
      if (aRealServiceGroup == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, "no-such-id", aSMPServiceGroup.getID ());
        return EChange.UNCHANGED;
      }

      markAsChanged (aRealServiceGroup, EDAOActionType.DELETE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPServiceGroup.OT, aSMPServiceGroup.getID ());
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroups ()
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

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;

    return getSMPServiceGroupOfID (SMPHelper.createSMPServiceGroupID (aParticipantIdentifier));
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.get (sID);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.containsKey (sID);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnegative
  public int getSMPServiceGroupCount ()
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
