/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.data.xml.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.SMPHelper;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.security.audit.AuditHelper;

public final class XMLServiceGroupManager extends AbstractWALDAO <SMPServiceGroup>implements ISMPServiceGroupManager
{
  private static final String ELEMENT_ROOT = "servicegroups";
  private static final String ELEMENT_ITEM = "servicegroup";

  private final Map <String, SMPServiceGroup> m_aMap = new HashMap <String, SMPServiceGroup> ();
  private final IRegistrationHook m_aHook;

  public XMLServiceGroupManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPServiceGroup.class, sFilename);
    m_aHook = RegistrationHookFactory.getOrCreateInstance ();
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
                                                @Nullable final String sExtension)
  {
    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);

    // It's a new service group - throws exception in case of an error
    m_aHook.createServiceGroup (aParticipantIdentifier);

    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPServiceGroup (aSMPServiceGroup);
      markAsChanged (aSMPServiceGroup, EDAOActionType.CREATE);
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      m_aHook.undoCreateServiceGroup (aParticipantIdentifier);
      throw ex;
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
    final ISMPServiceGroup aSMPServiceGroup = getSMPServiceGroupOfID (aParticipantIdentifier);
    if (aSMPServiceGroup == null)
      return EChange.UNCHANGED;

    // Delete in SML - throws exception in case of error
    m_aHook.deleteServiceGroup (aSMPServiceGroup.getParticpantIdentifier ());

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceGroup aRealServiceGroup = m_aMap.remove (aSMPServiceGroup.getID ());
      if (aRealServiceGroup == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, "no-such-id", aSMPServiceGroup.getID ());
        return EChange.UNCHANGED;
      }

      // Delete all redirects and all service information of this service group
      // as well
      MetaManager.getRedirectMgr ().deleteAllSMPRedirectsOfServiceGroup (aSMPServiceGroup);
      MetaManager.getServiceInformationMgr ().deleteAllSMPServiceInformationOfServiceGroup (aSMPServiceGroup);

      markAsChanged (aRealServiceGroup, EDAOActionType.DELETE);
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      m_aHook.undoDeleteServiceGroup (aSMPServiceGroup.getParticpantIdentifier ());
      throw ex;
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

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    final List <ISMPServiceGroup> ret = new ArrayList <> ();
    m_aRWLock.readLock ().lock ();
    try
    {
      for (final ISMPServiceGroup aSG : m_aMap.values ())
        if (aSG.getOwnerID ().equals (sOwnerID))
          ret.add (aSG);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
    return ret;
  }

  @Nonnegative
  public int getSMPServiceGroupCountOfOwner (@Nonnull final String sOwnerID)
  {
    int ret = 0;
    m_aRWLock.readLock ().lock ();
    try
    {
      for (final ISMPServiceGroup aSG : m_aMap.values ())
        if (aSG.getOwnerID ().equals (sOwnerID))
          ++ret;
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
    return ret;
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;

    final String sID = SMPHelper.createSMPServiceGroupID (aParticipantIdentifier);
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

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return false;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.containsKey (SMPHelper.createSMPServiceGroupID (aParticipantIdentifier));
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
