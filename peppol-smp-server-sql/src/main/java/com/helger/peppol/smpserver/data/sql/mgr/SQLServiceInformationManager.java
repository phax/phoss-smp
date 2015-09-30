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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
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
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class SQLServiceInformationManager implements ISMPServiceInformationManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SQLServiceInformationManager.class);
  private final Map <String, SMPServiceInformation> m_aMap = new HashMap <String, SMPServiceInformation> ();

  public SQLServiceInformationManager ()
  {}

  @Nonnull
  private ISMPServiceInformation _createSMPServiceInformation (@Nonnull final SMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "SMPServiceInformation");

    final String sSMPServiceInformationID = aSMPServiceInformation.getID ();
    if (m_aMap.containsKey (sSMPServiceInformationID))
      throw new IllegalArgumentException ("SMPServiceInformation ID '" +
                                          sSMPServiceInformationID +
                                          "' is already in use!");
    m_aMap.put (aSMPServiceInformation.getID (), aSMPServiceInformation);
    return aSMPServiceInformation;
  }

  @Nonnull
  private ISMPServiceInformation _updateSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformation)
  {
    return aSMPServiceInformation;
  }

  @Nonnull
  public ISMPServiceInformation markSMPServiceInformationChanged (@Nonnull final ISMPServiceInformation aServiceInfo)
  {
    ValueEnforcer.notNull (aServiceInfo, "ServiceInfo");

    return _updateSMPServiceInformation (aServiceInfo);
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IPeppolDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IPeppolProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aOldInformation = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                          aDocTypeID);
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

  @Nonnull
  public ISMPServiceInformation createOrUpdateSMPServiceInformation (@Nonnull final SMPServiceInformation aServiceInformation)
  {
    ValueEnforcer.notNull (aServiceInformation, "ServiceInformation");
    ValueEnforcer.isTrue (aServiceInformation.getProcessCount () == 1, "ServiceGroup must contain a single process");
    final SMPProcess aNewProcess = aServiceInformation.getAllProcesses ().get (0);
    ValueEnforcer.isTrue (aNewProcess.getEndpointCount () == 1,
                          "ServiceGroup must contain a single endpoint in the process");

    // Check for an update
    boolean bChangedExisting = false;
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceInformation.getServiceGroup (),
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

    final SMPServiceInformation aRealServiceInformation = m_aMap.remove (aSMPServiceInformation.getID ());
    if (aRealServiceInformation == null)
      return EChange.UNCHANGED;
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
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformations ()
  {
    return CollectionHelper.newList (m_aMap.values ());
  }

  @Nonnegative
  public int getSMPServiceInformationCount ()
  {
    return m_aMap.size ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final Collection <ISMPServiceInformation> ret = new ArrayList <ISMPServiceInformation> ();
    if (aServiceGroup != null)
    {
      for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
        if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()))
          ret.add (aServiceInformation);
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
      for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
        if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()))
          ret.add (aServiceInformation.getDocumentTypeIdentifier ());
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

    for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
      if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()) &&
          aServiceInformation.getDocumentTypeIdentifier ().equals (aDocumentTypeIdentifier))
      {
        ret.add (aServiceInformation);
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
