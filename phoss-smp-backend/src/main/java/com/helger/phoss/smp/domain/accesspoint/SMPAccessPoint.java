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
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;

/**
 * Default implementation of the {@link ISMPAccessPoint} interface.
 * <p>
 * An Access Point is identified by its unique name. All attributes may only be changed via the
 * {@link ISMPAccessPointManager} - because a single Access Point object is shared by all endpoints
 * referencing it, such a change is immediately effective for all of them, which is exactly the
 * purpose of the Access Point relation.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
@NotThreadSafe
public class SMPAccessPoint implements ISMPAccessPoint
{
  public static final ObjectType OT = new ObjectType ("smpaccesspoint");

  private final String m_sID;
  private String m_sName;
  private String m_sEndpointReference;
  private String m_sCertificate;

  public SMPAccessPoint (@NonNull @Nonempty final String sID,
                         @NonNull @Nonempty final String sName,
                         @Nullable final String sEndpointReference,
                         @Nullable final String sCertificate)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notEmpty (sName, "Name");
    m_sID = sID;
    m_sName = sName;
    m_sEndpointReference = sEndpointReference;
    m_sCertificate = sCertificate;
  }

  /**
   * Create a new Access Point with a newly created unique ID.
   *
   * @param sName
   *        The unique name of the Access Point. May neither be <code>null</code> nor empty.
   * @param sEndpointReference
   *        The endpoint reference URL. May be <code>null</code>.
   * @param sCertificate
   *        The certificate. May be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static SMPAccessPoint createWithNewID (@NonNull @Nonempty final String sName,
                                                @Nullable final String sEndpointReference,
                                                @Nullable final String sCertificate)
  {
    return new SMPAccessPoint (SMPAccessPointHelper.createUniqueAccessPointID (),
                               sName,
                               sEndpointReference,
                               sCertificate);
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  /**
   * Set the name of this Access Point. The caller must ensure that no other Access Point with that
   * name exists, so this method may only be called by an {@link ISMPAccessPointManager}.
   *
   * @param sName
   *        The new name. May neither be <code>null</code> nor empty.
   * @return {@link EChange#CHANGED} if the value was changed.
   */
  @NonNull
  public EChange setName (@NonNull @Nonempty final String sName)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    if (sName.equals (m_sName))
      return EChange.UNCHANGED;
    m_sName = sName;
    return EChange.CHANGED;
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
   * Set the endpoint reference URL of this Access Point. Because a single Access Point object is
   * shared by all endpoints referencing it, this change is immediately effective for all of them.
   * This method may only be called by an {@link ISMPAccessPointManager}.
   *
   * @param sEndpointReference
   *        The new endpoint reference URL. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the value was changed.
   */
  @NonNull
  public EChange setEndpointReference (@Nullable final String sEndpointReference)
  {
    if (hasSameEndpointReference (sEndpointReference))
      return EChange.UNCHANGED;
    m_sEndpointReference = sEndpointReference;
    return EChange.CHANGED;
  }

  /**
   * Set the certificate of this Access Point. Because a single Access Point object is shared by all
   * endpoints referencing it, this change is immediately effective for all of them. This method may
   * only be called by an {@link ISMPAccessPointManager}.
   *
   * @param sCertificate
   *        The new certificate. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the value was changed.
   */
  @NonNull
  public EChange setCertificate (@Nullable final String sCertificate)
  {
    if (hasSameCertificate (sCertificate))
      return EChange.UNCHANGED;
    m_sCertificate = sCertificate;
    return EChange.CHANGED;
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
                                       .append ("Name", m_sName)
                                       .append ("EndpointReference", m_sEndpointReference)
                                       .append ("Certificate", m_sCertificate)
                                       .getToString ();
  }
}
