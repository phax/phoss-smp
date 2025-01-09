/*
 * Copyright (C) 2015-2024 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

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
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.bdxr.smp1.doctype.BDXR1DocumentTypeIdentifier;
import com.helger.peppolid.bdxr.smp1.participant.BDXR1ParticipantIdentifier;
import com.helger.peppolid.bdxr.smp2.doctype.BDXR2DocumentTypeIdentifier;
import com.helger.peppolid.bdxr.smp2.participant.BDXR2ParticipantIdentifier;
import com.helger.peppolid.simple.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;

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
  private final IParticipantIdentifier m_aParticipantID;
  private final String m_sServiceGroupID;
  private IDocumentTypeIdentifier m_aDocumentTypeIdentifier;
  private final ICommonsOrderedMap <String, SMPProcess> m_aProcesses = new CommonsLinkedHashMap <> ();

  /**
   * Constructor for new service information
   *
   * @param aParticipantID
   *        Owning service group participant ID
   * @param aDocumentTypeIdentifier
   *        Document type ID
   * @param aProcesses
   *        Processes list. May be <code>null</code>.
   * @param sExtension
   *        Optional extension. May be <code>null</code>.
   */
  public SMPServiceInformation (@Nonnull final IParticipantIdentifier aParticipantID,
                                @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                @Nullable final List <SMPProcess> aProcesses,
                                @Nullable final String sExtension)
  {
    m_aParticipantID = ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    m_sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    setDocumentTypeIdentifier (aDocumentTypeIdentifier);
    if (aProcesses != null)
      for (final SMPProcess aProcess : aProcesses)
        addProcess (aProcess);
    getExtensions ().setExtensionAsString (sExtension);
    m_sID = m_sServiceGroupID + "-" + aDocumentTypeIdentifier.getURIEncoded ();
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public IParticipantIdentifier getServiceGroupParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  @Nonnull
  @Nonempty
  public String getServiceGroupID ()
  {
    return m_sServiceGroupID;
  }

  @Nonnull
  public IDocumentTypeIdentifier getDocumentTypeIdentifier ()
  {
    return m_aDocumentTypeIdentifier;
  }

  public final void setDocumentTypeIdentifier (@Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier)
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

  public final void addProcess (@Nonnull final SMPProcess aProcess)
  {
    ValueEnforcer.notNull (aProcess, "Process");
    final String sProcessID = _getKey (aProcess.getProcessIdentifier ());
    if (m_aProcesses.containsKey (sProcessID))
      throw new IllegalStateException ("A process with ID '" + sProcessID + "' is already contained!");
    m_aProcesses.put (sProcessID, aProcess);
  }

  public final void addProcesses (@Nonnull final Iterable <? extends SMPProcess> aProcesses)
  {
    ValueEnforcer.notNull (aProcesses, "Processes");
    for (final SMPProcess aProcess : aProcesses)
      addProcess (aProcess);
  }

  public final void setProcesses (@Nonnull @Nonempty final Map <String, ? extends SMPProcess> aProcesses)
  {
    ValueEnforcer.notEmptyNoNullValue (aProcesses, "Processes");
    m_aProcesses.setAll (aProcesses);
  }

  @Nonnull
  public EChange deleteProcess (@Nullable final IProcessIdentifier aProcessID)
  {
    if (aProcessID == null)
      return EChange.UNCHANGED;

    final String sProcessID = _getKey (aProcessID);
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

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.hasNoText (sTransportProfileID))
      return false;

    return m_aProcesses.containsAnyValue (x -> x.containsAnyEndpointWithTransportProfile (sTransportProfileID));
  }

  @Nullable
  public com.helger.xsds.peppol.smp1.ServiceMetadataType getAsJAXBObjectPeppol ()
  {
    if (m_aProcesses.isEmpty ())
    {
      // "ProcessList" is mandatory and MUST contain at least 1 value
      return null;
    }

    final com.helger.xsds.peppol.smp1.ServiceInformationType aSI = new com.helger.xsds.peppol.smp1.ServiceInformationType ();
    // Explicit constructor call is needed here!
    aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (m_aParticipantID));
    aSI.setDocumentIdentifier (new SimpleDocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    final com.helger.xsds.peppol.smp1.ProcessListType aProcesses = new com.helger.xsds.peppol.smp1.ProcessListType ();
    for (final ISMPProcess aProcess : m_aProcesses.values ())
    {
      final com.helger.xsds.peppol.smp1.ProcessType aJAXBProcess = aProcess.getAsJAXBObjectPeppol ();
      if (aJAXBProcess != null)
        aProcesses.addProcess (aJAXBProcess);
    }
    if (aProcesses.hasNoProcessEntries ())
    {
      // "ProcessList" is mandatory and MUST contain at least 1 value
      return null;
    }
    aSI.setProcessList (aProcesses);
    aSI.setExtension (getExtensions ().getAsPeppolExtension ());

    final com.helger.xsds.peppol.smp1.ServiceMetadataType ret = new com.helger.xsds.peppol.smp1.ServiceMetadataType ();
    ret.setServiceInformation (aSI);
    return ret;
  }

  @Nullable
  public com.helger.xsds.bdxr.smp1.ServiceMetadataType getAsJAXBObjectBDXR1 ()
  {
    if (m_aProcesses.isEmpty ())
    {
      // "ProcessList" is mandatory and MUST contain at least 1 value
      return null;
    }

    final com.helger.xsds.bdxr.smp1.ServiceInformationType aSI = new com.helger.xsds.bdxr.smp1.ServiceInformationType ();
    // Explicit constructor call is needed here!
    aSI.setParticipantIdentifier (new BDXR1ParticipantIdentifier (m_aParticipantID));
    aSI.setDocumentIdentifier (new BDXR1DocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    final com.helger.xsds.bdxr.smp1.ProcessListType aProcesses = new com.helger.xsds.bdxr.smp1.ProcessListType ();
    for (final ISMPProcess aProcess : m_aProcesses.values ())
    {
      final com.helger.xsds.bdxr.smp1.ProcessType aJAXBProcess = aProcess.getAsJAXBObjectBDXR1 ();
      if (aJAXBProcess != null)
        aProcesses.addProcess (aJAXBProcess);
    }
    if (aProcesses.hasNoProcessEntries ())
    {
      // "ProcessList" is mandatory and MUST contain at least 1 value
      return null;
    }

    aSI.setProcessList (aProcesses);
    aSI.setExtension (getExtensions ().getAsBDXRExtensions ());

    final com.helger.xsds.bdxr.smp1.ServiceMetadataType ret = new com.helger.xsds.bdxr.smp1.ServiceMetadataType ();
    ret.setServiceInformation (aSI);
    return ret;
  }

  @Nullable
  public com.helger.xsds.bdxr.smp2.ServiceMetadataType getAsJAXBObjectBDXR2 ()
  {
    if (m_aProcesses.isEmpty ())
    {
      // "ProcessList" is mandatory and MUST contain at least 1 value
      return null;
    }

    final com.helger.xsds.bdxr.smp2.ServiceMetadataType ret = new com.helger.xsds.bdxr.smp2.ServiceMetadataType ();
    ret.setSMPExtensions (getExtensions ().getAsBDXR2Extensions ());
    ret.setSMPVersionID ("2.0");
    // It's okay to use the constructor directly
    ret.setID (new BDXR2DocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    // Explicit constructor call is needed here!
    ret.setParticipantID (new BDXR2ParticipantIdentifier (m_aParticipantID));

    for (final ISMPProcess aProcess : m_aProcesses.values ())
    {
      final com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType aJAXBProcess = aProcess.getAsJAXBObjectBDXR2 ();
      if (aJAXBProcess != null)
        ret.addProcessMetadata (aJAXBProcess);
    }
    if (ret.hasNoProcessMetadataEntries ())
    {
      // "ProcessList" is mandatory and MUST contain at least 1 value
      return null;
    }

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
                            .append ("ParticipantID", m_aParticipantID)
                            .append ("DocumentTypeIdentifier", m_aDocumentTypeIdentifier)
                            .append ("Processes", m_aProcesses)
                            .getToString ();
  }
}
