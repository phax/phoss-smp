/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;
import com.helger.commons.url.IURLProtocol;
import com.helger.commons.url.URLHelper;
import com.helger.commons.url.URLProtocolRegistry;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.bdxr.smp2.doctype.BDXR2DocumentTypeIdentifier;
import com.helger.peppolid.bdxr.smp2.participant.BDXR2ParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xsds.bdxr.smp2.bc.ContentBinaryObjectType;

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
  private final IParticipantIdentifier m_aParticipantID;
  private final String m_sServiceGroupID;
  private IDocumentTypeIdentifier m_aDocumentTypeIdentifier;
  private String m_sTargetHref;
  private String m_sSubjectUniqueIdentifier;
  private X509Certificate m_aCertificate;

  public SMPRedirect (@Nonnull final IParticipantIdentifier aParticipantID,
                      @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                      @Nonnull @Nonempty final String sTargetHref,
                      @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                      @Nullable final X509Certificate aCertificate,
                      @Nullable final String sExtension)
  {
    m_aParticipantID = ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    m_sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    setDocumentTypeIdentifier (aDocumentTypeIdentifier);
    setTargetHref (sTargetHref);
    setSubjectUniqueIdentifier (sSubjectUniqueIdentifier);
    setCertificate (aCertificate);
    getExtensions ().setExtensionAsString (sExtension);
    m_sID = m_sServiceGroupID + "-" + aDocumentTypeIdentifier.getURIEncoded ();
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public IParticipantIdentifier getServiceGroupParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  @Nonnull
  @Nonempty
  public String getServiceGroupID ()
  {
    return m_sServiceGroupID;
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

  @VisibleForTesting
  static String getPercentEncodedURL (@Nullable final String sURL)
  {
    if (sURL == null)
      return null;

    final IURLProtocol eProtocol = URLProtocolRegistry.getInstance ().getProtocol (sURL);
    if (eProtocol != null)
    {
      // The base URL may contain "#" which is not an anchor
      // That's why this weird parsing is needed
      String sPathRest = sURL.substring (eProtocol.getProtocol ().length ());
      final int nParamIdx = sPathRest.indexOf ('?');
      String sParams;
      if (nParamIdx >= 0)
      {
        sParams = sPathRest.substring (nParamIdx);
        sPathRest = sPathRest.substring (0, nParamIdx);
      }
      else
        sParams = "";

      // Take the path and URL encode each path part separately
      final StringBuilder aSB = new StringBuilder (sPathRest.length () * 2);
      final boolean bEndsWithSlash = StringHelper.endsWith (sPathRest, '/');

      boolean bFirst = true;
      for (final String sOriginalPathPart : StringHelper.getExplodedArray ('/', sPathRest))
        if (bFirst)
        {
          // Host (with or without part) - never URL escape
          aSB.append (sOriginalPathPart).append ('/');
          bFirst = false;
        }
        else
          if (sOriginalPathPart.length () > 0)
          {
            // First decode, to ensure it is not double encoded
            final String sDecoded = URLHelper.urlDecodeOrNull (sOriginalPathPart);
            // If decoding failed, assume it to be not encoded
            final String sEncoded = URLHelper.urlEncode (sDecoded != null ? sDecoded : sOriginalPathPart);
            aSB.append (sEncoded).append ('/');
          }
      if (!bEndsWithSlash)
      {
        // Remove trailing slash to remain consistent with original URL
        aSB.setLength (aSB.length () - 1);
      }

      return eProtocol.getProtocol () + aSB.toString () + sParams;
    }

    // Fallback: use original URL
    return sURL;
  }

  @Nonnull
  public com.helger.xsds.peppol.smp1.ServiceMetadataType getAsJAXBObjectPeppol ()
  {
    final com.helger.xsds.peppol.smp1.RedirectType aRedirect = new com.helger.xsds.peppol.smp1.RedirectType ();
    aRedirect.setHref (getPercentEncodedURL (m_sTargetHref));
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (getExtensions ().getAsPeppolExtension ());
    // Certificate is not used here

    final com.helger.xsds.peppol.smp1.ServiceMetadataType ret = new com.helger.xsds.peppol.smp1.ServiceMetadataType ();
    ret.setRedirect (aRedirect);
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp1.ServiceMetadataType getAsJAXBObjectBDXR1 ()
  {
    final com.helger.xsds.bdxr.smp1.RedirectType aRedirect = new com.helger.xsds.bdxr.smp1.RedirectType ();
    aRedirect.setHref (getPercentEncodedURL (m_sTargetHref));
    aRedirect.setCertificateUID (m_sSubjectUniqueIdentifier);
    aRedirect.setExtension (getExtensions ().getAsBDXRExtensions ());
    // Certificate is not used here

    final com.helger.xsds.bdxr.smp1.ServiceMetadataType ret = new com.helger.xsds.bdxr.smp1.ServiceMetadataType ();
    ret.setRedirect (aRedirect);
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp2.ServiceMetadataType getAsJAXBObjectBDXR2 ()
  {
    final com.helger.xsds.bdxr.smp2.ServiceMetadataType ret = new com.helger.xsds.bdxr.smp2.ServiceMetadataType ();
    ret.setSMPExtensions (getExtensions ().getAsBDXR2Extensions ());
    ret.setSMPVersionID ("2.0");
    // It's okay to use the constructor directly
    ret.setID (new BDXR2DocumentTypeIdentifier (m_aDocumentTypeIdentifier));
    // Explicit constructor call is needed here!
    ret.setParticipantID (new BDXR2ParticipantIdentifier (m_aParticipantID));

    final com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType aProc = new com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType ();
    {
      final com.helger.xsds.bdxr.smp2.ac.RedirectType aRedirect = new com.helger.xsds.bdxr.smp2.ac.RedirectType ();
      aRedirect.setPublisherURI (m_sTargetHref);
      if (m_aCertificate != null)
      {
        final com.helger.xsds.bdxr.smp2.ac.CertificateType aCert = new com.helger.xsds.bdxr.smp2.ac.CertificateType ();
        aCert.setActivationDate (PDTFactory.createXMLOffsetDate (m_aCertificate.getNotBefore ()));
        aCert.setExpirationDate (PDTFactory.createXMLOffsetDate (m_aCertificate.getNotAfter ()));
        final ContentBinaryObjectType aCBO = aCert.setContentBinaryObject (CertificateHelper.getEncodedCertificate (m_aCertificate));
        aCBO.setMimeCode (SMPServerConfiguration.getBDXR2CertificateMimeCode ());
        aCert.setTypeCode (SMPServerConfiguration.getBDXR2CertificateTypeCode ());
        aRedirect.addCertificate (aCert);
      }
      aProc.setRedirect (aRedirect);
    }
    ret.addProcessMetadata (aProc);

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
                            .append ("ParticipantID", m_aParticipantID)
                            .append ("DocumentTypeIdentifier", m_aDocumentTypeIdentifier)
                            .append ("TargetHref", m_sTargetHref)
                            .append ("SubjectUniqueIdentifier", m_sSubjectUniqueIdentifier)
                            .append ("Certificate", m_aCertificate)
                            .getToString ();
  }
}
