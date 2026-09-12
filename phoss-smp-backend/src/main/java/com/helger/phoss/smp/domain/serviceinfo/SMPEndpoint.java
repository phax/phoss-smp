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
package com.helger.phoss.smp.domain.serviceinfo;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.extension.AbstractSMPHasExtension;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.security.certificate.CertificateHelper;
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

  private final String m_sID;
  private String m_sTransportProfile;
  private ISMPAccessPoint m_aAccessPoint;
  private String m_sEndpointReference;
  private String m_sCertificate;
  private boolean m_bRequireBusinessLevelSignature;
  private String m_sMinimumAuthenticationLevel;
  private XMLOffsetDateTime m_aServiceActivationDT;
  private XMLOffsetDateTime m_aServiceExpirationDT;
  private String m_sServiceDescription;
  private String m_sTechnicalContactUrl;
  private String m_sTechnicalInformationUrl;

  /**
   * Constructor using an explicit endpoint reference URL and certificate. The endpoint does not
   * reference an Access Point.
   *
   * @param sID
   *        Endpoint ID. May neither be <code>null</code> nor empty.
   * @param sTransportProfile
   *        Transport profile. May neither be <code>null</code> nor empty.
   * @param sEndpointReference
   *        Endpoint reference URL. May be <code>null</code>.
   * @param bRequireBusinessLevelSignature
   *        Business level signature flag.
   * @param sMinimumAuthenticationLevel
   *        Minimum authentication level. May be <code>null</code>.
   * @param aServiceActivationDT
   *        Service activation date time. May be <code>null</code>.
   * @param aServiceExpirationDT
   *        Service expiration date time. May be <code>null</code>.
   * @param sCertificate
   *        The AP certificate. May be <code>null</code>.
   * @param sServiceDescription
   *        Service description. May be <code>null</code>.
   * @param sTechnicalContactUrl
   *        Technical contact URL. May be <code>null</code>.
   * @param sTechnicalInformationUrl
   *        Technical information URL. May be <code>null</code>.
   * @param sExtension
   *        Optional extension. May be <code>null</code>.
   */
  public SMPEndpoint (@NonNull @Nonempty final String sID,
                      @NonNull @Nonempty final String sTransportProfile,
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
    ValueEnforcer.notEmpty (sID, "ID");
    m_sID = sID;
    setTransportProfile (sTransportProfile);
    setEndpointReference (sEndpointReference);
    setCertificate (sCertificate);
    setRequireBusinessLevelSignature (bRequireBusinessLevelSignature);
    setMinimumAuthenticationLevel (sMinimumAuthenticationLevel);
    setServiceActivationDateTime (aServiceActivationDT);
    setServiceExpirationDateTime (aServiceExpirationDT);
    setServiceDescription (sServiceDescription);
    setTechnicalContactUrl (sTechnicalContactUrl);
    setTechnicalInformationUrl (sTechnicalInformationUrl);
    getExtensions ().setExtensionAsString (sExtension);
  }

  /**
   * Constructor referencing an Access Point. The endpoint reference URL and the certificate are
   * taken from the Access Point.
   *
   * @param sID
   *        Endpoint ID. May neither be <code>null</code> nor empty.
   * @param sTransportProfile
   *        Transport profile. May neither be <code>null</code> nor empty.
   * @param aAccessPoint
   *        The Access Point holding the endpoint reference URL and the certificate. May not be
   *        <code>null</code>.
   * @param bRequireBusinessLevelSignature
   *        Business level signature flag.
   * @param sMinimumAuthenticationLevel
   *        Minimum authentication level. May be <code>null</code>.
   * @param aServiceActivationDT
   *        Service activation date time. May be <code>null</code>.
   * @param aServiceExpirationDT
   *        Service expiration date time. May be <code>null</code>.
   * @param sServiceDescription
   *        Service description. May be <code>null</code>.
   * @param sTechnicalContactUrl
   *        Technical contact URL. May be <code>null</code>.
   * @param sTechnicalInformationUrl
   *        Technical information URL. May be <code>null</code>.
   * @param sExtension
   *        Optional extension. May be <code>null</code>.
   * @since 8.4.4
   */
  public SMPEndpoint (@NonNull @Nonempty final String sID,
                      @NonNull @Nonempty final String sTransportProfile,
                      @NonNull final ISMPAccessPoint aAccessPoint,
                      final boolean bRequireBusinessLevelSignature,
                      @Nullable final String sMinimumAuthenticationLevel,
                      @Nullable final XMLOffsetDateTime aServiceActivationDT,
                      @Nullable final XMLOffsetDateTime aServiceExpirationDT,
                      @Nullable final String sServiceDescription,
                      @Nullable final String sTechnicalContactUrl,
                      @Nullable final String sTechnicalInformationUrl,
                      @Nullable final String sExtension)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (aAccessPoint, "AccessPoint");
    m_sID = sID;
    setTransportProfile (sTransportProfile);
    setAccessPoint (aAccessPoint);
    setRequireBusinessLevelSignature (bRequireBusinessLevelSignature);
    setMinimumAuthenticationLevel (sMinimumAuthenticationLevel);
    setServiceActivationDateTime (aServiceActivationDT);
    setServiceExpirationDateTime (aServiceExpirationDT);
    setServiceDescription (sServiceDescription);
    setTechnicalContactUrl (sTechnicalContactUrl);
    setTechnicalInformationUrl (sTechnicalInformationUrl);
    getExtensions ().setExtensionAsString (sExtension);
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getTransportProfile ()
  {
    return m_sTransportProfile;
  }

  public final void setTransportProfile (@NonNull @Nonempty final String sTransportProfile)
  {
    ValueEnforcer.notEmpty (sTransportProfile, "TransportProfile");
    m_sTransportProfile = sTransportProfile;
  }

  /**
   * @return The Access Point referenced by this endpoint or <code>null</code> if the endpoint
   *         reference URL and the certificate are contained directly.
   * @since 8.4.4
   */
  @Nullable
  public ISMPAccessPoint getAccessPoint ()
  {
    return m_aAccessPoint;
  }

  /**
   * Let this endpoint reference the provided Access Point. The endpoint reference URL and the
   * certificate that may be contained directly are removed, because an endpoint either references
   * an Access Point or contains the data directly - but never both.
   *
   * @param aAccessPoint
   *        The new Access Point. May not be <code>null</code>.
   * @since 8.4.4
   */
  public final void setAccessPoint (@NonNull final ISMPAccessPoint aAccessPoint)
  {
    ValueEnforcer.notNull (aAccessPoint, "AccessPoint");
    m_aAccessPoint = aAccessPoint;
    m_sEndpointReference = null;
    m_sCertificate = null;
  }

  /**
   * Remove the reference to an Access Point and use the provided endpoint reference URL and
   * certificate directly instead.
   *
   * @param sEndpointReference
   *        The endpoint reference URL to be used directly. May be <code>null</code>.
   * @param sCertificate
   *        The certificate to be used directly. May be <code>null</code>.
   * @since 8.4.4
   */
  public final void setDirectData (@Nullable final String sEndpointReference, @Nullable final String sCertificate)
  {
    m_aAccessPoint = null;
    m_sEndpointReference = sEndpointReference;
    m_sCertificate = sCertificate;
  }

  @Nullable
  public String getEndpointReference ()
  {
    return m_aAccessPoint != null ? m_aAccessPoint.getEndpointReference () : m_sEndpointReference;
  }

  /**
   * Change the endpoint reference URL that is contained in this endpoint directly. A reference to
   * an Access Point - if present - is removed, because an endpoint either references an Access
   * Point or contains the data directly.
   *
   * @param sEndpointReference
   *        The new endpoint reference URL. May be <code>null</code>.
   */
  public final void setEndpointReference (@Nullable final String sEndpointReference)
  {
    if (m_aAccessPoint != null)
    {
      // Take over the certificate of the Access Point, so that only the URL changes
      m_sCertificate = m_aAccessPoint.getCertificate ();
      m_aAccessPoint = null;
    }
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
    return m_aAccessPoint != null ? m_aAccessPoint.getCertificate () : m_sCertificate;
  }

  /**
   * Change the certificate that is contained in this endpoint directly. A reference to an Access
   * Point - if present - is removed, because an endpoint either references an Access Point or
   * contains the data directly.
   *
   * @param sCertificate
   *        The new certificate. May be <code>null</code>.
   */
  public final void setCertificate (@Nullable final String sCertificate)
  {
    if (m_aAccessPoint != null)
    {
      // Take over the URL of the Access Point, so that only the certificate changes
      m_sEndpointReference = m_aAccessPoint.getEndpointReference ();
      m_aAccessPoint = null;
    }
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

  public com.helger.xsds.peppol.smp1.@NonNull EndpointType getAsJAXBObjectPeppol ()
  {
    final String sEndpointReference = getEndpointReference ();
    final String sCertificate = getCertificate ();
    final com.helger.xsds.peppol.smp1.EndpointType ret = new com.helger.xsds.peppol.smp1.EndpointType ();
    // EndpointReference element is mandatory
    ret.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference (sEndpointReference != null
                                                                                                             ? sEndpointReference
                                                                                                             : ""));
    ret.setRequireBusinessLevelSignature (m_bRequireBusinessLevelSignature);
    ret.setMinimumAuthenticationLevel (m_sMinimumAuthenticationLevel);
    ret.setServiceActivationDate (m_aServiceActivationDT);
    ret.setServiceExpirationDate (m_aServiceExpirationDT);
    // For compatibility, don't add BEGIN_CERTIFCATE and END_CERTIFICATE
    // For .NET compatibility only use "\n" as line separator
    ret.setCertificate (CertificateHelper.getRFC1421CompliantString (sCertificate, false, "\n"));
    ret.setServiceDescription (m_sServiceDescription);
    ret.setTechnicalContactUrl (m_sTechnicalContactUrl);
    if (StringHelper.isNotEmpty (m_sTechnicalInformationUrl))
      ret.setTechnicalInformationUrl (m_sTechnicalInformationUrl);
    ret.setExtension (getExtensions ().getAsPeppolExtension ());
    ret.setTransportProfile (m_sTransportProfile);
    return ret;
  }

  public com.helger.xsds.bdxr.smp1.@NonNull EndpointType getAsJAXBObjectBDXR1 ()
  {
    final com.helger.xsds.bdxr.smp1.EndpointType ret = new com.helger.xsds.bdxr.smp1.EndpointType ();
    // Ensure an empty element is emitted if no endpoint reference is present
    ret.setEndpointURI (StringHelper.getNotNull (getEndpointReference ()));
    ret.setRequireBusinessLevelSignature (Boolean.valueOf (m_bRequireBusinessLevelSignature));
    ret.setMinimumAuthenticationLevel (m_sMinimumAuthenticationLevel);
    ret.setServiceActivationDate (m_aServiceActivationDT);
    ret.setServiceExpirationDate (m_aServiceExpirationDT);
    ret.setCertificate (CertificateHelper.convertCertificateStringToByteArray (getCertificate ()));
    ret.setServiceDescription (m_sServiceDescription);
    ret.setTechnicalContactUrl (m_sTechnicalContactUrl);
    ret.setTechnicalInformationUrl (m_sTechnicalInformationUrl);
    ret.setExtension (getExtensions ().getAsBDXRExtensions ());
    ret.setTransportProfile (m_sTransportProfile);
    return ret;
  }

  public com.helger.xsds.bdxr.smp2.ac.@NonNull EndpointType getAsJAXBObjectBDXR2 ()
  {
    final com.helger.xsds.bdxr.smp2.ac.EndpointType ret = new com.helger.xsds.bdxr.smp2.ac.EndpointType ();
    ret.setSMPExtensions (getExtensions ().getAsBDXR2Extensions ());
    ret.setTransportProfileID (m_sTransportProfile);
    if (StringHelper.isNotEmpty (m_sServiceDescription))
      ret.setDescription (m_sServiceDescription);
    if (StringHelper.isNotEmpty (m_sTechnicalContactUrl))
      ret.setContact (m_sTechnicalContactUrl);
    if (StringHelper.isNotEmpty (getEndpointReference ()))
      ret.setAddressURI (getEndpointReference ());
    if (m_aServiceActivationDT != null)
      ret.setActivationDate (m_aServiceActivationDT.toLocalDate ());
    if (m_aServiceExpirationDT != null)
      ret.setExpirationDate (m_aServiceExpirationDT.toLocalDate ());
    final X509Certificate aX509Cert = new CertificateDecodeHelper ().source (getCertificate ())
                                                                    .pemEncoded (true)
                                                                    .getDecodedOrNull ();
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

    // Ignore super.equals
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final SMPEndpoint rhs = ((SMPEndpoint) o);
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    // Ignore super.hashCode
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("ID", m_sID)
                            .append ("TransportProfile", m_sTransportProfile)
                            .append ("AccessPoint", m_aAccessPoint)
                            .append ("EndpointReference", m_sEndpointReference)
                            .append ("Certificate", m_sCertificate)
                            .append ("RequireBusinessLevelSignature", m_bRequireBusinessLevelSignature)
                            .append ("MinimumAuthenticationLevel", m_sMinimumAuthenticationLevel)
                            .append ("ServiceActivationDate", m_aServiceActivationDT)
                            .append ("ServiceExpirationDate", m_aServiceExpirationDT)
                            .append ("ServiceDescription", m_sServiceDescription)
                            .append ("TechnicalContactUrl", m_sTechnicalContactUrl)
                            .append ("TechnicalInformationUrl", m_sTechnicalInformationUrl)
                            .getToString ();
  }
}
