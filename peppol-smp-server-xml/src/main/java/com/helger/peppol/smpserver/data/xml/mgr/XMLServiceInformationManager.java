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
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class XMLServiceInformationManager extends AbstractWALDAO <SMPServiceInformation> implements ISMPServiceInformationManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLServiceInformationManager.class);
  private static final String ELEMENT_ROOT = "serviceinformationlist";
  private static final String ELEMENT_ITEM = "serviceinformation";

  private final Map <String, SMPServiceInformation> m_aMap = new HashMap <String, SMPServiceInformation> ();

  public XMLServiceInformationManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPServiceInformation.class, sFilename);
    initialRead ();
  }

  @Override
  protected void onRecoveryCreate (final SMPServiceInformation aElement)
  {
    _addSMPServiceInformation (aElement);
  }

  @Override
  protected void onRecoveryUpdate (final SMPServiceInformation aElement)
  {
    _addSMPServiceInformation (aElement);
  }

  @Override
  protected void onRecoveryDelete (final SMPServiceInformation aElement)
  {
    m_aMap.remove (aElement.getID ());
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    for (final IMicroElement eSMPServiceInformation : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
    {
      _addSMPServiceInformation (MicroTypeConverter.convertToNative (eSMPServiceInformation, SMPServiceInformation.class));
    }
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
    for (final ISMPServiceInformation aSMPServiceInformation : CollectionHelper.getSortedByKey (m_aMap).values ())
      eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aSMPServiceInformation, ELEMENT_ITEM));
    return aDoc;
  }

  private void _addSMPServiceInformation (@Nonnull final SMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "SMPServiceInformation");

    final String sSMPServiceInformationID = aSMPServiceInformation.getID ();
    if (m_aMap.containsKey (sSMPServiceInformationID))
      throw new IllegalArgumentException ("SMPServiceInformation ID '" + sSMPServiceInformationID + "' is already in use!");
    m_aMap.put (aSMPServiceInformation.getID (), aSMPServiceInformation);
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IPeppolDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IPeppolProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aOldInformation = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID);
    if (aOldInformation != null)
    {
      final ISMPProcess aProcess = aOldInformation.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
        if (aEndpoint != null)
          return aOldInformation;
      }
    }
    return null;
  }

  public void mergeSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformationObj)
  {
    final SMPServiceInformation aSMPServiceInformation = (SMPServiceInformation) aSMPServiceInformationObj;
    ValueEnforcer.notNull (aSMPServiceInformation, "ServiceInformation");

    // Check for an update
    boolean bChangedExisting = false;
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aSMPServiceInformation.getServiceGroup (),
                                                                                                                                 aSMPServiceInformation.getDocumentTypeIdentifier ());
    if (aOldInformation != null)
    {
      // If a service information is present, it must be the provided object!
      // This is not true for the REST API
      if (aOldInformation == aSMPServiceInformation)
        bChangedExisting = true;
    }

    if (bChangedExisting)
    {
      // Edit existing
      m_aRWLock.writeLock ().lock ();
      try
      {
        markAsChanged (aOldInformation, EDAOActionType.UPDATE);
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
      AuditHelper.onAuditModifySuccess (SMPServiceInformation.OT,
                                        aOldInformation.getID (),
                                        aOldInformation.getServiceGroupID (),
                                        aOldInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aOldInformation.getAllProcesses (),
                                        aOldInformation.getExtension ());
    }
    else
    {
      // (Optionally delete the old one and) create the new one
      boolean bRemovedOld = false;
      m_aRWLock.writeLock ().lock ();
      try
      {
        if (aOldInformation != null)
          bRemovedOld = m_aMap.remove (aOldInformation.getID ()) == aOldInformation;

        if (bRemovedOld)
          markAsChanged (aOldInformation, EDAOActionType.DELETE);

        _addSMPServiceInformation (aSMPServiceInformation);
        markAsChanged (aSMPServiceInformation, EDAOActionType.CREATE);
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }

      if (bRemovedOld)
        AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT,
                                          aOldInformation.getID (),
                                          aOldInformation.getServiceGroupID (),
                                          aOldInformation.getDocumentTypeIdentifier ().getURIEncoded ());
      else
        if (aOldInformation != null)
          AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT,
                                            aOldInformation.getID (),
                                            aOldInformation.getServiceGroupID (),
                                            aOldInformation.getDocumentTypeIdentifier ().getURIEncoded ());

      AuditHelper.onAuditCreateSuccess (SMPServiceInformation.OT,
                                        aSMPServiceInformation.getID (),
                                        aSMPServiceInformation.getServiceGroupID (),
                                        aSMPServiceInformation.getDocumentTypeIdentifier ().getURIEncoded (),
                                        aSMPServiceInformation.getAllProcesses (),
                                        aSMPServiceInformation.getExtension ());
    }
  }

  @Nonnull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (aSMPServiceInformation == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceInformation aRealServiceInformation = m_aMap.remove (aSMPServiceInformation.getID ());
      if (aRealServiceInformation == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPServiceInformation.OT, "no-such-id", aSMPServiceInformation.getID ());
        return EChange.UNCHANGED;
      }

      markAsChanged (aRealServiceInformation, EDAOActionType.DELETE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPServiceInformation.OT,
                                      aSMPServiceInformation.getID (),
                                      aSMPServiceInformation.getServiceGroupID (),
                                      aSMPServiceInformation.getDocumentTypeIdentifier ().getURIEncoded ());
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationsOfServiceGroup (aServiceGroup))
      eChange = eChange.or (deleteSMPServiceInformation (aSMPServiceInformation));
    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformation ()
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

  @Nonnegative
  public int getSMPServiceInformationCount ()
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

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final Collection <ISMPServiceInformation> ret = new ArrayList <ISMPServiceInformation> ();
    if (aServiceGroup != null)
    {
      m_aRWLock.readLock ().lock ();
      try
      {
        for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
          if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()))
            ret.add (aServiceInformation);
      }
      finally
      {
        m_aRWLock.readLock ().unlock ();
      }
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final Collection <IDocumentTypeIdentifier> ret = new ArrayList <> ();
    if (aServiceGroup != null)
    {
      m_aRWLock.readLock ().lock ();
      try
      {
        for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
          if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()))
            ret.add (aServiceInformation.getDocumentTypeIdentifier ());
      }
      finally
      {
        m_aRWLock.readLock ().unlock ();
      }
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final List <ISMPServiceInformation> ret = new ArrayList <ISMPServiceInformation> ();

    m_aRWLock.readLock ().lock ();
    try
    {
      for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
        if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()) &&
            aServiceInformation.getDocumentTypeIdentifier ().equals (aDocumentTypeIdentifier))
        {
          ret.add (aServiceInformation);
        }
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }

    if (ret.isEmpty ())
      return null;
    if (ret.size () > 1)
      s_aLogger.warn ("Found more than one entry for service group '" +
                      aServiceGroup.getID () +
                      "' and document type '" +
                      aDocumentTypeIdentifier.getValue () +
                      "'. This seems to be a bug! Using the first one.");
    return ret.get (0);
  }
}
