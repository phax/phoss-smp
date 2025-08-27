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
package com.helger.phoss.smp.domain.serviceinfo;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDateTime;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.utils.SMPExtensionConverter;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.xsds.bdxr.smp2.bc.ContentBinaryObjectType;

/**
 * Default implementation of the {@link ISMPEndpoint} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPEndpoint extends AbstractSMPHasExtension implements ISMPEndpoint
{
  public static final boolean DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE = false;

  private String m_sTransportProfile;
  private String m_sEndpointReference;
  private boolean m_bRequireBusinessLevelSignature;
  private String m_sMinimumAuthenticationLevel;
  private XMLOffsetDateTime m_aServiceActivationDT;
  private XMLOffsetDateTime m_aServiceExpirationDT;
  private String m_sCertificate;
  private String m_sServiceDescription;
  private String m_sTechnicalContactUrl;
  private String m_sTechnicalInformationUrl;

  public SMPEndpoint (@Nonnull @Nonempty final String sTransportProfile,
                      @Nullable final String sEndpointReference,
                      final boolean bRequireBusinessLevelSignature,
                      @Nullable final String sMinimumAuthenticationLevel,
                      @Nullable final XMLOffsetDateTime aServiceActivationDT,
                      @Nullable final XMLOffsetDateTime aServiceExpirationDT,
                      @Nullable final String sCertificate,
                      @Nullable final String sServiceDescription,
                      @Nullable final String sTechnicalContactUrl,
                      @Nullable final String sTechnicalInformationUrl,
                      @Nullable final String sExtension)
  {
    setTransportProfile (sTransportProfile);
    setEndpointReference (sEndpointReference);
    setRequireBusinessLevelSignature (bRequireBusinessLevelSignature);
    setMinimumAuthenticationLevel (sMinimumAuthenticationLevel);
    setServiceActivationDateTime (aServiceActivationDT);
    setServiceExpirationDateTime (aServiceExpirationDT);
    setCertificate (sCertificate);
    setServiceDescription (sServiceDescription);
    setTechnicalContactUrl (sTechnicalContactUrl);
    setTechnicalInformationUrl (sTechnicalInformationUrl);
    getExtensions ().setExtensionAsString (sExtension);
  }

  @Nonnull
  @Nonempty
  public String getTransportProfile ()
  {
    return m_sTransportProfile;
  }

  public final void setTransportProfile (@Nonnull @Nonempty final String sTransportProfile)
  {
    ValueEnforcer.notEmpty (sTransportProfile, "TransportProfile");
    m_sTransportProfile = sTransportProfile;
  }

  @Nullable
  public String getEndpointReference ()
  {
    return m_sEndpointReference;
  }

  public final void setEndpointReference (@Nullable final String sEndpointReference)
  {
    m_sEndpointReference = sEndpointReference;
  }

  public boolean isRequireBusinessLevelSignature ()
  {
    return m_bRequireBusinessLevelSignature;
  }

  public final void setRequireBusinessLevelSignature (final boolean bRequireBusinessLevelSignature)
  {
    m_bRequireBusinessLevelSignature = bRequireBusinessLevelSignature;
  }

  @Nullable
  public String getMinimumAuthenticationLevel ()
  {
    return m_sMinimumAuthenticationLevel;
  }

  public final void setMinimumAuthenticationLevel (@Nullable final String sMinimumAuthenticationLevel)
  {
    m_sMinimumAuthenticationLevel = sMinimumAuthenticationLevel;
  }

  @Nullable
  public XMLOffsetDateTime getServiceActivationDateTime ()
  {
    return m_aServiceActivationDT;
  }

  public final void setServiceActivationDateTime (@Nullable final XMLOffsetDateTime aServiceActivationDate)
  {
    m_aServiceActivationDT = aServiceActivationDate;
  }

  @Nullable
  public XMLOffsetDateTime getServiceExpirationDateTime ()
  {
    return m_aServiceExpirationDT;
  }

  public final void setServiceExpirationDateTime (@Nullable final XMLOffsetDateTime aServiceExpirationDate)
  {
    m_aServiceExpirationDT = aServiceExpirationDate;
  }

  @Nullable
  public String getCertificate ()
  {
    return m_sCertificate;
  }

  public final void setCertificate (@Nullable final String sCertificate)
  {
    m_sCertificate = sCertificate;
  }

  @Nullable
  public String getServiceDescription ()
  {
    return m_sServiceDescription;
  }

  public final void setServiceDescription (@Nullable final String sServiceDescription)
  {
    m_sServiceDescription = sServiceDescription;
  }

  @Nullable
  public String getTechnicalContactUrl ()
  {
    return m_sTechnicalContactUrl;
  }

  public final void setTechnicalContactUrl (@Nullable final String sTechnicalContactUrl)
  {
    m_sTechnicalContactUrl = sTechnicalContactUrl;
  }

  @Nullable
  public String getTechnicalInformationUrl ()
  {
    return m_sTechnicalInformationUrl;
  }

  public final void setTechnicalInformationUrl (@Nullable final String sTechnicalInformationUrl)
  {
    m_sTechnicalInformationUrl = sTechnicalInformationUrl;
  }

  @Nonnull
  public com.helger.xsds.peppol.smp1.EndpointType getAsJAXBObjectPeppol ()
  {
    final com.helger.xsds.peppol.smp1.EndpointType ret = new com.helger.xsds.peppol.smp1.EndpointType ();
    // EndpointReference element is mandatory
    ret.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference (m_sEndpointReference != null ? m_sEndpointReference
                                                                                                               : ""));
    ret.setRequireBusinessLevelSignature (m_bRequireBusinessLevelSignature);
    ret.setMinimumAuthenticationLevel (m_sMinimumAuthenticationLevel);
    ret.setServiceActivationDate (m_aServiceActivationDT);
    ret.setServiceExpirationDate (m_aServiceExpirationDT);
    // For compatibility, don't add BEGIN_CERTIFCATE and END_CERTIFICATE
    // For .NET compatibility only use "\n" as line separator
    ret.setCertificate (CertificateHelper.getRFC1421CompliantString (m_sCertificate, false, "\n"));
    ret.setServiceDescription (m_sServiceDescription);
    ret.setTechnicalContactUrl (m_sTechnicalContactUrl);
    if (StringHelper.isNotEmpty (m_sTechnicalInformationUrl))
      ret.setTechnicalInformationUrl (m_sTechnicalInformationUrl);
    ret.setExtension (getExtensions ().getAsPeppolExtension ());
    ret.setTransportProfile (m_sTransportProfile);
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp1.EndpointType getAsJAXBObjectBDXR1 ()
  {
    final com.helger.xsds.bdxr.smp1.EndpointType ret = new com.helger.xsds.bdxr.smp1.EndpointType ();
    // Ensure an empty element is emitted if no endpoint reference is present
    ret.setEndpointURI (StringHelper.getNotNull (m_sEndpointReference));
    ret.setRequireBusinessLevelSignature (Boolean.valueOf (m_bRequireBusinessLevelSignature));
    ret.setMinimumAuthenticationLevel (m_sMinimumAuthenticationLevel);
    ret.setServiceActivationDate (m_aServiceActivationDT);
    ret.setServiceExpirationDate (m_aServiceExpirationDT);
    ret.setCertificate (CertificateHelper.convertCertificateStringToByteArray (m_sCertificate));
    ret.setServiceDescription (m_sServiceDescription);
    ret.setTechnicalContactUrl (m_sTechnicalContactUrl);
    ret.setTechnicalInformationUrl (m_sTechnicalInformationUrl);
    ret.setExtension (getExtensions ().getAsBDXRExtensions ());
    ret.setTransportProfile (m_sTransportProfile);
    return ret;
  }

  @Nonnull
  public com.helger.xsds.bdxr.smp2.ac.EndpointType getAsJAXBObjectBDXR2 ()
  {
    final com.helger.xsds.bdxr.smp2.ac.EndpointType ret = new com.helger.xsds.bdxr.smp2.ac.EndpointType ();
    ret.setSMPExtensions (getExtensions ().getAsBDXR2Extensions ());
    ret.setTransportProfileID (m_sTransportProfile);
    if (StringHelper.isNotEmpty (m_sServiceDescription))
      ret.setDescription (m_sServiceDescription);
    if (StringHelper.isNotEmpty (m_sTechnicalContactUrl))
      ret.setContact (m_sTechnicalContactUrl);
    if (StringHelper.isNotEmpty (m_sEndpointReference))
      ret.setAddressURI (m_sEndpointReference);
    if (m_aServiceActivationDT != null)
      ret.setActivationDate (m_aServiceActivationDT.toLocalDate ());
    if (m_aServiceExpirationDT != null)
      ret.setExpirationDate (m_aServiceExpirationDT.toLocalDate ());
    final X509Certificate aX509Cert = CertificateHelper.convertStringToCertficateOrNull (m_sCertificate);
    if (aX509Cert != null)
    {
      final com.helger.xsds.bdxr.smp2.ac.CertificateType aCert = new com.helger.xsds.bdxr.smp2.ac.CertificateType ();
      aCert.setActivationDate (PDTFactory.createXMLOffsetDate (aX509Cert.getNotBefore ()));
      aCert.setExpirationDate (PDTFactory.createXMLOffsetDate (aX509Cert.getNotAfter ()));
      final ContentBinaryObjectType aCBO = aCert.setContentBinaryObject (CertificateHelper.getEncodedCertificate (aX509Cert));
      aCBO.setMimeCode (SMPServerConfiguration.getBDXR2CertificateMimeCode ());
      aCert.setTypeCode (SMPServerConfiguration.getBDXR2CertificateTypeCode ());
      ret.addCertificate (aCert);
    }
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!super.equals (o))
      return false;

    final SMPEndpoint rhs = ((SMPEndpoint) o);
    return EqualsHelper.equals (m_sTransportProfile, rhs.m_sTransportProfile) &&
           EqualsHelper.equals (m_sEndpointReference, rhs.m_sEndpointReference) &&
           m_bRequireBusinessLevelSignature == rhs.m_bRequireBusinessLevelSignature &&
           EqualsHelper.equals (m_sMinimumAuthenticationLevel, rhs.m_sMinimumAuthenticationLevel) &&
           EqualsHelper.equals (m_aServiceActivationDT, rhs.m_aServiceActivationDT) &&
           EqualsHelper.equals (m_aServiceExpirationDT, rhs.m_aServiceExpirationDT) &&
           EqualsHelper.equals (m_sCertificate, rhs.m_sCertificate) &&
           EqualsHelper.equals (m_sServiceDescription, rhs.m_sServiceDescription) &&
           EqualsHelper.equals (m_sTechnicalContactUrl, rhs.m_sTechnicalContactUrl) &&
           EqualsHelper.equals (m_sTechnicalInformationUrl, rhs.m_sTechnicalInformationUrl);
  }

  @Override
  public int hashCode ()
  {
    return HashCodeGenerator.getDerived (super.hashCode ())
                            .append (m_sTransportProfile)
                            .append (m_sEndpointReference)
                            .append (m_bRequireBusinessLevelSignature)
                            .append (m_sMinimumAuthenticationLevel)
                            .append (m_aServiceActivationDT)
                            .append (m_aServiceExpirationDT)
                            .append (m_sCertificate)
                            .append (m_sServiceDescription)
                            .append (m_sTechnicalContactUrl)
                            .append (m_sTechnicalInformationUrl)
                            .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("TransportProfile", m_sTransportProfile)
                            .append ("EndpointReference", m_sEndpointReference)
                            .append ("RequireBusinessLevelSignature", m_bRequireBusinessLevelSignature)
                            .append ("MinimumAuthenticationLevel", m_sMinimumAuthenticationLevel)
                            .append ("ServiceActivationDate", m_aServiceActivationDT)
                            .append ("ServiceExpirationDate", m_aServiceExpirationDT)
                            .append ("Certificate", m_sCertificate)
                            .append ("ServiceDescription", m_sServiceDescription)
                            .append ("TechnicalContactUrl", m_sTechnicalContactUrl)
                            .append ("TechnicalInformationUrl", m_sTechnicalInformationUrl)
                            .getToString ();
  }

  @Nonnull
  public static SMPEndpoint createFromJAXB (@Nonnull final com.helger.xsds.peppol.smp1.EndpointType aEndpoint)
  {
    return new SMPEndpoint (aEndpoint.getTransportProfile (),
                            W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()),
                            aEndpoint.isRequireBusinessLevelSignature (),
                            aEndpoint.getMinimumAuthenticationLevel (),
                            aEndpoint.getServiceActivationDate (),
                            aEndpoint.getServiceExpirationDate (),
                            aEndpoint.getCertificate (),
                            aEndpoint.getServiceDescription (),
                            aEndpoint.getTechnicalContactUrl (),
                            aEndpoint.getTechnicalInformationUrl (),
                            SMPExtensionConverter.convertToString (aEndpoint.getExtension ()));
  }
}
