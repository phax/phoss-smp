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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.audit.AuditHelper;

public final class XMLServiceGroupManager extends AbstractWALDAO <SMPServiceGroup> implements ISMPServiceGroupManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLServiceGroupManager.class);

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
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID, @Nullable @Nonnull final IParticipantIdentifier aParticipantIdentifier, @Nullable final String sExtension)
  {
    s_aLogger.info ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier) +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

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

    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT, aSMPServiceGroup.getID (), sOwnerID, IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier), sExtension);
    s_aLogger.info ("createSMPServiceGroup succeeded");
    return aSMPServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID, @Nonnull @Nonempty final String sNewOwnerID, @Nullable final String sExtension)
  {
    s_aLogger.info ("updateSMPServiceGroup (" + sSMPServiceGroupID + ", " + sNewOwnerID + ", " + (StringHelper.hasText (sExtension) ? "with extension" : "without extension") + ")");

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
      eChange = eChange.or (aSMPServiceGroup.setOwnerID (sNewOwnerID));
      eChange = eChange.or (aSMPServiceGroup.setExtension (sExtension));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged (aSMPServiceGroup, EDAOActionType.UPDATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT, "all", sSMPServiceGroupID, sNewOwnerID, sExtension);
    s_aLogger.info ("updateSMPServiceGroup succeeded");
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    s_aLogger.info ("deleteSMPServiceGroup (" + IdentifierHelper.getIdentifierURIEncoded (aParticipantID) + ")");

    final ISMPServiceGroup aSMPServiceGroup = getSMPServiceGroupOfID (aParticipantID);
    if (aSMPServiceGroup == null)
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, "no-such-id", aParticipantID);
      return EChange.UNCHANGED;
    }

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
      SMPMetaManager.getRedirectMgr ().deleteAllSMPRedirectsOfServiceGroup (aSMPServiceGroup);
      SMPMetaManager.getServiceInformationMgr ().deleteAllSMPServiceInformationOfServiceGroup (aSMPServiceGroup);

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
    s_aLogger.info ("deleteSMPServiceGroup succeeded");
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

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
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

    final String sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
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
