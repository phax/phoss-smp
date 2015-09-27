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
package com.helger.peppol.smpserver.domain.serviceinfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.utils.CertificateHelper;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;

/**
 * Default implementation of the {@link ISMPEndpoint} interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class SMPEndpoint implements ISMPEndpoint
{
  private String m_sTransportProfile;
  private String m_sEndpointReference;
  private boolean m_bRequireBusinessLevelSignature;
  private String m_sMinimumAuthenticationLevel;
  private LocalDateTime m_aServiceActivationDT;
  private LocalDateTime m_aServiceExpirationDT;
  private String m_sCertificate;
  private String m_sServiceDescription;
  private String m_sTechnicalContactUrl;
  private String m_sTechnicalInformationUrl;
  private String m_sExtension;

  public SMPEndpoint (@Nonnull @Nonempty final String sTransportProfile,
                      @Nonnull @Nonempty final String sEndpointReference,
                      final boolean bRequireBusinessLevelSignature,
                      @Nullable final String sMinimumAuthenticationLevel,
                      @Nullable final LocalDateTime aServiceActivationDT,
                      @Nullable final LocalDateTime aServiceExpirationDT,
                      @Nonnull @Nonempty final String sCertificate,
                      @Nonnull @Nonempty final String sServiceDescription,
                      @Nonnull @Nonempty final String sTechnicalContactUrl,
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
    setExtension (sExtension);
  }

  @Nonnull
  @Nonempty
  public String getTransportProfile ()
  {
    return m_sTransportProfile;
  }

  public void setTransportProfile (@Nonnull @Nonempty final String sTransportProfile)
  {
    ValueEnforcer.notEmpty (sTransportProfile, "TransportProfile");
    m_sTransportProfile = sTransportProfile;
  }

  @Nonnull
  @Nonempty
  public String getEndpointReference ()
  {
    return m_sEndpointReference;
  }

  public void setEndpointReference (@Nonnull @Nonempty final String sEndpointReference)
  {
    ValueEnforcer.notEmpty (sEndpointReference, "EndpointReference");
    m_sEndpointReference = sEndpointReference;
  }

  public boolean isRequireBusinessLevelSignature ()
  {
    return m_bRequireBusinessLevelSignature;
  }

  public void setRequireBusinessLevelSignature (final boolean bRequireBusinessLevelSignature)
  {
    m_bRequireBusinessLevelSignature = bRequireBusinessLevelSignature;
  }

  @Nullable
  public String getMinimumAuthenticationLevel ()
  {
    return m_sMinimumAuthenticationLevel;
  }

  public void setMinimumAuthenticationLevel (@Nullable final String sMinimumAuthenticationLevel)
  {
    m_sMinimumAuthenticationLevel = sMinimumAuthenticationLevel;
  }

  @Nullable
  public LocalDateTime getServiceActivationDateTime ()
  {
    return m_aServiceActivationDT;
  }

  @Nullable
  public LocalDate getServiceActivationDate ()
  {
    return m_aServiceActivationDT != null ? m_aServiceActivationDT.toLocalDate () : null;
  }

  public void setServiceActivationDateTime (@Nullable final LocalDateTime aServiceActivationDate)
  {
    m_aServiceActivationDT = aServiceActivationDate;
  }

  @Nullable
  public LocalDateTime getServiceExpirationDateTime ()
  {
    return m_aServiceExpirationDT;
  }

  @Nullable
  public LocalDate getServiceExpirationDate ()
  {
    return m_aServiceExpirationDT != null ? m_aServiceExpirationDT.toLocalDate () : null;
  }

  public void setServiceExpirationDateTime (@Nullable final LocalDateTime aServiceExpirationDate)
  {
    m_aServiceExpirationDT = aServiceExpirationDate;
  }

  @Nonnull
  @Nonempty
  public String getCertificate ()
  {
    return m_sCertificate;
  }

  public void setCertificate (@Nonnull @Nonempty final String sCertificate)
  {
    ValueEnforcer.notEmpty (sCertificate, "Certificate");
    m_sCertificate = sCertificate;
  }

  @Nonnull
  @Nonempty
  public String getServiceDescription ()
  {
    return m_sServiceDescription;
  }

  public void setServiceDescription (@Nonnull @Nonempty final String sServiceDescription)
  {
    ValueEnforcer.notEmpty (sServiceDescription, "ServiceDescription");
    m_sServiceDescription = sServiceDescription;
  }

  @Nonnull
  @Nonempty
  public String getTechnicalContactUrl ()
  {
    return m_sTechnicalContactUrl;
  }

  public void setTechnicalContactUrl (@Nonnull @Nonempty final String sTechnicalContactUrl)
  {
    ValueEnforcer.notEmpty (sTechnicalContactUrl, "TechnicalContactUrl");
    m_sTechnicalContactUrl = sTechnicalContactUrl;
  }

  @Nullable
  public String getTechnicalInformationUrl ()
  {
    return m_sTechnicalInformationUrl;
  }

  public void setTechnicalInformationUrl (@Nullable final String sTechnicalInformationUrl)
  {
    m_sTechnicalInformationUrl = sTechnicalInformationUrl;
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
  public EndpointType getAsJAXBObject ()
  {
    final EndpointType ret = new EndpointType ();
    ret.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference (m_sEndpointReference));
    ret.setRequireBusinessLevelSignature (m_bRequireBusinessLevelSignature);
    ret.setMinimumAuthenticationLevel (m_sMinimumAuthenticationLevel);
    ret.setServiceActivationDate (m_aServiceActivationDT);
    ret.setServiceExpirationDate (m_aServiceExpirationDT);
    ret.setCertificate (CertificateHelper.getRFC1421CompliantString (m_sCertificate));
    ret.setServiceDescription (m_sServiceDescription);
    ret.setTechnicalContactUrl (m_sTechnicalContactUrl);
    ret.setTechnicalInformationUrl (m_sTechnicalInformationUrl);
    ret.setExtension (SMPExtensionConverter.convertOrNull (m_sExtension));
    ret.setTransportProfile (m_sTransportProfile);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
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
           EqualsHelper.equals (m_sTechnicalInformationUrl, rhs.m_sTechnicalInformationUrl) &&
           EqualsHelper.equals (m_sExtension, rhs.m_sExtension);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sTransportProfile)
                                       .append (m_sEndpointReference)
                                       .append (m_bRequireBusinessLevelSignature)
                                       .append (m_sMinimumAuthenticationLevel)
                                       .append (m_aServiceActivationDT)
                                       .append (m_aServiceExpirationDT)
                                       .append (m_sCertificate)
                                       .append (m_sServiceDescription)
                                       .append (m_sTechnicalContactUrl)
                                       .append (m_sTechnicalInformationUrl)
                                       .append (m_sExtension)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("transportProfile", m_sTransportProfile)
                                       .append ("endpointReference", m_sEndpointReference)
                                       .append ("requireBusinessLevelSignature", m_bRequireBusinessLevelSignature)
                                       .append ("minimumAuthenticationLevel", m_sMinimumAuthenticationLevel)
                                       .append ("serviceActivationDate", m_aServiceActivationDT)
                                       .append ("serviceExpirationDate", m_aServiceExpirationDT)
                                       .append ("certificate", m_sCertificate)
                                       .append ("serviceDescription", m_sServiceDescription)
                                       .append ("technicalContactUrl", m_sTechnicalContactUrl)
                                       .append ("technicalInformationUrl", m_sTechnicalInformationUrl)
                                       .append ("extension", m_sExtension)
                                       .toString ();
  }

  @Nonnull
  public static SMPEndpoint createFromJAXB (@Nonnull final EndpointType aEndpoint)
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
