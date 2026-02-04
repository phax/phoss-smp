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
package com.helger.phoss.smp.domain.businesscard;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.businesscard.v3.PD3APIHelper;
import com.helger.peppol.businesscard.v3.PD3BusinessCardType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;

/**
 * A single business card.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPBusinessCard implements ISMPBusinessCard
{
  public static final ObjectType OT = new ObjectType ("smpbusinesscard");

  private final String m_sID;
  private final IParticipantIdentifier m_aParticipantID;
  private final ICommonsList <SMPBusinessCardEntity> m_aEntities;

  public SMPBusinessCard (@NonNull final IParticipantIdentifier aParticipantID,
                          @NonNull final Iterable <? extends SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aEntities, "Entities");

    m_sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    m_aParticipantID = aParticipantID;
    m_aEntities = new CommonsArrayList <> (aEntities);
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  public IParticipantIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  /**
   * @return A mutable list with all {@link SMPBusinessCardEntity} objects. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <SMPBusinessCardEntity> entities ()
  {
    return m_aEntities;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <SMPBusinessCardEntity> getAllEntities ()
  {
    return m_aEntities.getClone ();
  }

  @Nullable
  public SMPBusinessCardEntity getEntityAtIndex (@Nonnegative final int nIndex)
  {
    return m_aEntities.getAtIndex (nIndex);
  }

  @Nonnegative
  public int getEntityCount ()
  {
    return m_aEntities.size ();
  }

  @NonNull
  public PD3BusinessCardType getAsJAXBObject ()
  {
    final PD3BusinessCardType ret = new PD3BusinessCardType ();
    ret.setParticipantIdentifier (PD3APIHelper.createIdentifier (m_aParticipantID.getScheme (),
                                                                 m_aParticipantID.getValue ()));
    for (final SMPBusinessCardEntity aItem : m_aEntities)
      ret.addBusinessEntity (aItem.getAsJAXBObject ());
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SMPBusinessCard rhs = (SMPBusinessCard) o;
    return m_sID.equals (rhs.m_sID) && m_aEntities.equals (rhs.m_aEntities);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).append (m_aEntities).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantID", m_aParticipantID)
                                       .append ("Entities", m_aEntities)
                                       .getToString ();
  }
}
