/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.bdxr.smp1.process.BDXR1ProcessIdentifier;
import com.helger.peppolid.bdxr.smp2.process.BDXR2ProcessIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.smpclient.peppol.utils.SMPExtensionConverter;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ProcessType;

/**
 * Default implementation of the {@link ISMPProcess} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPProcess extends AbstractSMPHasExtension implements ISMPProcess
{
  private IProcessIdentifier m_aProcessIdentifier;
  private final ICommonsOrderedMap <String, SMPEndpoint> m_aEndpoints = new CommonsLinkedHashMap <> ();

  public SMPProcess (@Nonnull final IProcessIdentifier aProcessIdentifier,
                     @Nullable final List <SMPEndpoint> aEndpoints,
                     @Nullable final String sExtension)
  {
    setProcessIdentifier (aProcessIdentifier);
    if (aEndpoints != null)
      for (final SMPEndpoint aEndpoint : aEndpoints)
        addEndpoint (aEndpoint);
    getExtensions ().setExtensionAsString (sExtension);
  }

  @Nonnull
  public final IProcessIdentifier getProcessIdentifier ()
  {
    return m_aProcessIdentifier;
  }

  public final void setProcessIdentifier (@Nonnull final IProcessIdentifier aProcessIdentifier)
  {
    ValueEnforcer.notNull (aProcessIdentifier, "ProcessIdentifier");
    m_aProcessIdentifier = aProcessIdentifier;
  }

  @Nonnegative
  public int getEndpointCount ()
  {
    return m_aEndpoints.size ();
  }

  @Nullable
  public SMPEndpoint getEndpointOfTransportProfile (@Nullable final String sTransportProfile)
  {
    if (StringHelper.isEmpty (sTransportProfile))
      return null;
    return m_aEndpoints.get (sTransportProfile);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllEndpoints ()
  {
    return new CommonsArrayList <> (m_aEndpoints.values ());
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    return StringHelper.isNotEmpty (sTransportProfileID) && m_aEndpoints.containsKey (sTransportProfileID);
  }

  public final void addEndpoint (@Nonnull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    if (m_aEndpoints.containsKey (sTransportProfile))
      throw new IllegalStateException ("Another endpoint with transport profile '" +
                                       sTransportProfile +
                                       "' is already present");
    m_aEndpoints.put (sTransportProfile, aEndpoint);
  }

  public final void addEndpoints (@Nonnull final Iterable <? extends SMPEndpoint> aEndpoints)
  {
    ValueEnforcer.notNull (aEndpoints, "Endpoints");
    for (final SMPEndpoint aEndpoint : aEndpoints)
      addEndpoint (aEndpoint);
  }

  public final void setEndpoint (@Nonnull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    m_aEndpoints.put (sTransportProfile, aEndpoint);
  }

  @Nonnull
  public EChange deleteEndpoint (@Nullable final String sTransportProfile)
  {
    if (StringHelper.isEmpty (sTransportProfile))
      return EChange.UNCHANGED;
    return EChange.valueOf (m_aEndpoints.remove (sTransportProfile) != null);
  }

  @Nullable
  public com.helger.xsds.peppol.smp1.ProcessType getAsJAXBObjectPeppol ()
  {
    if (m_aEndpoints.isEmpty ())
    {
      // Empty ServiceEndpointList is not allowed
      return null;
    }

    final com.helger.xsds.peppol.smp1.ProcessType ret = new com.helger.xsds.peppol.smp1.ProcessType ();
    // Explicit constructor call is needed here!
    ret.setProcessIdentifier (new SimpleProcessIdentifier (m_aProcessIdentifier));
    final com.helger.xsds.peppol.smp1.ServiceEndpointList aEndpointList = new com.helger.xsds.peppol.smp1.ServiceEndpointList ();
    for (final ISMPEndpoint aEndpoint : m_aEndpoints.values ())
      aEndpointList.addEndpoint (aEndpoint.getAsJAXBObjectPeppol ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (getExtensions ().getAsPeppolExtension ());
    return ret;
  }

  @Nullable
  public com.helger.xsds.bdxr.smp1.ProcessType getAsJAXBObjectBDXR1 ()
  {
    if (m_aEndpoints.isEmpty ())
    {
      // Empty ServiceEndpointList is not allowed
      return null;
    }

    final com.helger.xsds.bdxr.smp1.ProcessType ret = new com.helger.xsds.bdxr.smp1.ProcessType ();
    // Explicit constructor call is needed here!
    ret.setProcessIdentifier (new BDXR1ProcessIdentifier (m_aProcessIdentifier));
    final com.helger.xsds.bdxr.smp1.ServiceEndpointList aEndpointList = new com.helger.xsds.bdxr.smp1.ServiceEndpointList ();
    for (final ISMPEndpoint aEndpoint : m_aEndpoints.values ())
      aEndpointList.addEndpoint (aEndpoint.getAsJAXBObjectBDXR1 ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (getExtensions ().getAsBDXRExtensions ());
    return ret;
  }

  @Nullable
  public com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType getAsJAXBObjectBDXR2 ()
  {
    if (m_aEndpoints.isEmpty ())
    {
      // Empty ServiceEndpointList is not allowed
      return null;
    }

    final com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType ret = new com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType ();
    ret.setSMPExtensions (getExtensions ().getAsBDXR2Extensions ());

    {
      final com.helger.xsds.bdxr.smp2.ac.ProcessType p = new com.helger.xsds.bdxr.smp2.ac.ProcessType ();
      // Explicit constructor call is needed here!
      p.setID (new BDXR2ProcessIdentifier (m_aProcessIdentifier));
      ret.addProcess (p);
    }

    for (final ISMPEndpoint aEndpoint : m_aEndpoints.values ())
      ret.addEndpoint (aEndpoint.getAsJAXBObjectBDXR2 ());

    if (ret.hasNoEndpointEntries ())
    {
      // Avoid creating invalid entries
      return null;
    }

    // Redirect is handled somewhere else
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!super.equals (o))
      return false;

    final SMPProcess rhs = (SMPProcess) o;
    return EqualsHelper.equals (m_aProcessIdentifier, rhs.m_aProcessIdentifier) &&
           EqualsHelper.equals (m_aEndpoints, rhs.m_aEndpoints);
  }

  @Override
  public int hashCode ()
  {
    return HashCodeGenerator.getDerived (super.hashCode ())
                            .append (m_aProcessIdentifier)
                            .append (m_aEndpoints)
                            .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("ProcessIdentifier", m_aProcessIdentifier)
                            .append ("Endpoints", m_aEndpoints)
                            .getToString ();
  }

  @Nonnull
  public static SMPProcess createFromJAXB (@Nonnull final ProcessType aProcess)
  {
    final ICommonsList <SMPEndpoint> aEndpoints = new CommonsArrayList <> ();
    for (final EndpointType aEndpoint : aProcess.getServiceEndpointList ().getEndpoint ())
      aEndpoints.add (SMPEndpoint.createFromJAXB (aEndpoint));
    return new SMPProcess (SimpleProcessIdentifier.wrap (aProcess.getProcessIdentifier ()),
                           aEndpoints,
                           SMPExtensionConverter.convertToString (aProcess.getExtension ()));
  }
}
