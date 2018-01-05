/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.redirect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.smpserver.domain.extension.AbstractSMPHasExtension;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

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

  public SMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                      @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                      @Nonnull @Nonempty final String sTargetHref,
                      @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                      @Nullable final String sExtension)
  {
    m_aServiceGroup = ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    setDocumentTypeIdentifier (aDocumentTypeIdentifier);
    setTargetHref (sTargetHref);
    setSubjectUniqueIdentifier (sSubjectUniqueIdentifier);
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
  public IDocumentTypeIdentifier getDocumentTypeIdentifier ()
  {
    return m_aDocumentTypeIdentifier;
  }

  public void setDocumentTypeIdentifier (@Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");
    m_aDocumentTypeIdentifier = aDocumentTypeIdentifier;
  }

  @Nonnull
  @Nonempty
  public String getTargetHref ()
  {
    return m_sTargetHref;
  }

  public void setTargetHref (@Nonnull @Nonempty final String sTargetHref)
  {
    ValueEnforcer.notEmpty (sTargetHref, "TargetHref");
    m_sTargetHref = sTargetHref;
  }

  @Nonnull
  @Nonempty
  public String getSubjectUniqueIdentifier ()
  {
    return m_sSubjectUniqueIdentifier;
  }

  public void setSubjectUniqueIdentifier (@Nonnull @Nonempty final String sSubjectUniqueIdentifier)
  {
    ValueEnforcer.notEmpty (sSubjectUniqueIdentifier, "SubjectUniqueIdentifier");
    m_sSubjectUniqueIdentifier = sSubjectUniqueIdentifier;
  }

  @Nonnull
  public com.helger.peppol.smp.ServiceMetadataType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.RedirectType aRedirect = new com.helger.peppol.smp.RedirectType ();
    aRedirect.setHref (m_sTargetHref);
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (getAsPeppolExtension ());

    final com.helger.peppol.smp.ServiceMetadataType ret = new com.helger.peppol.smp.ServiceMetadataType ();
    ret.setRedirect (aRedirect);
    return ret;
  }

  @Nonnull
  public com.helger.peppol.bdxr.ServiceMetadataType getAsJAXBObjectBDXR ()
  {
    final com.helger.peppol.bdxr.RedirectType aRedirect = new com.helger.peppol.bdxr.RedirectType ();
    aRedirect.setHref (m_sTargetHref);
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (getAsBDXRExtension ());

    final com.helger.peppol.bdxr.ServiceMetadataType ret = new com.helger.peppol.bdxr.ServiceMetadataType ();
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
                            .getToString ();
  }
}
