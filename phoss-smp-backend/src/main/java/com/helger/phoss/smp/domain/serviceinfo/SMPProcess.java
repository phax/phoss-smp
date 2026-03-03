/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.bdxr.smp1.process.BDXR1ProcessIdentifier;
import com.helger.peppolid.bdxr.smp2.process.BDXR2ProcessIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;

/**
 * Default implementation of the {@link ISMPProcess} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPProcess extends AbstractSMPHasExtension implements ISMPProcess
{
  private IProcessIdentifier m_aProcessIdentifier;
  private final ICommonsOrderedMap <String, ICommonsList <SMPEndpoint>> m_aEndpoints = new CommonsLinkedHashMap <> ();

  public SMPProcess (@NonNull final IProcessIdentifier aProcessIdentifier,
                     @Nullable final List <SMPEndpoint> aEndpoints,
                     @Nullable final String sExtension)
  {
    setProcessIdentifier (aProcessIdentifier);
    if (aEndpoints != null)
      addEndpoints (aEndpoints);
    getExtensions ().setExtensionAsString (sExtension);
  }

  @NonNull
  public final IProcessIdentifier getProcessIdentifier ()
  {
    return m_aProcessIdentifier;
  }

  public final void setProcessIdentifier (@NonNull final IProcessIdentifier aProcessIdentifier)
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
  @Deprecated (forRemoval = true, since = "8.1.2")
  public SMPEndpoint getEndpointOfTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.isEmpty (sTransportProfileID))
      return null;
    final ICommonsList <SMPEndpoint> aEPs = m_aEndpoints.get (sTransportProfileID);
    if (aEPs == null)
      return null;
    return aEPs.getFirstOrNull ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllEndpointsOfTransportProfile (@Nullable final String sTransportProfileID)
  {
    if (StringHelper.isEmpty (sTransportProfileID))
      return null;
    final ICommonsList <SMPEndpoint> aEPs = m_aEndpoints.get (sTransportProfileID);
    return new CommonsArrayList <> (aEPs);
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPEndpoint> getAllEndpoints ()
  {
    final ICommonsList <ISMPEndpoint> ret = new CommonsArrayList <> ();
    for (final var aList : m_aEndpoints.values ())
      ret.addAll (aList);
    return ret;
  }

  @Nullable
  public SMPEndpoint getEndpointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;
    for (final var aList : m_aEndpoints.values ())
      for (final SMPEndpoint aEndpoint : aList)
        if (aEndpoint.getID ().equals (sID))
          return aEndpoint;
    return null;
  }

  public boolean containsAnyEndpointWithTransportProfile (@Nullable final String sTransportProfileID)
  {
    return StringHelper.isNotEmpty (sTransportProfileID) && m_aEndpoints.containsKey (sTransportProfileID);
  }

  public final void addEndpoint (@NonNull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    m_aEndpoints.computeIfAbsent (sTransportProfile, k -> new CommonsArrayList <> ()).add (aEndpoint);
  }

  public final void addEndpoints (@NonNull final Iterable <? extends SMPEndpoint> aEndpoints)
  {
    ValueEnforcer.notNull (aEndpoints, "Endpoints");
    for (final SMPEndpoint aEndpoint : aEndpoints)
      addEndpoint (aEndpoint);
  }

  public final void createOrUpdateEndpoint (@NonNull final SMPEndpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    final String sTransportProfile = aEndpoint.getTransportProfile ();
    final ICommonsList <SMPEndpoint> aList = m_aEndpoints.computeIfAbsent (sTransportProfile,
                                                                           k -> new CommonsArrayList <> ());
    // Replace by ID if an endpoint with the same ID exists
    final String sID = aEndpoint.getID ();
    boolean bReplaced = false;
    for (int i = 0; i < aList.size (); i++)
    {
      if (aList.get (i).getID ().equals (sID))
      {
        aList.set (i, aEndpoint);
        bReplaced = true;
        break;
      }
    }
    if (!bReplaced)
      aList.add (aEndpoint);
  }

  @NonNull
  public EChange deleteAllEndpoints (@Nullable final String sTransportProfile)
  {
    if (StringHelper.isEmpty (sTransportProfile))
      return EChange.UNCHANGED;
    return EChange.valueOf (m_aEndpoints.remove (sTransportProfile) != null);
  }

  @NonNull
  public EChange deleteEndpointByID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;
    for (final var aEntry : m_aEndpoints.entrySet ())
    {
      final ICommonsList <SMPEndpoint> aList = aEntry.getValue ();
      if (aList.removeIf (x -> x.getID ().equals (sID)))
      {
        if (aList.isEmpty ())
          m_aEndpoints.remove (aEntry.getKey ());
        return EChange.CHANGED;
      }
    }
    return EChange.UNCHANGED;
  }

  public com.helger.xsds.peppol.smp1.@Nullable ProcessType getAsJAXBObjectPeppol ()
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
    for (final var aEndpoints : m_aEndpoints.values ())
      for (final var aEndpoint : aEndpoints)
        aEndpointList.addEndpoint (aEndpoint.getAsJAXBObjectPeppol ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (getExtensions ().getAsPeppolExtension ());
    return ret;
  }

  public com.helger.xsds.bdxr.smp1.@Nullable ProcessType getAsJAXBObjectBDXR1 ()
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
    for (final var aEndpoints : m_aEndpoints.values ())
      for (final var aEndpoint : aEndpoints)
        aEndpointList.addEndpoint (aEndpoint.getAsJAXBObjectBDXR1 ());
    ret.setServiceEndpointList (aEndpointList);
    ret.setExtension (getExtensions ().getAsBDXRExtensions ());
    return ret;
  }

  public com.helger.xsds.bdxr.smp2.ac.@Nullable ProcessMetadataType getAsJAXBObjectBDXR2 ()
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

    for (final var aEndpoints : m_aEndpoints.values ())
      for (final var aEndpoint : aEndpoints)
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
}
