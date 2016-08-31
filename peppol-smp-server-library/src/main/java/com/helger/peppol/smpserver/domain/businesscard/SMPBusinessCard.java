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

package com.helger.peppol.smpserver.domain.businesscard;

import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * A single business card.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPBusinessCard implements ISMPBusinessCard
{
  public static final ObjectType OT = new ObjectType ("smpbusinesscard");

  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private final ICommonsList <SMPBusinessCardEntity> m_aEntities;

  public SMPBusinessCard (@Nonnull final ISMPServiceGroup aServiceGroup,
                          @Nonnull final List <SMPBusinessCardEntity> aEntities)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aEntities, "Entities");

    m_aServiceGroup = aServiceGroup;
    m_sID = m_aServiceGroup.getID ();
    m_aEntities = new CommonsArrayList<> (aEntities);
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

  /**
   * @return A mutable list with all {@link SMPBusinessCardEntity} objects.
   *         Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("design")
  public ICommonsList <SMPBusinessCardEntity> directGetEntities ()
  {
    return m_aEntities;
  }

  /**
   * @return A mutable list with all {@link SMPBusinessCardEntity} objects.
   *         Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <SMPBusinessCardEntity> getAllEntities ()
  {
    return m_aEntities.getClone ();
  }

  @Nonnegative
  public int getEntityCount ()
  {
    return m_aEntities.size ();
  }

  @Nonnull
  public PDBusinessCardType getAsJAXBObject ()
  {
    final PDBusinessCardType ret = new PDBusinessCardType ();
    ret.setParticipantIdentifier (SMPBusinessCardIdentifier.getAsJAXBObject (m_aServiceGroup.getParticpantIdentifier ()
                                                                                            .getScheme (),
                                                                             m_aServiceGroup.getParticpantIdentifier ()
                                                                                            .getValue ()));
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
    return m_aServiceGroup.equals (rhs.m_aServiceGroup) && m_aEntities.equals (rhs.m_aEntities);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aServiceGroup).append (m_aEntities).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("serviceGroup", m_aServiceGroup)
                                       .append ("entities", m_aEntities)
                                       .toString ();
  }
}
