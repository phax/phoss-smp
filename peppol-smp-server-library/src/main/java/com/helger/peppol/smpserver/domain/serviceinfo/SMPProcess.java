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
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.identifier.IProcessIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smp.ServiceEndpointList;

/**
 * Default implementation of the {@link ISMPProcess} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPProcess implements ISMPProcess
{
  private SimpleProcessIdentifier m_aProcessIdentifier;
  private final Map <String, SMPEndpoint> m_aEndpoints = new LinkedHashMap <> ();
  private String m_sExtension;

  public SMPProcess (@Nonnull final IProcessIdentifier aProcessIdentifier,
                     @Nonnull final List <SMPEndpoint> aEndpoints,
                     @Nullable final String sExtension)
  {
    setProcessIdentifier (aProcessIdentifier);
    ValueEnforcer.notEmptyNoNullValue (aEndpoints, "Endpoints");
    for (final SMPEndpoint aEndpoint : aEndpoints)
      addEndpoint (aEndpoint);
    setExtension (sExtension);
  }

  @Nonnull
  public IPeppolProcessIdentifier getProcessIdentifier ()
  {
    return m_aProcessIdentifier;
  }

  public void setProcessIdentifier (@Nonnull final IProcessIdentifier aProcessIdentifier)
  {
    ValueEnforcer.notNull (aProcessIdentifier, "ProcessIdentifier");
    // Make a copy to avoid unavoided changes
    m_aProcessIdentifier = new SimpleProcessIdentifier (aProcessIdentifier);
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
  public List <SMPEndpoint> getAllEndpoints ()
  {
    return CollectionHelper.newList (m_aEndpoints.values ());
  }

  public void addEndpoint (@Nonnull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    if (m_aEndpoints.containsKey (sTransportProfile))
      throw new IllegalStateException ("No such key present: '" + sTransportProfile + "'");
    m_aEndpoints.put (sTransportProfile, aEndpoint);
  }

  public void setEndpoint (@Nonnull @Nonempty final String sTransportProfile, @Nonnull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notEmpty (sTransportProfile, "TransportProfile");
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    if (!m_aEndpoints.containsKey (sTransportProfile))
      throw new IllegalStateException ("No such key present: '" + sTransportProfile + "'");
    m_aEndpoints.put (sTransportProfile, aEndpoint);
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

  @Nonnull
  public ProcessType getAsJAXBObject ()
  {
    final ProcessType ret = new ProcessType ();
    ret.setProcessIdentifier (new SimpleProcessIdentifier (m_aProcessIdentifier));
    final ServiceEndpointList aEndpointList = new ServiceEndpointList ();
    for (final ISMPEndpoint aEndpoint : m_aEndpoints.values ())
      aEndpointList.addEndpoint (aEndpoint.getAsJAXBObject ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (SMPExtensionConverter.convertOrNull (m_sExtension));
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final SMPProcess rhs = (SMPProcess) o;
    return EqualsHelper.equals (m_aProcessIdentifier, rhs.m_aProcessIdentifier) &&
           EqualsHelper.equals (m_aEndpoints, rhs.m_aEndpoints) &&
           EqualsHelper.equals (m_sExtension, rhs.m_sExtension);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aProcessIdentifier)
                                       .append (m_aEndpoints)
                                       .append (m_sExtension)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ProcessIdentifier", m_aProcessIdentifier)
                                       .append ("Endpoints", m_aEndpoints)
                                       .append ("extension", m_sExtension)
                                       .toString ();
  }

  @Nonnull
  public static SMPProcess createFromJAXB (@Nonnull final ProcessType aProcess)
  {
    final List <SMPEndpoint> aEndpoints = new ArrayList <SMPEndpoint> ();
    for (final EndpointType aEndpoint : aProcess.getServiceEndpointList ().getEndpoint ())
      aEndpoints.add (SMPEndpoint.createFromJAXB (aEndpoint));
    return new SMPProcess (aProcess.getProcessIdentifier (),
                           aEndpoints,
                           SMPExtensionConverter.convertToString (aProcess.getExtension ()));
  }
}
