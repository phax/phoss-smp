/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.serviceinfo;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.identifier.bdxr.doctype.BDXRDocumentTypeIdentifier;
import com.helger.peppol.identifier.bdxr.participant.BDXRParticipantIdentifier;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smpserver.domain.extension.AbstractSMPHasExtension;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Default implementation of the {@link ISMPServiceInformation} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPServiceInformation extends AbstractSMPHasExtension implements ISMPServiceInformation
{
  public static final ObjectType OT = new ObjectType ("smpserviceinformation");

  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private IDocumentTypeIdentifier m_aDocumentTypeIdentifier;
  private final ICommonsOrderedMap <String, SMPProcess> m_aProcesses = new CommonsLinkedHashMap<> ();

  /**
   * Constructor for new service information
   *
   * @param aServiceGroup
   *        Owning service group
   * @param aDocumentTypeIdentifier
   *        Document type ID
   * @param aProcesses
   *        processes list. May be <code>null</code>.
   * @param sExtension
   *        Optional extension
   */
  public SMPServiceInformation (@Nonnull final ISMPServiceGroup aServiceGroup,
                                @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                @Nullable final List <SMPProcess> aProcesses,
                                @Nullable final String sExtension)
  {
    m_aServiceGroup = ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    setDocumentTypeIdentifier (aDocumentTypeIdentifier);
    if (aProcesses != null)
      for (final SMPProcess aProcess : aProcesses)
        addProcess (aProcess);
    setExtensionAsString (sExtension);
    m_sID = aServiceGroup.getID () + "-" + aDocumentTypeIdentifier.getURIEncoded ();
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
  public IDocumentTypeIdentifier getDocumentTypeIdentifier ()
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

  @Nonnull
  private static String _getKey (@Nonnull final IProcessIdentifier aProcessID)
  {
    return aProcessID.getURIEncoded ();
  }

  @Nullable
  public SMPProcess getProcessOfID (@Nullable final IProcessIdentifier aProcessID)
  {
    if (aProcessID == null)
      return null;
    return m_aProcesses.get (_getKey (aProcessID));
  }

  @Nonnull
  public ICommonsList <ISMPProcess> getAllProcesses ()
  {
    return new CommonsArrayList<> (m_aProcesses.values ());
  }

  public void addProcess (@Nonnull final SMPProcess aProcess)
  {
    ValueEnforcer.notNull (aProcess, "Process");
    final String sProcessID = _getKey (aProcess.getProcessIdentifier ());
    if (m_aProcesses.containsKey (sProcessID))
      throw new IllegalStateException ("A process with ID '" + sProcessID + "' is already contained!");
    m_aProcesses.put (sProcessID, aProcess);
  }

  public void setProcesses (@Nonnull @Nonempty final Map <String, ? extends SMPProcess> aProcesses)
  {
    ValueEnforcer.notEmptyNoNullValue (aProcesses, "Processes");
    m_aProcesses.setAll (aProcesses);
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
  public com.helger.peppol.smp.ServiceMetadataType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.ServiceInformationType aSI = new com.helger.peppol.smp.ServiceInformationType ();
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (m_aServiceGroup.getParticpantIdentifier ()));
    aSI.setDocumentIdentifier (new SimpleDocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    final com.helger.peppol.smp.ProcessListType aProcesses = new com.helger.peppol.smp.ProcessListType ();
    for (final ISMPProcess aProcess : m_aProcesses.values ())
      aProcesses.addProcess (aProcess.getAsJAXBObjectPeppol ());
    aSI.setProcessList (aProcesses);
    aSI.setExtension (getAsPeppolExtension ());

    final com.helger.peppol.smp.ServiceMetadataType ret = new com.helger.peppol.smp.ServiceMetadataType ();
    ret.setServiceInformation (aSI);
    return ret;
  }

  @Nonnull
  public com.helger.peppol.bdxr.ServiceMetadataType getAsJAXBObjectBDXR ()
  {
    final com.helger.peppol.bdxr.ServiceInformationType aSI = new com.helger.peppol.bdxr.ServiceInformationType ();
    aSI.setParticipantIdentifier (new BDXRParticipantIdentifier (m_aServiceGroup.getParticpantIdentifier ()));
    aSI.setDocumentIdentifier (new BDXRDocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    final com.helger.peppol.bdxr.ProcessListType aProcesses = new com.helger.peppol.bdxr.ProcessListType ();
    for (final ISMPProcess aProcess : m_aProcesses.values ())
      aProcesses.addProcess (aProcess.getAsJAXBObjectBDXR ());
    aSI.setProcessList (aProcesses);
    aSI.setExtension (getAsBDXRExtension ());

    final com.helger.peppol.bdxr.ServiceMetadataType ret = new com.helger.peppol.bdxr.ServiceMetadataType ();
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
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("ID", m_sID)
                            .append ("ServiceGroup", m_aServiceGroup)
                            .append ("DocumentTypeIdentifier", m_aDocumentTypeIdentifier)
                            .append ("Processes", m_aProcesses)
                            .toString ();
  }

  @Nonnull
  public static SMPServiceInformation createFromJAXB (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                      @Nonnull final com.helger.peppol.smp.ServiceInformationType aServiceInformation)
  {
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList<> ();
    for (final com.helger.peppol.smp.ProcessType aProcess : aServiceInformation.getProcessList ().getProcess ())
      aProcesses.add (SMPProcess.createFromJAXB (aProcess));
    return new SMPServiceInformation (aServiceGroup,
                                      aServiceInformation.getDocumentIdentifier (),
                                      aProcesses,
                                      SMPExtensionConverter.convertToString (aServiceInformation.getExtension ()));
  }
}
