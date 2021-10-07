/*
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.redirect;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;

/**
 * Default implementation of the {@link ISMPRedirect} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPRedirect extends AbstractSMPHasExtension implements ISMPRedirect
{
  public static final ObjectType OT = new ObjectType ("smpredirect");

  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private IDocumentTypeIdentifier m_aDocumentTypeIdentifier;
  private String m_sTargetHref;
  private String m_sSubjectUniqueIdentifier;
  private X509Certificate m_aCertificate;

  public SMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                      @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                      @Nonnull @Nonempty final String sTargetHref,
                      @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                      @Nullable final X509Certificate aCertificate,
                      @Nullable final String sExtension)
  {
    m_aServiceGroup = ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    setDocumentTypeIdentifier (aDocumentTypeIdentifier);
    setTargetHref (sTargetHref);
    setSubjectUniqueIdentifier (sSubjectUniqueIdentifier);
    setCertificate (aCertificate);
    setExtensionAsString (sExtension);
    m_sID = aServiceGroup.getID () + "-" + aDocumentTypeIdentifier.getURIEncoded ();
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

  @Nonnull
  public final IDocumentTypeIdentifier getDocumentTypeIdentifier ()
  {
    return m_aDocumentTypeIdentifier;
  }

  public final void setDocumentTypeIdentifier (@Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");
    m_aDocumentTypeIdentifier = aDocumentTypeIdentifier;
  }

  @Nonnull
  @Nonempty
  public final String getTargetHref ()
  {
    return m_sTargetHref;
  }

  public final void setTargetHref (@Nonnull @Nonempty final String sTargetHref)
  {
    ValueEnforcer.notEmpty (sTargetHref, "TargetHref");
    m_sTargetHref = sTargetHref;
  }

  @Nonnull
  @Nonempty
  public final String getSubjectUniqueIdentifier ()
  {
    return m_sSubjectUniqueIdentifier;
  }

  public final void setSubjectUniqueIdentifier (@Nonnull @Nonempty final String sSubjectUniqueIdentifier)
  {
    ValueEnforcer.notEmpty (sSubjectUniqueIdentifier, "SubjectUniqueIdentifier");
    m_sSubjectUniqueIdentifier = sSubjectUniqueIdentifier;
  }

  @Nullable
  public final X509Certificate getCertificate ()
  {
    return m_aCertificate;
  }

  public final void setCertificate (@Nullable final X509Certificate aCertificate)
  {
    m_aCertificate = aCertificate;
  }

  @Nonnull
  public com.helger.xsds.peppol.smp1.ServiceMetadataType getAsJAXBObjectPeppol ()
  {
    final com.helger.xsds.peppol.smp1.RedirectType aRedirect = new com.helger.xsds.peppol.smp1.RedirectType ();
    aRedirect.setHref (m_sTargetHref);
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (getAsPeppolExtension ());
    // Certificate is not used here

    final com.helger.xsds.peppol.smp1.ServiceMetadataType ret = new com.helger.xsds.peppol.smp1.ServiceMetadataType ();
    ret.setRedirect (aRedirect);
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp1.ServiceMetadataType getAsJAXBObjectBDXR1 ()
  {
    final com.helger.xsds.bdxr.smp1.RedirectType aRedirect = new com.helger.xsds.bdxr.smp1.RedirectType ();
    aRedirect.setHref (m_sTargetHref);
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (getAsBDXRExtension ());
    // Certificate is not used here

    final com.helger.xsds.bdxr.smp1.ServiceMetadataType ret = new com.helger.xsds.bdxr.smp1.ServiceMetadataType ();
    ret.setRedirect (aRedirect);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final SMPRedirect rhs = (SMPRedirect) o;
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
                            .append ("ServiceGroup", m_aServiceGroup)
                            .append ("DocumentTypeIdentifier", m_aDocumentTypeIdentifier)
                            .append ("TargetHref", m_sTargetHref)
                            .append ("SubjectUniqueIdentifier", m_sSubjectUniqueIdentifier)
                            .append ("Certificate", m_aCertificate)
                            .getToString ();
  }
}
