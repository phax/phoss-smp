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
package com.helger.phoss.smp.domain.servicegroup;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.type.ObjectType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.bdxr.smp1.participant.BDXR1ParticipantIdentifier;
import com.helger.peppolid.bdxr.smp2.participant.BDXR2ParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;

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
  private SGCustomPropertyList m_aCustomProperties;

  // Status member
  private final IParticipantIdentifier m_aParticipantIdentifier;

  /**
   * Create a unified participant identifier with a lower cased value, because according to the
   * Peppol policy for identifiers, the values must be treated case-sensitive.
   *
   * @param aParticipantIdentifier
   *        The original participant identifier. May not be <code>null</code>.
   * @return The new participant identifier with a lower cased value.
   */
  @NonNull
  private static IParticipantIdentifier _createUnifiedParticipantIdentifier (@NonNull final IParticipantIdentifier aParticipantIdentifier)
  {
    ValueEnforcer.notNull (aParticipantIdentifier, "ParticipantIdentifier");
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier ret = aIdentifierFactory.getClone (aParticipantIdentifier);
    if (ret == null)
      throw new IllegalStateException ("Failed to clone " +
                                       aParticipantIdentifier +
                                       " with identifier factory " +
                                       aIdentifierFactory);
    return ret;
  }

  @NonNull
  @Nonempty
  public static String createSMPServiceGroupID (@NonNull final IParticipantIdentifier aParticipantIdentifier)
  {
    return _createUnifiedParticipantIdentifier (aParticipantIdentifier).getURIEncoded ();
  }

  public SMPServiceGroup (@NonNull @Nonempty final String sOwnerID,
                          @NonNull final IParticipantIdentifier aParticipantIdentifier,
                          @Nullable final String sExtension)
  {
    this (sOwnerID, aParticipantIdentifier, sExtension, null);
  }

  public SMPServiceGroup (@NonNull @Nonempty final String sOwnerID,
                          @NonNull final IParticipantIdentifier aParticipantIdentifier,
                          @Nullable final String sExtension,
                          @Nullable final SGCustomPropertyList aCustomProperties)
  {
    m_sID = createSMPServiceGroupID (aParticipantIdentifier);
    setOwnerID (sOwnerID);
    getExtensions ().setExtensionAsString (sExtension);
    m_aCustomProperties = aCustomProperties;
    // Make a copy to avoid unwanted changes
    m_aParticipantIdentifier = _createUnifiedParticipantIdentifier (aParticipantIdentifier);
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getOwnerID ()
  {
    return m_sOwnerID;
  }

  @NonNull
  public final EChange setOwnerID (@NonNull @Nonempty final String sOwnerID)
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    if (sOwnerID.equals (m_sOwnerID))
      return EChange.UNCHANGED;
    m_sOwnerID = sOwnerID;
    return EChange.CHANGED;
  }

  @NonNull
  public IParticipantIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantIdentifier;
  }

  @Nullable
  public SGCustomPropertyList getCustomProperties ()
  {
    return m_aCustomProperties;
  }

  @NonNull
  public final EChange setCustomProperties (@Nullable final SGCustomPropertyList aCustomProperties)
  {
    if (EqualsHelper.equals (aCustomProperties, m_aCustomProperties))
      return EChange.UNCHANGED;
    m_aCustomProperties = aCustomProperties;
    return EChange.CHANGED;
  }

  public com.helger.xsds.peppol.smp1.@NonNull ServiceGroupType getAsJAXBObjectPeppol ()
  {
    final com.helger.xsds.peppol.smp1.ServiceGroupType ret = new com.helger.xsds.peppol.smp1.ServiceGroupType ();
    // Explicit constructor call is needed here!
    ret.setParticipantIdentifier (new SimpleParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (getExtensions ().getAsPeppolExtension ());
    return ret;
  }

  public com.helger.xsds.bdxr.smp1.@NonNull ServiceGroupType getAsJAXBObjectBDXR1 ()
  {
    final com.helger.xsds.bdxr.smp1.ServiceGroupType ret = new com.helger.xsds.bdxr.smp1.ServiceGroupType ();
    // Explicit constructor call is needed here!
    ret.setParticipantIdentifier (new BDXR1ParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceMetadataReferenceCollection (null);
    }
    ret.setExtension (getExtensions ().getAsBDXRExtensions ());
    return ret;
  }

  public com.helger.xsds.bdxr.smp2.@NonNull ServiceGroupType getAsJAXBObjectBDXR2 ()
  {
    final com.helger.xsds.bdxr.smp2.ServiceGroupType ret = new com.helger.xsds.bdxr.smp2.ServiceGroupType ();
    ret.setSMPExtensions (getExtensions ().getAsBDXR2Extensions ());
    ret.setSMPVersionID ("2.0");
    // Explicit constructor call is needed here!
    ret.setParticipantID (new BDXR2ParticipantIdentifier (m_aParticipantIdentifier));
    if (false)
    {
      // This is set by the REST server
      ret.setServiceReference (null);
    }
    // An eventually present Signature is not applied here
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
                            .getToString ();
  }
}
