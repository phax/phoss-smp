/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
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
  private final ICommonsOrderedMap <String, SMPProcess> m_aProcesses = new CommonsLinkedHashMap <> ();

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
    m_aDocumentTypeIdentifier = aDocumentTypeIdentifier;
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
    return new CommonsArrayList <> (m_aProcesses.values ());
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

  @Nonnull
  public EChange deleteProcess (@Nullable final ISMPProcess aProcess)
  {
    if (aProcess == null)
      return EChange.UNCHANGED;

    final String sProcessID = _getKey (aProcess.getProcessIdentifier ());
    return m_aProcesses.removeObject (sProcessID);
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
    // Explicit constructor call is needed here!
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
    // Explicit constructor call is needed here!
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
                            .getToString ();
  }

  @Nonnull
  public static SMPServiceInformation createFromJAXB (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                      @Nonnull final com.helger.peppol.smp.ServiceInformationType aServiceInformation)
  {
    final ICommonsList <SMPProcess> aProcesses = new CommonsArrayList <> ();
    for (final com.helger.peppol.smp.ProcessType aProcess : aServiceInformation.getProcessList ().getProcess ())
      aProcesses.add (SMPProcess.createFromJAXB (aProcess));
    return new SMPServiceInformation (aServiceGroup,
                                      aServiceInformation.getDocumentIdentifier (),
                                      aProcesses,
                                      SMPExtensionConverter.convertToString (aServiceInformation.getExtension ()));
  }
}
