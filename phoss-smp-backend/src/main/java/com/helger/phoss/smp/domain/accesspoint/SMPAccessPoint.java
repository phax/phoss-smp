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
package com.helger.phoss.smp.domain.accesspoint;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;

/**
 * Default implementation of the {@link ISMPAccessPoint} interface.
 * <p>
 * Instances of this class are immutable. Changing the URL or the certificate of an endpoint
 * therefore never modifies an existing Access Point (which may be shared by many endpoints), but
 * always results in a new Access Point object that is de-duplicated by the backend upon saving.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
@Immutable
public class SMPAccessPoint implements ISMPAccessPoint
{
  public static final ObjectType OT = new ObjectType ("smpaccesspoint");

  private final String m_sID;
  private final String m_sEndpointReference;
  private final String m_sCertificate;

  public SMPAccessPoint (@NonNull @Nonempty final String sID,
                         @Nullable final String sEndpointReference,
                         @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    m_sID = sID;
    m_sEndpointReference = sEndpointReference;
    m_sCertificate = sCertificate;
  }

  /**
   * Create a new Access Point with a newly created unique ID. Such an object is "detached", meaning
   * it is not necessarily contained in the {@link ISMPAccessPointManager}. It is the responsibility
   * of the respective backend to resolve it to a managed Access Point upon saving.
   *
   * @param sEndpointReference
   *        The endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The certificate. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static SMPAccessPoint createDetached (@Nullable final String sEndpointReference,
                                               @Nullable final String sCertificate)
  {
    return new SMPAccessPoint (SMPAccessPointHelper.createUniqueAccessPointID (), sEndpointReference, sCertificate);
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public String getEndpointReference ()
  {
    return m_sEndpointReference;
  }

  @Nullable
  public String getCertificate ()
  {
    return m_sCertificate;
  }

  /**
   * @param sEndpointReference
   *        The new endpoint reference URL. May be <code>null</code>.
   * @return A new detached Access Point with the provided endpoint reference and the certificate of
   *         this object. Never <code>null</code>.
   */
  @NonNull
  public SMPAccessPoint withEndpointReference (@Nullable final String sEndpointReference)
  {
    if (hasSameContent (sEndpointReference, m_sCertificate))
      return this;
    return createDetached (sEndpointReference, m_sCertificate);
  }

  /**
   * @param sCertificate
   *        The new certificate. May be <code>null</code>.
   * @return A new detached Access Point with the provided certificate and the endpoint reference of
   *         this object. Never <code>null</code>.
   */
  @NonNull
  public SMPAccessPoint withCertificate (@Nullable final String sCertificate)
  {
    if (hasSameContent (m_sEndpointReference, sCertificate))
      return this;
    return createDetached (m_sEndpointReference, sCertificate);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPAccessPoint rhs = (SMPAccessPoint) o;
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
                                       .append ("EndpointReference", m_sEndpointReference)
                                       .append ("Certificate", m_sCertificate)
                                       .getToString ();
  }
}
