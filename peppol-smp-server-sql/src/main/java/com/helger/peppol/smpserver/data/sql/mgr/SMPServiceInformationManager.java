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
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class SMPServiceInformationManager implements ISMPServiceInformationManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPServiceInformationManager.class);
  private final ReadWriteLock m_aRWLock = new ReentrantReadWriteLock ();
  private final Map <String, SMPServiceInformation> m_aMap = new HashMap <String, SMPServiceInformation> ();

  public SMPServiceInformationManager ()
  {}

  private void _addSMPServiceInformation (@Nonnull final SMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "SMPServiceInformation");

    final String sSMPServiceInformationID = aSMPServiceInformation.getID ();
    if (m_aMap.containsKey (sSMPServiceInformationID))
      throw new IllegalArgumentException ("SMPServiceInformation ID '" +
                                          sSMPServiceInformationID +
                                          "' is already in use!");
    m_aMap.put (aSMPServiceInformation.getID (), aSMPServiceInformation);
  }

  @Nonnull
  private ISMPServiceInformation _createSMPServiceInformation (@Nonnull final SMPServiceInformation aSMPServiceInformation)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPServiceInformation (aSMPServiceInformation);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return aSMPServiceInformation;
  }

  @Nonnull
  private ISMPServiceInformation _updateSMPServiceInformation (@Nonnull final SMPServiceInformation aSMPServiceInformation)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {}
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return aSMPServiceInformation;
  }

  @Nonnull
  public ISMPServiceInformation updateSMPServiceInformation (final String sServiceInfoID)
  {
    final SMPServiceInformation aServiceInfo = (SMPServiceInformation) getSMPServiceInformationOfID (sServiceInfoID);
    if (aServiceInfo == null)
      return null;

    return _updateSMPServiceInformation (aServiceInfo);
  }

  @Nonnull
  public ISMPServiceInformation createSMPServiceInformation (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                             @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                             @Nonnull final List <SMPProcess> aProcesses,
                                                             @Nullable final String sExtension)
  {
    final SMPServiceInformation aSMPServiceInformation = new SMPServiceInformation (aServiceGroup,
                                                                                    aDocumentTypeIdentifier,
                                                                                    aProcesses,
                                                                                    sExtension);
    return createSMPServiceInformation (aSMPServiceInformation);
  }

  public ISMPServiceInformation findServiceInformation (@Nullable final String sServiceGroupID,
                                                        @Nullable final IPeppolDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IPeppolProcessIdentifier aProcessID,
                                                        @Nullable final ESMPTransportProfile eTransportProfile)
  {
    final ISMPServiceInformation aOldInformation = getSMPServiceInformationOfServiceGroupAndDocumentType (sServiceGroupID,
                                                                                                          aDocTypeID);
    if (aOldInformation != null)
    {
      final ISMPProcess aProcess = aOldInformation.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (eTransportProfile);
        if (aEndpoint != null)
          return aOldInformation;
      }
    }
    return null;
  }

  @Nonnull
  public ISMPServiceInformation createSMPServiceInformation (@Nonnull final SMPServiceInformation aServiceInformation)
  {
    ValueEnforcer.notNull (aServiceInformation, "ServiceInformation");
    ValueEnforcer.isTrue (aServiceInformation.getProcessCount () == 1, "ServiceGroup must contain a single process");
    final SMPProcess aNewProcess = aServiceInformation.getAllProcesses ().get (0);
    ValueEnforcer.isTrue (aNewProcess.getEndpointCount () == 1,
                          "ServiceGroup must contain a single endpoint in the process");

    // Check for an update
    boolean bChangedExisting = false;
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceInformation.getServiceGroupID (),
                                                                                                                                 aServiceInformation.getDocumentTypeIdentifier ());
    if (aOldInformation != null)
    {
      final SMPProcess aOldProcess = aOldInformation.getProcessOfID (aNewProcess.getProcessIdentifier ());
      if (aOldProcess != null)
      {
        final SMPEndpoint aNewEndpoint = aNewProcess.getAllEndpoints ().get (0);
        final ISMPEndpoint aOldEndpoint = aOldProcess.getEndpointOfTransportProfile (aNewEndpoint.getTransportProfile ());
        if (aOldEndpoint != null)
        {
          // Overwrite existing endpoint
          aOldProcess.setEndpoint (aNewEndpoint.getTransportProfile (), aNewEndpoint);
        }
        else
        {
          // Add endpoint to existing process
          aOldProcess.addEndpoint (aNewEndpoint);
        }
      }
      else
      {
        // Add process to existing service information
        aOldInformation.addProcess (aNewProcess);
      }
      bChangedExisting = true;
    }

    if (bChangedExisting)
      return _updateSMPServiceInformation (aOldInformation);

    return _createSMPServiceInformation (aServiceInformation);
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
        return EChange.UNCHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;
    return deleteAllSMPServiceInformationOfServiceGroup (aServiceGroup.getID ());
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    EChange eChange = EChange.UNCHANGED;
    for (final ISMPServiceInformation aSMPServiceInformation : getAllSMPServiceInformationsOfServiceGroup (sServiceGroupID))
      eChange = eChange.or (deleteSMPServiceInformation (aSMPServiceInformation));
    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformations ()
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
    return getAllSMPServiceInformationsOfServiceGroup (aServiceGroup == null ? null : aServiceGroup.getID ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    final Collection <ISMPServiceInformation> ret = new ArrayList <ISMPServiceInformation> ();
    if (StringHelper.hasText (sServiceGroupID))
    {
      m_aRWLock.readLock ().lock ();
      try
      {
        for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
          if (aServiceInformation.getServiceGroupID ().equals (sServiceGroupID))
            ret.add (aServiceInformation);
      }
      finally
      {
        m_aRWLock.readLock ().unlock ();
      }
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final String sServiceGroupID,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (StringHelper.hasNoText (sServiceGroupID))
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final List <ISMPServiceInformation> ret = new ArrayList <ISMPServiceInformation> ();

    m_aRWLock.readLock ().lock ();
    try
    {
      for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
        if (aServiceInformation.getServiceGroupID ().equals (sServiceGroupID) &&
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
                      sServiceGroupID +
                      "' and document type '" +
                      aDocumentTypeIdentifier.getValue () +
                      "'. This seems to be a bug! Using the first one.");
    return ret.get (0);
  }

  public ISMPServiceInformation getSMPServiceInformationOfID (@Nullable final String sID)
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

  public boolean containsSMPServiceInformationWithID (@Nullable final String sID)
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
}
