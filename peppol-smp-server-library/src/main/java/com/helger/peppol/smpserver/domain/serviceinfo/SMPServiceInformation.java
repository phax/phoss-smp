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
package com.helger.peppol.smpserver.domain.serviceinfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ProcessListType;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smp.ServiceInformationType;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Default implementation of the {@link ISMPServiceInformation} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPServiceInformation implements ISMPServiceInformation
{
  public static final ObjectType OT = new ObjectType ("smpserviceinformation");

  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private SimpleDocumentTypeIdentifier m_aDocumentTypeIdentifier;
  private final Map <IPeppolProcessIdentifier, SMPProcess> m_aProcesses = new LinkedHashMap <> ();
  private String m_sExtension;

  /**
   * Constructor for new service information
   *
   * @param aServiceGroup
   *        Owning service group
   * @param aDocumentTypeIdentifier
   *        Document type ID
   * @param aProcesses
   *        process ID list
   * @param sExtension
   *        Optional extension
   */
  public SMPServiceInformation (@Nonnull final ISMPServiceGroup aServiceGroup,
                                @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                @Nonnull final List <SMPProcess> aProcesses,
                                @Nullable final String sExtension)
  {
    this (GlobalIDFactory.getNewPersistentStringID (), aServiceGroup, aDocumentTypeIdentifier, aProcesses, sExtension);
  }

  public SMPServiceInformation (@Nonnull @Nonempty final String sID,
                                @Nonnull final ISMPServiceGroup aServiceGroup,
                                @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                @Nonnull final List <SMPProcess> aProcesses,
                                @Nullable final String sExtension)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
    m_aServiceGroup = ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    setDocumentTypeIdentifier (aDocumentTypeIdentifier);
    ValueEnforcer.notEmptyNoNullValue (aProcesses, "Processes");
    for (final SMPProcess aProcess : aProcesses)
      addProcess (aProcess);
    setExtension (sExtension);
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public ISMPServiceGroup getServiceGroup ()
  {
    return m_aServiceGroup;
  }

  @Nonnull
  @Nonempty
  public String getServiceGroupID ()
  {
    return m_aServiceGroup.getID ();
  }

  @Nonnull
  public IPeppolDocumentTypeIdentifier getDocumentTypeIdentifier ()
  {
    return m_aDocumentTypeIdentifier;
  }

  public void setDocumentTypeIdentifier (@Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");
    // Make a copy to avoid external changes
    m_aDocumentTypeIdentifier = new SimpleDocumentTypeIdentifier (aDocumentTypeIdentifier);
  }

  @Nonnegative
  public int getProcessCount ()
  {
    return m_aProcesses.size ();
  }

  @Nullable
  public SMPProcess getProcessOfID (@Nullable final IPeppolProcessIdentifier aProcessID)
  {
    if (aProcessID == null)
      return null;
    return m_aProcesses.get (aProcessID);
  }

  @Nonnull
  public List <SMPProcess> getAllProcesses ()
  {
    return CollectionHelper.newList (m_aProcesses.values ());
  }

  public void addProcess (@Nonnull final SMPProcess aProcess)
  {
    ValueEnforcer.notNull (aProcess, "Process");
    final IPeppolProcessIdentifier aProcessID = aProcess.getProcessIdentifier ();
    if (m_aProcesses.containsKey (aProcessID))
      throw new IllegalStateException ("A process with ID '" + aProcessID.getURIEncoded () + "' is already contained!");
    m_aProcesses.put (aProcessID, aProcess);
  }

  public void setProcesses (@Nonnull @Nonempty final Map <IPeppolProcessIdentifier, SMPProcess> aProcesses)
  {
    ValueEnforcer.notEmptyNoNullValue (aProcesses, "Processes");
    m_aProcesses.clear ();
    m_aProcesses.putAll (aProcesses);
  }

  public boolean hasExtension ()
  {
    return StringHelper.hasText (m_sExtension);
  }

  @Nullable
  public String getExtension ()
  {
    return m_sExtension;
  }

  public void setExtension (@Nullable final String sExtension)
  {
    m_sExtension = sExtension;
  }

  @Nonnegative
  public int getTotalEndpointCount ()
  {
    int ret = 0;
    for (final ISMPProcess aProcess : m_aProcesses.values ())
      ret += aProcess.getEndpointCount ();
    return ret;
  }

  @Nonnull
  public ServiceMetadataType getAsJAXBObject ()
  {
    final ServiceInformationType aSI = new ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (m_aServiceGroup.getParticpantIdentifier ()));
    aSI.setDocumentIdentifier (new SimpleDocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    final ProcessListType aProcesses = new ProcessListType ();
    for (final ISMPProcess aProcess : m_aProcesses.values ())
      aProcesses.addProcess (aProcess.getAsJAXBObject ());
    aSI.setProcessList (aProcesses);
    aSI.setExtension (SMPExtensionConverter.convertOrNull (m_sExtension));

    final ServiceMetadataType ret = new ServiceMetadataType ();
    ret.setServiceInformation (aSI);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final SMPServiceInformation rhs = (SMPServiceInformation) o;
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("ServiceGroup", m_aServiceGroup)
                                       .append ("DocumentTypeIdentifier", m_aDocumentTypeIdentifier)
                                       .append ("Processes", m_aProcesses)
                                       .appendIfNotEmpty ("Extension", m_sExtension)
                                       .toString ();
  }

  @Nonnull
  public static SMPServiceInformation createFromJAXB (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                      @Nonnull final ServiceInformationType aServiceInformation)
  {
    final List <SMPProcess> aProcesses = new ArrayList <SMPProcess> ();
    for (final ProcessType aProcess : aServiceInformation.getProcessList ().getProcess ())
      aProcesses.add (SMPProcess.createFromJAXB (aProcess));
    return new SMPServiceInformation (aServiceGroup,
                                      aServiceInformation.getDocumentIdentifier (),
                                      aProcesses,
                                      SMPExtensionConverter.convertToString (aServiceInformation.getExtension ()));
  }
}
