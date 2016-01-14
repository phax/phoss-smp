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
package com.helger.peppol.smpserver.domain.redirect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.peppol.bdxr.BDXRExtensionConverter;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Default implementation of the {@link ISMPRedirect} interface.
 *
 * @author Philip Helger
 */
public class SMPRedirect implements ISMPRedirect
{
  public static final ObjectType OT = new ObjectType ("smpredirect");

  private final String m_sID;
  private final ISMPServiceGroup m_aServiceGroup;
  private SimpleDocumentTypeIdentifier m_aDocumentTypeIdentifier;
  private String m_sTargetHref;
  private String m_sSubjectUniqueIdentifier;
  private String m_sExtension;

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
    setExtension (sExtension);
    m_sID = aServiceGroup.getID () + "-" + IdentifierHelper.getIdentifierURIEncoded (aDocumentTypeIdentifier);
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
  public IPeppolDocumentTypeIdentifier getDocumentTypeIdentifier ()
  {
    return m_aDocumentTypeIdentifier;
  }

  public void setDocumentTypeIdentifier (@Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");
    // Make a copy to avoid external changes
    m_aDocumentTypeIdentifier = new SimpleDocumentTypeIdentifier (aDocumentTypeIdentifier);
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

  public boolean hasExtension ()
  {
    return StringHelper.hasText (m_sExtension);
  }

  @Nullable
  public String getExtension ()
  {
    return m_sExtension;
  }

  public void setExtension (@Nullable final String sExtension)
  {
    m_sExtension = sExtension;
  }

  @Nonnull
  public com.helger.peppol.smp.ServiceMetadataType getAsJAXBObjectPeppol ()
  {
    final com.helger.peppol.smp.RedirectType aRedirect = new com.helger.peppol.smp.RedirectType ();
    aRedirect.setHref (m_sTargetHref);
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (SMPExtensionConverter.convertOrNull (m_sExtension));

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
    aRedirect.setExtension (BDXRExtensionConverter.convertOrNull (m_sExtension));

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
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("ServiceGroup", m_aServiceGroup)
                                       .append ("DocumentTypeIdentifier", m_aDocumentTypeIdentifier)
                                       .append ("TargetHref", m_sTargetHref)
                                       .append ("SubjectUniqueIdentifier", m_sSubjectUniqueIdentifier)
                                       .appendIfNotEmpty ("Extension", m_sExtension)
                                       .toString ();
  }
}
