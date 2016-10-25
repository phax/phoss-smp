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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.identifier.bdxr.participant.BDXRParticipantIdentifier;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.extension.AbstractSMPHasExtension;

/**
 * This class represents a single service group.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPServiceGroup extends AbstractSMPHasExtension implements ISMPServiceGroup
{
  public static final ObjectType OT = new ObjectType ("smpservicegroup");

  private final String m_sID;
  private String m_sOwnerID;

  // Status member
  private final IParticipantIdentifier m_aParticipantIdentifier;

  /**
   * Create a unified participant identifier with a lower cased value, because
   * according to the PEPPOL policy for identifiers, the values must be treated
   * case-sensitive.
   *
   * @param sValue
   *        The original participant identifier value. May not be
   *        <code>null</code>.
   * @return The new participant identifier value with a lower cased value.
   */
  @Nonnull
  public static String createUnifiedParticipantIdentifierValue (@Nonnull final String sValue)
  {
    ValueEnforcer.notNull (sValue, "Value");
    return sValue.toLowerCase (Locale.US);
  }

  /**
   * Create a unified participant identifier with a lower cased value, because
   * according to the PEPPOL policy for identifiers, the values must be treated
   * case-sensitive.
   *
   * @param aParticipantIdentifier
   *        The original participant identifier. May not be <code>null</code>.
   * @return The new participant identifier with a lower cased value.
   */
  @Nonnull
  public static IParticipantIdentifier createUnifiedParticipantIdentifier (@Nonnull final IParticipantIdentifier aParticipantIdentifier)
  {
    ValueEnforcer.notNull (aParticipantIdentifier, "ParticipantIdentifier");
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    return aIdentifierFactory.createParticipantIdentifier (aParticipantIdentifier.getScheme (),
                                                           createUnifiedParticipantIdentifierValue (aParticipantIdentifier.getValue ()));
  }

  public SMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                          @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                          @Nullable final String sExtension)
  {
    m_sID = SMPServiceGroup.createSMPServiceGroupID (aParticipantIdentifier);
    setOwnerID (sOwnerID);
    setExtensionAsString (sExtension);
    // Make a copy to avoid unwanted changes
    m_aParticipantIdentifier = createUnifiedParticipantIdentifier (aParticipantIdentifier);
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

  @Nonnull
  public IParticipantIdentifier getParticpantIdentifier ()
  {
    return m_aParticipantIdentifier;
  }

  @Nonnull
  public com.helger.peppol.smp.ServiceGroupType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.ServiceGroupType ret = new com.helger.peppol.smp.ServiceGroupType ();
    // Explicit constructor call is needed here!
    ret.setParticipantIdentifier (new SimpleParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (getAsPeppolExtension ());
    return ret;
  }

  @Nonnull
  public com.helger.peppol.bdxr.ServiceGroupType getAsJAXBObjectBDXR ()
  {
    final com.helger.peppol.bdxr.ServiceGroupType ret = new com.helger.peppol.bdxr.ServiceGroupType ();
    // Explicit constructor call is needed here!
    ret.setParticipantIdentifier (new BDXRParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (getAsBDXRExtension ());
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
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("ID", m_sID)
                            .append ("OwnerID", m_sOwnerID)
                            .append ("ParticipantIdentifier", m_aParticipantIdentifier)
                            .toString ();
  }

  @Nonnull
  @Nonempty
  public static String createSMPServiceGroupID (@Nonnull final IParticipantIdentifier aParticipantIdentifier)
  {
    return createUnifiedParticipantIdentifier (aParticipantIdentifier).getURIEncoded ();
  }
}
