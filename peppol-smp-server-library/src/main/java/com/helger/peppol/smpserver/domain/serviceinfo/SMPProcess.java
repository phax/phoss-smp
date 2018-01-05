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
import com.helger.peppol.identifier.bdxr.process.BDXRProcessIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.generic.process.SimpleProcessIdentifier;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smpserver.domain.extension.AbstractSMPHasExtension;

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
    setExtensionAsString (sExtension);
  }

  @Nonnull
  public IProcessIdentifier getProcessIdentifier ()
  {
    return m_aProcessIdentifier;
  }

  public void setProcessIdentifier (@Nonnull final IProcessIdentifier aProcessIdentifier)
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
  public SMPEndpoint getEndpointOfTransportProfile (@Nullable final ISMPTransportProfile eTransportProfile)
  {
    return getEndpointOfTransportProfile (eTransportProfile == null ? null : eTransportProfile.getID ());
  }

  @Nullable
  public SMPEndpoint getEndpointOfTransportProfile (@Nullable final String sTransportProfile)
  {
    if (StringHelper.hasNoText (sTransportProfile))
      return null;
    return m_aEndpoints.get (sTransportProfile);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllEndpoints ()
  {
    return new CommonsArrayList <> (m_aEndpoints.values ());
  }

  public void addEndpoint (@Nonnull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    if (m_aEndpoints.containsKey (sTransportProfile))
      throw new IllegalStateException ("Another endpoint with transport profile '" +
                                       sTransportProfile +
                                       "' is already present");
    m_aEndpoints.put (sTransportProfile, aEndpoint);
  }

  public void setEndpoint (@Nonnull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    m_aEndpoints.put (sTransportProfile, aEndpoint);
  }

  @Nonnull
  public EChange deleteEndpoint (@Nullable final String sTransportProfile)
  {
    if (StringHelper.hasNoText (sTransportProfile))
      return EChange.UNCHANGED;
    return EChange.valueOf (m_aEndpoints.remove (sTransportProfile) != null);
  }

  @Nonnull
  public com.helger.peppol.smp.ProcessType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.ProcessType ret = new com.helger.peppol.smp.ProcessType ();
    // Explicit constructor call is needed here!
    ret.setProcessIdentifier (new SimpleProcessIdentifier (m_aProcessIdentifier));
    final com.helger.peppol.smp.ServiceEndpointList aEndpointList = new com.helger.peppol.smp.ServiceEndpointList ();
    for (final ISMPEndpoint aEndpoint : m_aEndpoints.values ())
      aEndpointList.addEndpoint (aEndpoint.getAsJAXBObjectPeppol ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (getAsPeppolExtension ());
    return ret;
  }

  @Nonnull
  public com.helger.peppol.bdxr.ProcessType getAsJAXBObjectBDXR ()
  {
    final com.helger.peppol.bdxr.ProcessType ret = new com.helger.peppol.bdxr.ProcessType ();
    // Explicit constructor call is needed here!
    ret.setProcessIdentifier (new BDXRProcessIdentifier (m_aProcessIdentifier));
    final com.helger.peppol.bdxr.ServiceEndpointList aEndpointList = new com.helger.peppol.bdxr.ServiceEndpointList ();
    for (final ISMPEndpoint aEndpoint : m_aEndpoints.values ())
      aEndpointList.addEndpoint (aEndpoint.getAsJAXBObjectBDXR ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (getAsBDXRExtension ());
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
    return new SMPProcess (aProcess.getProcessIdentifier (),
                           aEndpoints,
                           SMPExtensionConverter.convertToString (aProcess.getExtension ()));
  }
}
