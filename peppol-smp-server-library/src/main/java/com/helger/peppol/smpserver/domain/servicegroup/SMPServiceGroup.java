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
import com.helger.peppol.bdxr.BDXRExtensionConverter;
import com.helger.peppol.bdxr.BDXRHelper;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smp.SMPExtensionConverter;

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
    m_sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
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
  public com.helger.peppol.smp.ServiceGroupType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.ServiceGroupType ret = new com.helger.peppol.smp.ServiceGroupType ();
    ret.setParticipantIdentifier (m_aParticipantIdentifier.getClone ());
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (SMPExtensionConverter.convertOrNull (m_sExtension));
    return ret;
  }

  @Nonnull
  public com.helger.peppol.bdxr.ServiceGroupType getAsJAXBObjectBDXR ()
  {
    final com.helger.peppol.bdxr.ServiceGroupType ret = new com.helger.peppol.bdxr.ServiceGroupType ();
    ret.setParticipantIdentifier (BDXRHelper.getAsBDXRParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (BDXRExtensionConverter.convertOrNull (m_sExtension));
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

  @Nonnull
  @Nonempty
  public static String createSMPServiceGroupID (@Nonnull final IParticipantIdentifier aParticipantIdentifier)
  {
    return IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier);
  }
}
