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
package com.helger.peppol.smpserver.domain.servicegroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smpserver.domain.SMPHelper;

/**
 * This class represents a single service group.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPServiceGroup implements ISMPServiceGroup
{
  public static final ObjectType OT = new ObjectType ("smpservicegroup");

  private final String m_sID;
  private String m_sOwnerID;
  private String m_sExtension;

  // Status member
  private final SimpleParticipantIdentifier m_aParticipantIdentifier;

  public SMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                          @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                          @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aParticipantIdentifier, "ParticipantIdentifier");
    m_sID = SMPHelper.createSMPServiceGroupID (aParticipantIdentifier);
    setOwnerID (sOwnerID);
    setExtension (sExtension);
    // Make a copy to avoid unwanted changes
    m_aParticipantIdentifier = new SimpleParticipantIdentifier (aParticipantIdentifier);
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getOwnerID ()
  {
    return m_sOwnerID;
  }

  @Nonnull
  public EChange setOwnerID (@Nonnull @Nonempty final String sOwnerID)
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    if (sOwnerID.equals (m_sOwnerID))
      return EChange.UNCHANGED;
    m_sOwnerID = sOwnerID;
    return EChange.CHANGED;
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

  @Nonnull
  public EChange setExtension (@Nullable final String sExtension)
  {
    if (EqualsHelper.equals (sExtension, m_sExtension))
      return EChange.UNCHANGED;
    m_sExtension = sExtension;
    return EChange.CHANGED;
  }

  @Nonnull
  public IPeppolParticipantIdentifier getParticpantIdentifier ()
  {
    return m_aParticipantIdentifier;
  }

  @Nonnull
  public ServiceGroupType getAsJAXBObject ()
  {
    final ServiceGroupType ret = new ServiceGroupType ();
    ret.setParticipantIdentifier (m_aParticipantIdentifier.getClone ());
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
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
    final SMPServiceGroup rhs = (SMPServiceGroup) o;
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
                                       .append ("OwnerID", m_sOwnerID)
                                       .appendIfNotEmpty ("Extension", m_sExtension)
                                       .append ("ParticipantIdentifier", m_aParticipantIdentifier)
                                       .toString ();
  }
}
